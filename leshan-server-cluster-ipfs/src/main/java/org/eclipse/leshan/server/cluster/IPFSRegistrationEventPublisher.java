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
 *     AnyLedger - Initial version of IPFS specific RegistrationListener implementation
 *******************************************************************************/

package org.eclipse.leshan.server.cluster;

import java.util.Collection;
import java.io.IOException;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.cluster.serialization.RegistrationSerDes;
import org.eclipse.leshan.server.cluster.serialization.RegistrationUpdateSerDes;
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

    public IPFSRegistrationEventPublisher(IPFS ipfs) {
        this.ipfs = ipfs;
    }

    @Override
    public void registered(
        Registration registration, 
        Registration previousReg,
        Collection<Observation> previousObsersations) {
        saveOrUpdateRegistrationToIPFS(registration);
    }

    @Override
    public void updated(
        RegistrationUpdate update, 
        Registration updatedRegistration, 
        Registration previousRegistration) {
        //value.add("regUpdate", RegistrationUpdateSerDes.jSerialize(update));
        saveOrUpdateRegistrationToIPFS(updatedRegistration);
    }

    @Override
    public void unregistered(
        Registration registration, 
        Collection<Observation> observations, 
        boolean expired,
        Registration newReg) {
        saveOrUpdateRegistrationToIPFS(registration);
    }

    void saveOrUpdateRegistrationToIPFS(Registration registration) {        
        try {
            registration.setPreviousIpfsHash(registration.getLatestIpfsHash());

            byte[] payload = RegistrationSerDes.bSerialize(registration);
            NamedStreamable.ByteArrayWrapper ipfsPayload = new NamedStreamable.ByteArrayWrapper(payload);

            registration.setLatestIpfsHash(this.ipfs.add(ipfsPayload).get(0).hash.toString());

            LOG.info(String.format("Saved/updated registration to IPFS with hash: %s", registration.getLatestIpfsHash()));
        } catch (IOException e) { 
            LOG.error("There was an error while adding registration to IPFS", e);
        } catch (NullPointerException e) { 
            LOG.error("There was an error while adding registration to IPFS", e);
        } 
    }
}
