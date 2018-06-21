/*******************************************************************************
 * Copyright (c) 2018 AnyLedger
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
 *     AnyLedger - Initial version of IPFS and Blockchain specific RegistrationListener implementation
 *******************************************************************************/

package org.eclipse.leshan.server.cluster;

import java.util.Collection;
import java.io.IOException;
import java.util.Arrays;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.cluster.blockchain.BlockchainManager;
import org.eclipse.leshan.server.cluster.serialization.RegistrationSerDes;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;

/**
 * A Registration registry Listener which publish registration event on Redis channel.
 */
public class IPFSRegistrationEventPublisher implements RegistrationListener {
    private final Logger LOG = LoggerFactory.getLogger(IPFSRegistrationEventPublisher.class);

    private final IPFS ipfs;
    private final BlockchainManager blockchainManager;

    public IPFSRegistrationEventPublisher(IPFS ipfs, BlockchainManager blockchainManager) {
        this.ipfs = ipfs;
        this.blockchainManager = blockchainManager;
    }

    @Override
    public void registered(
        Registration registration, 
        Registration previousReg,
        Collection<Observation> previousObsersations) {
            saveOrUpdateRegistration(registration);
    }

    @Override
    public void updated(
        RegistrationUpdate update, 
        Registration updatedRegistration, 
        Registration previousRegistration) {
            if(didRegistrationUpdate(previousRegistration, updatedRegistration)) {
                saveOrUpdateRegistration(updatedRegistration);
            }
    }

    @Override
    public void unregistered(
        Registration registration, 
        Collection<Observation> observations, 
        boolean expired,
        Registration newReg) {
            saveOrUpdateRegistration(registration);
    }

    private boolean didRegistrationUpdate(Registration previousRegistration, Registration updatedRegistration) {
        if (previousRegistration.getAdditionalRegistrationAttributes() == null) {
            if (updatedRegistration.getAdditionalRegistrationAttributes() != null) {
                LOG.debug(String.format("AdditionalRegistrationAttributes updated"));

                return true;
            } 
        } else if (!previousRegistration.getAdditionalRegistrationAttributes().keySet().equals(updatedRegistration.getAdditionalRegistrationAttributes().keySet()) || 
                   !previousRegistration.getAdditionalRegistrationAttributes().values().equals(updatedRegistration.getAdditionalRegistrationAttributes().values())) {
            LOG.debug(String.format("AdditionalRegistrationAttributes updated"));

            return true;
        }

        if (previousRegistration.getAddress() == null) {
            if (updatedRegistration.getAddress() != null) {
                LOG.debug(String.format("Address updated"));

                return true;
            }
        } else if (!previousRegistration.getAddress().equals(updatedRegistration.getAddress())) {
            LOG.debug(String.format("Address updated"));
        
            return true;
        }

        if (!(previousRegistration.getBindingMode() == updatedRegistration.getBindingMode())) {
            LOG.debug(String.format("BindingMode updated"));

            return true;
        }

        if (previousRegistration.getEndpoint() == null) {
            if (updatedRegistration.getEndpoint() != null) {
                LOG.debug(String.format("Endpoint updated"));

                return true;
            }
        } else if (!previousRegistration.getEndpoint().equals(updatedRegistration.getEndpoint())) {
            LOG.debug(String.format("Endpoint updated"));

            return true;
        }

        if (previousRegistration.getId() == null) {
            if (updatedRegistration.getId() != null) {
                LOG.debug(String.format("Id updated"));

                return true;
            }
        } else if (!previousRegistration.getId().equals(updatedRegistration.getId())) {
            LOG.debug(String.format("Id updated"));

            return true;
        }

        if (previousRegistration.getIdentity() == null) {
            if (updatedRegistration.getIdentity() != null) {
                LOG.debug(String.format("Identity updated"));

                return true;
            }
        } else if (!previousRegistration.getIdentity().equals(updatedRegistration.getIdentity())) {
            LOG.debug(String.format("Identity updated"));

            return true;
        }

        if (previousRegistration.getLwM2mVersion() == null) {
            if (updatedRegistration.getLwM2mVersion() != null) {
                LOG.debug(String.format("LwM2mVersion updated"));

                return true;
            }
        } else if (!previousRegistration.getLwM2mVersion().equals(updatedRegistration.getLwM2mVersion())) {
            LOG.debug(String.format("LwM2mVersion updated"));

            return true;
        }

        if (previousRegistration.getObjectLinks() == null) {
            if (updatedRegistration.getObjectLinks() != null) {
                LOG.debug(String.format("ObjectLinks updated"));

                return true;
            }
        } else if (!Arrays.equals(previousRegistration.getObjectLinks(), updatedRegistration.getObjectLinks())) {
            LOG.debug(String.format("ObjectLinks updated"));

            return true;
        }

        if (!(previousRegistration.getPort() == updatedRegistration.getPort())) {
            LOG.debug(String.format("Port updated"));

            return true;
        }

        if (previousRegistration.getRegistrationDate() == null) {
            if (updatedRegistration.getRegistrationDate() != null) {
                LOG.debug(String.format("RegistrationDate updated"));

                return true;
            }
        } else if (!previousRegistration.getRegistrationDate().equals(updatedRegistration.getRegistrationDate())) {
            LOG.debug(String.format("RegistrationDate updated"));

            return true;
        }

        if (previousRegistration.getRegistrationEndpointAddress() == null) {
            if (updatedRegistration.getRegistrationEndpointAddress() != null) {
                LOG.debug(String.format("RegistrationEndpointAddress updated"));

                return true;
            }
        } else if (!previousRegistration.getRegistrationEndpointAddress().equals(updatedRegistration.getRegistrationEndpointAddress())) {
            LOG.debug(String.format("RegistrationEndpointAddress updated"));

            return true;
        }

        if (previousRegistration.getRootPath() == null) {
            if (updatedRegistration.getRootPath() != null) {
                LOG.debug(String.format("RootPath updated"));

                return true;
            }
        } else if (!previousRegistration.getRootPath().equals(updatedRegistration.getRootPath())) {
            LOG.debug(String.format("RootPath updated"));

            return true;
        }

        if (previousRegistration.getSmsNumber() == null) {
            if (updatedRegistration.getSmsNumber() != null) {
                LOG.debug(String.format("SmsNumber updated"));

                return true;
            }
        } else if (!previousRegistration.getSmsNumber().equals(updatedRegistration.getSmsNumber())) {
            LOG.debug(String.format("SmsNumber updated"));

            return true;
        }

        if (previousRegistration.getSortedObjectLinks() == null) {
            if (updatedRegistration.getSortedObjectLinks() != null) {
                LOG.debug(String.format("SortedObjectLinks updated"));
                
                return true;
            }
        } else if (!Arrays.equals(previousRegistration.getSortedObjectLinks(), updatedRegistration.getSortedObjectLinks())) {
            LOG.debug(String.format("SortedObjectLinks updated"));

            return true;
        }

        return false;
    }

    void saveOrUpdateRegistration(Registration registration) {        
        try {
            byte[] payload = RegistrationSerDes.bSerialize(registration);
            NamedStreamable.ByteArrayWrapper ipfsPayload = new NamedStreamable.ByteArrayWrapper(payload);

            registration.setPreviousIpfsHash(registration.getLatestIpfsHash());
            registration.setLatestIpfsHash(this.ipfs.add(ipfsPayload).get(0).hash.toString());

            LOG.info(String.format("Previous: %s", registration.getPreviousIpfsHash()));
            LOG.info(String.format("Latest: %s", registration.getLatestIpfsHash()));

            LOG.info(String.format("Saved/updated registration to IPFS with hash: %s", registration.getLatestIpfsHash()));

            blockchainManager.saveOrUpdateRegistration(registration);
        } catch (IOException e) { 
            LOG.error("There was an error while adding registration to IPFS", e);
        } catch (NullPointerException e) { 
            LOG.error("There was an error while adding registration to IPFS", e);
        } 
    }
}
