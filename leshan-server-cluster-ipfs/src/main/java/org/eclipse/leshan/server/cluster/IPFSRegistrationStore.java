/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *     Achim Kraus (Bosch Software Innovations GmbH) - replace serialize/parse in
 *                                                     unsafeGetObservation() with
 *                                                     ObservationUtil.shallowClone.
 *                                                     Reuse already created Key in
 *                                                     setContext().
 *     Achim Kraus (Bosch Software Innovations GmbH) - rename CorrelationContext to
 *                                                     EndpointContext
 *     Achim Kraus (Bosch Software Innovations GmbH) - update to modified 
 *                                                     ObservationStore API
 *******************************************************************************/
package org.eclipse.leshan.server.cluster;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.math.BigInteger;

import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.observe.ObservationUtil;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.Startable;
import org.eclipse.leshan.server.Stoppable;
import org.eclipse.leshan.server.californium.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.californium.ObserveUtil;
import org.eclipse.leshan.server.registration.Deregistration;
import org.eclipse.leshan.server.registration.ExpirationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.UpdatedRegistration;
import org.eclipse.leshan.server.cluster.serialization.ObservationSerDes;
import org.eclipse.leshan.server.cluster.serialization.RegistrationSerDes;
import org.eclipse.leshan.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.RemoteCall;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;

/**
 * An in memory store for registration and observation.
 */
public class IPFSRegistrationStore implements CaliforniumRegistrationStore, Startable, Stoppable {
    private final Logger LOG = LoggerFactory.getLogger(IPFSRegistrationStore.class);

    // Data structure
    private final Map<String /* end-point */, Registration> registrationsByEndpoint = new HashMap<>();
    private final Map<String /* end-point */, String> ipfsHashByEndpoint = new HashMap<>();

    private Map<Token, org.eclipse.californium.core.observe.Observation> obsByToken = new HashMap<>();
    private Map<String, Set<Token>> tokensByRegId = new HashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // Listener use to notify when a registration expires
    private ExpirationListener expirationListener;

    private final ScheduledExecutorService schedExecutor;
    private final long cleanPeriod; // in seconds

    private final IPFS ipfs;

    public IPFSRegistrationStore(IPFS ipfs) {
        this(ipfs, 2); // default clean period : 2s
    }

    public IPFSRegistrationStore(IPFS ipfs, long cleanPeriodInSec) {
        this(
            ipfs, 
            Executors.newScheduledThreadPool(1, new NamedThreadFactory(String.format("IPFSRegistrationStore (%ds)", cleanPeriodInSec))),
            cleanPeriodInSec);
    }

    public IPFSRegistrationStore(IPFS ipfs, ScheduledExecutorService schedExecutor, long cleanPeriodInSec) {
        this.ipfs = ipfs;
        this.schedExecutor = schedExecutor;
        this.cleanPeriod = cleanPeriodInSec;
    }

    /* *************** Leshan Registration API **************** */

    @Override
    public Deregistration addRegistration(Registration registration) {
        try {
            lock.writeLock().lock();

            Registration registrationRemoved = registrationsByEndpoint.put(registration.getEndpoint(), registration);

            saveOrUpdateRegistrationToIPFS(registration);

            if (registrationRemoved != null) {
                Collection<Observation> observationsRemoved = unsafeRemoveAllObservations(registrationRemoved.getId());
                return new Deregistration(registrationRemoved, observationsRemoved);
            }
        } finally {
            lock.writeLock().unlock();
        }
        return null;
    }

    @Override
    public UpdatedRegistration updateRegistration(RegistrationUpdate update) {
        try {
            lock.writeLock().lock();

            Registration registration = getRegistration(update.getRegistrationId());
            if (registration == null) {
                return null;
            } else {
                Registration updatedRegistration = update.update(registration);
                registrationsByEndpoint.put(updatedRegistration.getEndpoint(), updatedRegistration);
                
                saveOrUpdateRegistrationToIPFS(updatedRegistration);

                return new UpdatedRegistration(registration, updatedRegistration);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Registration getRegistration(String registrationId) {
        try {
            lock.readLock().lock();
            // TODO we should create an index instead of iterate all over the collection
            if (registrationId != null) {
                for (Registration registration : registrationsByEndpoint.values()) {
                    if (registrationId.equals(registration.getId())) {
                        return registration;
                    }
                }
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Registration getRegistrationByEndpoint(String endpoint) {
        try {
            lock.readLock().lock();
            return registrationsByEndpoint.get(endpoint);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Registration getRegistrationByAdress(InetSocketAddress address) {
        try {
            lock.readLock().lock();
            // TODO we should create an index instead of iterate all over the collection
            if (address != null) {
                for (Registration r : registrationsByEndpoint.values()) {
                    if (address.getPort() == r.getPort() && address.getAddress().equals(r.getAddress())) {
                        return r;
                    }
                }
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Iterator<Registration> getAllRegistrations() {
        try {
            lock.readLock().lock();
            return new ArrayList<>(registrationsByEndpoint.values()).iterator();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Deregistration removeRegistration(String registrationId) {
        try {
            lock.writeLock().lock();

            Registration registration = getRegistration(registrationId);
            if (registration != null) {
                Collection<Observation> observationsRemoved = unsafeRemoveAllObservations(registration.getId());
                
                registrationsByEndpoint.remove(registration.getEndpoint());
                ipfsHashByEndpoint.remove(registration.getEndpoint());

                return new Deregistration(registration, observationsRemoved);
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* *************** Leshan Observation API **************** */

    /*
     * The observation is not persisted here, it is done by the Californium layer (in the implementation of the
     * org.eclipse.californium.core.observe.ObservationStore#add method)
     */
    @Override
    public Collection<Observation> addObservation(String registrationId, Observation observation) {

        List<Observation> removed = new ArrayList<>();

        try {
            lock.writeLock().lock();
            // cancel existing observations for the same path and registration id.
            for (Observation obs : unsafeGetObservations(registrationId)) {
                if (observation.getPath().equals(obs.getPath()) && !Arrays.equals(observation.getId(), obs.getId())) {
                    unsafeRemoveObservation(new Token(obs.getId()));
                    removed.add(obs);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

        return removed;
    }

    @Override
    public Observation removeObservation(String registrationId, byte[] observationId) {
        try {
            lock.writeLock().lock();
            Token token = new Token(observationId);
            Observation observation = build(unsafeGetObservation(token));
            if (observation != null && registrationId.equals(observation.getRegistrationId())) {
                unsafeRemoveObservation(token);
                return observation;
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Observation getObservation(String registrationId, byte[] observationId) {
        try {
            lock.readLock().lock();
            Observation observation = build(unsafeGetObservation(new Token(observationId)));
            if (observation != null && registrationId.equals(observation.getRegistrationId())) {
                return observation;
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Collection<Observation> getObservations(String registrationId) {
        try {
            lock.readLock().lock();
            return unsafeGetObservations(registrationId);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Collection<Observation> removeObservations(String registrationId) {
        try {
            lock.writeLock().lock();
            return unsafeRemoveAllObservations(registrationId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* *************** Californium ObservationStore API **************** */

    @Override
    public org.eclipse.californium.core.observe.Observation putIfAbsent(Token token, org.eclipse.californium.core.observe.Observation obs) {
        return add(token, obs, true);
    }

    @Override
    public org.eclipse.californium.core.observe.Observation put(Token token, org.eclipse.californium.core.observe.Observation obs) {
        return add(token, obs, false);
    }

    private org.eclipse.californium.core.observe.Observation add(Token token, org.eclipse.californium.core.observe.Observation obs, boolean ifAbsent) {
        org.eclipse.californium.core.observe.Observation previousObservation = null;
        if (obs != null) {
            try {
                lock.writeLock().lock();

                validateObservation(obs);

                String registrationId = ObserveUtil.extractRegistrationId(obs);
                if (ifAbsent) {
                    if (!obsByToken.containsKey(token))
                        previousObservation = obsByToken.put(token, obs);
                    else
                        return obsByToken.get(token);
                } else {
                    previousObservation = obsByToken.put(token, obs);
                }
                if (!tokensByRegId.containsKey(registrationId)) {
                    tokensByRegId.put(registrationId, new HashSet<Token>());
                }
                tokensByRegId.get(registrationId).add(token);

                // log any collisions
                if (previousObservation != null) {
                    LOG.warn(
                            "Token collision ? observation from request [{}] will be replaced by observation from request [{}] ",
                            previousObservation.getRequest(), obs.getRequest());
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        return previousObservation;
    }

    @Override
    public org.eclipse.californium.core.observe.Observation get(Token token) {
        try {
            lock.readLock().lock();
            return unsafeGetObservation(token);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void setContext(Token token, EndpointContext ctx) {
        try {
            lock.writeLock().lock();
            org.eclipse.californium.core.observe.Observation obs = obsByToken.get(token);
            if (obs != null) {
                obsByToken.put(token, new org.eclipse.californium.core.observe.Observation(obs.getRequest(), ctx));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(Token token) {
        try {
            lock.writeLock().lock();
            unsafeRemoveObservation(token);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /* *************** IPFS utility functions **************** */

    Multihash saveOrUpdateRegistrationToIPFS(Registration registration) {        
        try {
            byte[] payload = RegistrationSerDes.bSerialize(registration);
            NamedStreamable.ByteArrayWrapper filePayload = new NamedStreamable.ByteArrayWrapper("hello.txt", payload);
    
            MerkleNode addResult = this.ipfs.add(filePayload).get(0);
            ipfsHashByEndpoint.put(registration.getEndpoint(), addResult.hash.toString());

            LOG.info(String.format("Saved/updated registration to IPFS with hash: %s", addResult.hash));

            return addResult.hash;
        } catch (IOException e) { 
            LOG.error("There was an error while adding registration to IPFS", e);
        } catch (NullPointerException e) { 
            LOG.error("There was an error while adding registration to IPFS", e);
        } 

        return null;
    }

    /* *************** Observation utility functions **************** */

    private org.eclipse.californium.core.observe.Observation unsafeGetObservation(Token token) {
        org.eclipse.californium.core.observe.Observation obs = obsByToken.get(token);
        return ObservationUtil.shallowClone(obs);
    }

    private void unsafeRemoveObservation(Token observationId) {
        org.eclipse.californium.core.observe.Observation removed = obsByToken.remove(observationId);

        if (removed != null) {
            String registrationId = ObserveUtil.extractRegistrationId(removed);
            Set<Token> tokens = tokensByRegId.get(registrationId);
            tokens.remove(observationId);
            if (tokens.isEmpty()) {
                tokensByRegId.remove(registrationId);
            }
        }
    }

    private Collection<Observation> unsafeRemoveAllObservations(String registrationId) {
        Collection<Observation> removed = new ArrayList<>();
        Set<Token> tokens = tokensByRegId.get(registrationId);
        if (tokens != null) {
            for (Token token : tokens) {
                Observation observationRemoved = build(obsByToken.remove(token));
                if (observationRemoved != null) {
                    removed.add(observationRemoved);
                }
            }
        }
        tokensByRegId.remove(registrationId);
        return removed;
    }

    private Collection<Observation> unsafeGetObservations(String registrationId) {
        Collection<Observation> result = new ArrayList<>();
        Set<Token> tokens = tokensByRegId.get(registrationId);
        if (tokens != null) {
            for (Token token : tokens) {
                Observation obs = build(unsafeGetObservation(token));
                if (obs != null) {
                    result.add(obs);
                }
            }
        }
        return result;
    }

    private Observation build(org.eclipse.californium.core.observe.Observation cfObs) {
        if (cfObs == null)
            return null;

        return ObserveUtil.createLwM2mObservation(cfObs.getRequest());
    }

    private String validateObservation(org.eclipse.californium.core.observe.Observation observation) {
        String endpoint = ObserveUtil.validateCoapObservation(observation);
        if (getRegistration(ObserveUtil.extractRegistrationId(observation)) == null) {
            throw new IllegalStateException("no registration for this Id");
        }

        return endpoint;
    }

    /* *************** Expiration handling **************** */

    @Override
    public void setExpirationListener(ExpirationListener listener) {
        this.expirationListener = listener;
    }

    /**
     * start the registration store, will start regular cleanup of dead registrations.
     */
    @Override
    public void start() {
        schedExecutor.scheduleAtFixedRate(new EthereumAgent(), cleanPeriod, cleanPeriod, TimeUnit.SECONDS);
    }

    /**
     * Stop the underlying cleanup of the registrations.
     */
    @Override
    public void stop() {
        schedExecutor.shutdownNow();
        try {
            schedExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Clean up registration thread was interrupted.", e);
        }
    }

    private class Cleaner implements Runnable {
        @Override
        public void run() {
            try {
                Collection<Registration> allRegistrations = new ArrayList<>();
                try {
                    lock.readLock().lock();
                    allRegistrations.addAll(registrationsByEndpoint.values());
                } finally {
                    lock.readLock().unlock();
                }

                for (Registration reg : allRegistrations) {
                    if (!reg.isAlive()) {
                        // force de-registration
                        Deregistration removedRegistration = removeRegistration(reg.getId());
                        expirationListener.registrationExpired(removedRegistration.getRegistration(),
                                removedRegistration.getObservations());
                    }
                }
            } catch (Exception e) {
                LOG.warn("Unexpected Exception while registration cleaning", e);
            }
        }
    }

    private class EthereumAgent implements Runnable {

        private Web3j web3j;
        private Credentials credentials;
        private int gasLimit = 90000;
        private long gasPrice = 20000000000L;
        private String ethereumNodeUrl = "http://172.21.0.2:8545";
        private String privateKey = "0x76dda3572973659eabbd6c9279b66256838038da8189ee689e174e7acabfe3c5";
        private String deviceManagerSmartContractAddress = "0x3d18c830c5110e3d29c5dfff28719dee3cc3ed80";

        public EthereumAgent() {
            this.web3j = Web3j.build(new HttpService(ethereumNodeUrl));
            this.credentials = Credentials.create(privateKey);
        }

        @Override
        public void run() {
            try {
                LOG.info("Starting Ethereum agent.");

                Collection<Registration> allRegistrations = new ArrayList<>();
                try {
                    lock.readLock().lock();
                    allRegistrations.addAll(registrationsByEndpoint.values());
                } finally {
                    lock.readLock().unlock();
                }

                for (Registration registration : allRegistrations) {
                    if (registration.isAlive()) {
                        DeviceManager deviceManager = DeviceManager.load(
                            this.deviceManagerSmartContractAddress, 
                            this.web3j, 
                            this.credentials, 
                            new BigInteger(String.valueOf(this.gasPrice)), 
                            new BigInteger(String.valueOf(this.gasLimit)));
                        
                        String ipfsHash = ipfsHashByEndpoint.get(registration.getEndpoint());
                        
                        TransactionReceipt transactionReceipt = deviceManager.setLastRegistration(ipfsHash).send();
                        LOG.info(String.format("Calling Device Manager smart contract. Transaction hash: %s", transactionReceipt.getTransactionHash()));
                    }
                }
            } catch (Exception e) {
                LOG.warn("Unexpected Exception while adding registrations to Ethereum", e);
            }
        }
    }
}
