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
 *     AnyLedger - Adding initial implementation for Blockchain manager for Ethereum
 *******************************************************************************/

package org.eclipse.leshan.server.cluster.blockchain.ethereum;

import java.util.Collection;
import java.util.ArrayList;
import java.math.BigInteger;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.RemoteCall;

import org.eclipse.leshan.server.cluster.DeviceManager;
import org.eclipse.leshan.server.cluster.blockchain.BlockchainManager;
import org.eclipse.leshan.server.registration.Registration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EthereumManager implements BlockchainManager {
    private final Logger LOG = LoggerFactory.getLogger(EthereumManager.class);

    private Web3j web3j;
    private Credentials credentials;
    private long gasLimit = 2000000;
    private long gasPrice = 20000000000L;
    private String ethereumNodeUrl = "http://ganache-cli:8545";
    private String privateKey = "0x76dda3572973659eabbd6c9279b66256838038da8189ee689e174e7acabfe3c5";
    private String deviceManagerSmartContractAddress = "0x3d18c830c5110e3d29c5dfff28719dee3cc3ed80";

    public EthereumManager() {
        this.web3j = Web3j.build(new HttpService(ethereumNodeUrl));
        this.credentials = Credentials.create(privateKey);
    }

    public void saveOrUpdateRegistration(Registration registration) {
        try {
            DeviceManager deviceManager = DeviceManager.load(
                this.deviceManagerSmartContractAddress, 
                this.web3j, 
                this.credentials, 
                new BigInteger(String.valueOf(this.gasPrice)), 
                new BigInteger(String.valueOf(this.gasLimit)));

            TransactionReceipt transactionReceipt = deviceManager.updateDeviceRegistration(registration.getId(), registration.getLatestIpfsHash()).send();
                
            LOG.info(String.format("Calling Device Manager smart contract. Transaction hash: %s", transactionReceipt.getTransactionHash()));
        } catch (RuntimeException e) {
            LOG.error("Device Manager smart contract might not be deployed?", e);
        } catch (Exception e) {
            LOG.error("Unexpected Exception while adding registrations to Ethereum", e);
        }
    }
}