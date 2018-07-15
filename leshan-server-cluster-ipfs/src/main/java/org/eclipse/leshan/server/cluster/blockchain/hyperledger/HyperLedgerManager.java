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

import org.eclipse.leshan.server.cluster.DeviceManager;
import org.eclipse.leshan.server.cluster.blockchain.BlockchainManager;
import org.eclipse.leshan.server.registration.Registration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HyperLedgerManager implements BlockchainManager {
    private final Logger LOG = LoggerFactory.getLogger(HyperLedgerManager.class);

    public void saveOrUpdateRegistration(Registration registration) {
    }
}