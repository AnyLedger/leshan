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
 *******************************************************************************/
package org.eclipse.leshan.server.cluster;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.LwM2m;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.cluster.blockchain.BlockchainManager;
import org.eclipse.leshan.server.cluster.blockchain.ethereum.EthereumManager;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StaticModelProvider;
import org.eclipse.leshan.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.RetryException;
import io.ipfs.api.IPFS;

/**
 * The main entry point for the Leshan LWM2M cluster server node.
 * 
 * Will start a LWM2M server which would be part of a large cluster group. This group of cluster node will share state
 * and communicate using Redis. So they all need too be connected to the same redis server.
 */
public class LeshanClusterServer {

    static {
        System.setProperty("logback.configurationFile", "logback-config.xml");
    }

    private static final Logger LOG = LoggerFactory.getLogger(LeshanClusterServer.class);

    private final static String USAGE = "java -jar leshan-server-cluster.jar [OPTION]";

    public static void main(String[] args) {
        // Define options for command line tools
        Options options = new Options();

        options.addOption("h", "help", false, "Display help information.");
        options.addOption("n", "instanceID", true, "Sets the unique identifier of this instance in the cluster.");
        options.addOption("lh", "coaphost", true, "Sets the local CoAP address.\n  Default: any local address.");
        options.addOption("lp", "coapport", true,
                String.format("Sets the local CoAP port.\n  Default: %d.", LwM2m.DEFAULT_COAP_PORT));
        options.addOption("slh", "coapshost", true, "Sets the local secure CoAP address.\nDefault: any local address.");
        options.addOption("slp", "coapsport", true,
                String.format("Sets the local secure CoAP port.\nDefault: %d.", LwM2m.DEFAULT_COAP_SECURE_PORT));
        options.addOption("m", "modelsfolder", true, "A folder which contains object models in OMA DDF(.xml) format.");
        options.addOption("ipfs", "ipfs", true,
                "Sets the location of IPFS. Default: '/ip4/172.21.0.4/tcp/5001'.");
        options.addOption("bn", "blockchainnode", true,
                "Sets the address of blockchain node. Default: '172.21.0.2:8545'.");        
        options.addOption("gl", "gaslimit", true,
                "Gas limit. Default: 90000.");   
        options.addOption("gp", "gasprice", true,
                "Gas price. Default: 20000000000L.");   
        options.addOption("pk", "privatekey", true,
                "Gas price. Default: 0x76dda3572973659eabbd6c9279b66256838038da8189ee689e174e7acabfe3c5.");   
        options.addOption("sc", "smartcontract", true,
                "Device Manager smart contract address. Default: 0x3d18c830c5110e3d29c5dfff28719dee3cc3ed80.");   

        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);

        // Parse arguments
        CommandLine cl;
        try {
            cl = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            System.err.println("Parsing failed.  Reason: " + e.getMessage());
            formatter.printHelp(USAGE, options);
            return;
        }

        // Print help
        if (cl.hasOption("help")) {
            formatter.printHelp(USAGE, options);
            return;
        }

        // Abort if unexpected options
        if (cl.getArgs().length > 0) {
            System.err.println("Unexpected option or arguments : " + cl.getArgList());
            formatter.printHelp(USAGE, options);
            return;
        }

        // Get cluster instance Id
        String clusterInstanceId = cl.getOptionValue("n");
        if (clusterInstanceId == null) {
            //System.err.println("InstanceId is mandatory !");
            //formatter.printHelp(USAGE, options);
            //return;
        }

        // Get local address
        String localAddress = cl.getOptionValue("lh");
        String localPortOption = cl.getOptionValue("lp");
        int localPort = LwM2m.DEFAULT_COAP_PORT;
        if (localPortOption != null) {
            localPort = Integer.parseInt(localPortOption);
        }

        // get secure local address
        String secureLocalAddress = cl.getOptionValue("slh");
        String secureLocalPortOption = cl.getOptionValue("slp");
        int secureLocalPort = LwM2m.DEFAULT_COAP_SECURE_PORT;
        if (secureLocalPortOption != null) {
            secureLocalPort = Integer.parseInt(secureLocalPortOption);
        }

        // Get models folder
        String modelsFolderPath = cl.getOptionValue("m");

        // Get the IPFS hostname
        String ipfsUrl = cl.getOptionValue("ipfs");
        if (ipfsUrl == null) {
            ipfsUrl = "ipfs-ethereum";
        }

        LOG.info(String.format("IPFS address set to: %s", ipfsUrl));

        // Get the blockchain node hostname:port
        String blockchainNodeUrl = cl.getOptionValue("bn");
        if (blockchainNodeUrl == null) {
            blockchainNodeUrl = "ganache-cli:8545";
        }

        LOG.info(String.format("Blockchain node host address set to: %s", blockchainNodeUrl));

        String gasLimitOption = cl.getOptionValue("gl");
        long gasLimit = 90000;
        if (gasLimitOption != null) {
            gasLimit = Long.parseLong(gasLimitOption);
        }

        LOG.info(String.format("Gas limit set to: %d", gasLimit));

        String gasPriceOption = cl.getOptionValue("gp");
        long gasPrice = 20000000000L;
        if (gasPriceOption != null) {
            gasPrice = Long.parseLong(gasPriceOption);
        }

        LOG.info(String.format("Gas price set to: %d", gasPrice));

        String privateKey = cl.getOptionValue("pk");
        if (privateKey == null) {
            privateKey = "0x76dda3572973659eabbd6c9279b66256838038da8189ee689e174e7acabfe3c5";
        }

        LOG.info(String.format("Private key set to: %s", privateKey));

        String deviceManagerSmartContractAddress = cl.getOptionValue("sc");
        if (deviceManagerSmartContractAddress == null) {
            deviceManagerSmartContractAddress = "0x3d18c830c5110e3d29c5dfff28719dee3cc3ed80";
        }

        LOG.info(String.format("Device Manager smart contract address set to: %s", deviceManagerSmartContractAddress));

        createAndStartServer(
            clusterInstanceId, 
            localAddress,
            localPort, 
            secureLocalAddress, 
            secureLocalPort, 
            modelsFolderPath, 
            ipfsUrl,
            gasLimit,
            gasPrice,
            privateKey,
            deviceManagerSmartContractAddress,
            blockchainNodeUrl);
    }

    private static void createAndStartServer(
        String clusterInstanceId, 
        String localAddress, 
        int localPort,
        String secureLocalAddress, 
        int secureLocalPort, 
        String modelsFolderPath, 
        String ipfsUrl,
        long gasLimit,
        long gasPrice,
        String privateKey,
        String deviceManagerSmartContractAddress,
        String blockchainNodeUrl) {
        
        IPFS ipfs = null;

        Retryer<IPFS> retryer = RetryerBuilder.<IPFS>newBuilder()
            .retryIfResult(Predicates.<IPFS>isNull())
            .retryIfRuntimeException()
            .withStopStrategy(StopStrategies.stopAfterDelay(15, TimeUnit.SECONDS))
            .build();

        try {
            ipfs = retryer.call(new IPFSRegistrationTask(ipfsUrl));
        } catch (RetryException e) {
            LOG.error("Error while opening a connection to IPFS. Retry timeout expired.");
        } catch (ExecutionException e) {
            LOG.error("Error while opening a connection to IPFS");
        }

        BlockchainManager blockchainManager = new EthereumManager(gasLimit, gasPrice, blockchainNodeUrl, privateKey, deviceManagerSmartContractAddress);

        // Prepare LWM2M server.
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(localAddress, localPort);
        builder.setLocalSecureAddress(secureLocalAddress, secureLocalPort);
        DefaultLwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
        builder.setDecoder(decoder);
        builder.setCoapConfig(NetworkConfig.getStandard());

        List<ObjectModel> models = ObjectLoader.loadDefault();
        if (modelsFolderPath != null) {
            models.addAll(ObjectLoader.loadObjectsFromDir(new File(modelsFolderPath)));
        }
        LwM2mModelProvider modelProvider = new StaticModelProvider(models);
        builder.setObjectModelProvider(modelProvider);

        DecentralizedRegistrationStore registrationStore = new DecentralizedRegistrationStore();
        builder.setRegistrationStore(registrationStore);
        builder.setSecurityStore(new IPFSSecurityStore(ipfs));

        // Create and start LWM2M server
        LeshanServer lwServer = builder.build();

        // Create Clustering support
        //RedisTokenHandler tokenHandler = new RedisTokenHandler(jedis, clusterInstanceId);
        //new RedisRequestResponseHandler(jedis, lwServer, lwServer.getRegistrationService(), tokenHandler,
        //        lwServer.getObservationService());
        //lwServer.getRegistrationService().addListener(tokenHandler);
        
        lwServer.getRegistrationService().addListener(new DecentralizedRegistrationEventPublisher(ipfs, blockchainManager));

        // Start Jetty & Leshan
        LOG.info("Starting Leshan server...");

        lwServer.start();
    }
}
