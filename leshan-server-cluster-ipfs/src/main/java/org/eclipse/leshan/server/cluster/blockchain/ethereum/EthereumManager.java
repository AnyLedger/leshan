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
    private DeviceManager deviceManager;

    public EthereumManager(long gasLimit, long gasPrice, String ethereumNodeUrl, String privateKey, String deviceManagerSmartContractAddress) {
        this.web3j = Web3j.build(new HttpService(ethereumNodeUrl));
        this.credentials = Credentials.create(privateKey);

        this.deviceManager = DeviceManager.load(
            deviceManagerSmartContractAddress, 
            web3j, 
            credentials, 
            new BigInteger(String.valueOf(gasPrice)), 
            new BigInteger(String.valueOf(gasLimit)));
    }

    public void saveOrUpdateRegistration(Registration registration) {
        try {
            TransactionReceipt transactionReceipt = deviceManager.updateDeviceRegistration(registration.getId(), registration.getLatestIpfsHash()).send();
                
            LOG.info(String.format("Calling Device Manager smart contract. Transaction hash: %s", transactionReceipt.getTransactionHash()));
        } catch (RuntimeException e) {
            LOG.error("Device Manager smart contract might not be deployed?", e);
        } catch (Exception e) {
            LOG.error("Unexpected Exception while adding registrations to Ethereum", e);
        }
    }
}