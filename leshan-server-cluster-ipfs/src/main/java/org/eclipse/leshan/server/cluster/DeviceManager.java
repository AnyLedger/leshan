package org.eclipse.leshan.server.cluster;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 3.4.0.
 */
public class DeviceManager extends Contract {
    private static final String BINARY = "0x608060405234801561001057600080fd5b506102d3806100206000396000f30060806040526004361061004c576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff168063a0b039b214610051578063c57005f7146100e1575b600080fd5b34801561005d57600080fd5b5061006661014a565b6040518080602001828103825283818151815260200191508051906020019080838360005b838110156100a657808201518184015260208101905061008b565b50505050905090810190601f1680156100d35780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b3480156100ed57600080fd5b50610148600480360381019080803590602001908201803590602001908080601f01602080910402602001604051908101604052809392919081815260200183838082843782019150505050505091929192905050506101e8565b005b60008054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156101e05780601f106101b5576101008083540402835291602001916101e0565b820191906000526020600020905b8154815290600101906020018083116101c357829003601f168201915b505050505081565b80600090805190602001906101fe929190610202565b5050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061024357805160ff1916838001178555610271565b82800160010185558215610271579182015b82811115610270578251825591602001919060010190610255565b5b50905061027e9190610282565b5090565b6102a491905b808211156102a0576000816000905550600101610288565b5090565b905600a165627a7a723058207d487744e08fd92e63b57b89135c49499c00182575cb1ba97e920c9908cbd2fc0029";

    public static final String FUNC_LASTREGISTRATION = "lastRegistration";

    public static final String FUNC_SETLASTREGISTRATION = "setLastRegistration";

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
        _addresses.put("1528465719401", "0x86dc4341f917f237c72c2d206c332121970d256e");
        _addresses.put("1528466419445", "0x6df43d5efd4dde3cc72edf36f012a5c390b628ac");
        _addresses.put("1528466048042", "0x6df43d5efd4dde3cc72edf36f012a5c390b628ac");
        _addresses.put("1528466329814", "0x6df43d5efd4dde3cc72edf36f012a5c390b628ac");
    }

    protected DeviceManager(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected DeviceManager(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public RemoteCall<String> lastRegistration() {
        final Function function = new Function(FUNC_LASTREGISTRATION, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<TransactionReceipt> setLastRegistration(String tempLastRegistration) {
        final Function function = new Function(
                FUNC_SETLASTREGISTRATION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(tempLastRegistration)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public static RemoteCall<DeviceManager> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(DeviceManager.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<DeviceManager> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(DeviceManager.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static DeviceManager load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new DeviceManager(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    public static DeviceManager load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new DeviceManager(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected String getStaticDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static String getPreviouslyDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }
}
