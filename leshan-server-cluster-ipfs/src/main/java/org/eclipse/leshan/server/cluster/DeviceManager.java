package org.eclipse.leshan.server.cluster;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple2;
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
    private static final String BINARY = "0x608060405234801561001057600080fd5b506106dc806100206000396000f300608060405260043610610057576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680632848a7d91461005c578063c33a63351461010b578063f356142614610136575b600080fd5b34801561006857600080fd5b50610109600480360381019080803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290505050610248565b005b34801561011757600080fd5b5061012061041d565b6040518082815260200191505060405180910390f35b34801561014257600080fd5b506101616004803603810190808035906020019092919050505061042a565b604051808060200180602001838103835285818151815260200191508051906020019080838360005b838110156101a557808201518184015260208101905061018a565b50505050905090810190601f1680156101d25780820380516001836020036101000a031916815260200191505b50838103825284818151815260200191508051906020019080838360005b8381101561020b5780820151818401526020810190506101f0565b50505050905090810190601f1680156102385780820380516001836020036101000a031916815260200191505b5094505050505060405180910390f35b6103546000836040518082805190602001908083835b602083101515610283578051825260208201915060208101905060208303925061025e565b6001836020036101000a03801982511681845116808217855250505050505090500191505090815260200160405180910390208054600181600116156101000203166002900480601f01602080910402602001604051908101604052809291908181526020018280546001816001161561010002031660029004801561034a5780601f1061031f5761010080835404028352916020019161034a565b820191906000526020600020905b81548152906001019060200180831161032d57829003601f168201915b50505050506105fe565b1561039957600182908060018154018082558091505090600182039060005260206000200160009091929091909150908051906020019061039692919061060b565b50505b806000836040518082805190602001908083835b6020831015156103d257805182526020820191506020810190506020830392506103ad565b6001836020036101000a0380198251168184511680821785525050505050509050019150509081526020016040518091039020908051906020019061041892919061060b565b505050565b6000600180549050905090565b606080600060018481548110151561043e57fe5b9060005260206000200190508060008260405180828054600181600116156101000203166002900480156104a95780601f106104875761010080835404028352918201916104a9565b820191906000526020600020905b815481529060010190602001808311610495575b50509150509081526020016040518091039020818054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156105515780601f1061052657610100808354040283529160200191610551565b820191906000526020600020905b81548152906001019060200180831161053457829003601f168201915b50505050509150808054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156105ed5780601f106105c2576101008083540402835291602001916105ed565b820191906000526020600020905b8154815290600101906020018083116105d057829003601f168201915b505050505090509250925050915091565b6000808251149050919050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061064c57805160ff191683800117855561067a565b8280016001018555821561067a579182015b8281111561067957825182559160200191906001019061065e565b5b509050610687919061068b565b5090565b6106ad91905b808211156106a9576000816000905550600101610691565b5090565b905600a165627a7a72305820d2a68f64f42f84cf50f33dfcc70bd0d4a02ad0979d00c2d351e31dff057331db0029";

    public static final String FUNC_UPDATEDEVICEREGISTRATION = "updateDeviceRegistration";

    public static final String FUNC_GETDEVICEREGISTRATIONCOUNT = "getDeviceRegistrationCount";

    public static final String FUNC_GETDEVICEREGISTRATIONBYINDEX = "getDeviceRegistrationByIndex";

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

    public RemoteCall<TransactionReceipt> updateDeviceRegistration(String deviceId, String ipfsHash) {
        final Function function = new Function(
                FUNC_UPDATEDEVICEREGISTRATION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(deviceId), 
                new org.web3j.abi.datatypes.Utf8String(ipfsHash)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<BigInteger> getDeviceRegistrationCount() {
        final Function function = new Function(FUNC_GETDEVICEREGISTRATIONCOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<Tuple2<String, String>> getDeviceRegistrationByIndex(BigInteger deviceRegistrationIndex) {
        final Function function = new Function(FUNC_GETDEVICEREGISTRATIONBYINDEX, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(deviceRegistrationIndex)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}));
        return new RemoteCall<Tuple2<String, String>>(
                new Callable<Tuple2<String, String>>() {
                    @Override
                    public Tuple2<String, String> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple2<String, String>(
                                (String) results.get(0).getValue(), 
                                (String) results.get(1).getValue());
                    }
                });
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
