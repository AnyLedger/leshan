/*******************************************************************************
 * Copyright (c) 2018 AnyLedger. Bogdan Djukic
 *******************************************************************************/

package org.eclipse.leshan.server.cluster;

import java.util.concurrent.Callable;
import io.ipfs.api.IPFS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IPFSRegistrationTask implements Callable<IPFS> {
    private String ipfsUrl;
    private static final Logger LOG = LoggerFactory.getLogger(IPFSRegistrationTask.class);

    public IPFSRegistrationTask(String ipfsUrl) {
        this.ipfsUrl = ipfsUrl;
    }

    @Override
    public IPFS call() {
        return new IPFS(ipfsUrl, 5001);
    }
}