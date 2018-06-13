/*******************************************************************************
 * Copyright (c) 2018 AnyLedger. Bogdan Djukic
 *******************************************************************************/

package org.eclipse.leshan.server.cluster;

import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.Link;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Map;

import io.ipfs.multihash.Multihash;

public class DecentralizedRegistration extends Registration {

    private Multihash lastIpfsHash;
    private Multihash ipfsHash;

    public DecentralizedRegistration(Registration registration) {
            super(
                registration.getId(), 
                registration.getEndpoint(),
                registration.getIdentity(), 
                registration.getLwM2mVersion(), 
                registration.getLifeTimeInSec(), 
                registration.getSmsNumber(), 
                registration.getBindingMode(),
                registration.getObjectLinks(),
                registration.getRegistrationEndpointAddress(),
                registration.getRegistrationDate(),
                registration.getLastUpdate(),
                registration.getAdditionalRegistrationAttributes());
    }

    public Multihash getLastIpfsHash() {
        return this.lastIpfsHash;
    }

    public Multihash getIpfsHash() {
        return this.ipfsHash;
    }

    public void setLastIpfsHash(Multihash lastIpfsHash) {
        this.lastIpfsHash = lastIpfsHash;
    }

    public void setIpfsHash(Multihash ipfsHash) {
        this.ipfsHash = ipfsHash;
    }
}