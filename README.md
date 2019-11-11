# Tetryon Sandbox

The [Tetryon Test-Network](https://github.com/aionnetwork/tetryon) is an incubator for prototype implementations of privacy-preserving primitives and applications on the Open Application Network. This repository will serve as a sandbox for publishing demonstrations and prototypes leveraging features introduced to Tetryon.  

Currently, this repository contains test-benches to exercise the features introduced in the initial release for Tetryon, at a variety of different levels:  

### Contract Integration Testing

The [contract-bench](https://github.com/aionnetwork/tetryon-bench/tree/master/contract-bench) tests the verifier smart contracts (generated by ZoKrates) for correctness. New contracts leveraging Alt-Bn 128 operations implemented in the AVM can be developed in this environment.  

Assuming >JDK 10 is installed, the tests can be run using packaged Gradle wrapper: 
```
./gradlew :zokrates-bench:cleanTest :contract-bench:test -i
```

### ZoKrates Integration Testing

The [zokrates-bench](https://github.com/aionnetwork/tetryon-bench/tree/master/zokrates-bench) tests integration of the ZoKrates toolchain with the AVM programming environment. Here, the ZoKates binary is invoked from Java (using the class `ZokratesProgram`), to enable development and testing of SNARKs on the AVM, without needing to switch to the ZoKrates CLI to invoke intermediate operations. One could go directly from writing a SNARK in the ZoKrates DSL, to deploying a verifier for the SNARK on the AVM, to testing the deployed verifier, all without leaving the JUnit environment. This environment uses a stand-alone AVM object (using the [AVM JUnit rule](https://blog.aion.network/debugging-avm-contracts-4a3256e86221)) to enable greater debuggability.  

Assuming >JDK 10 is installed, the tests can be run using packaged Gradle wrapper: 
```
./gradlew :zokrates-bench:cleanTest :zokrates-bench:test -i
```

### Kernel Integration Testing

The [rpc-bench](https://github.com/aionnetwork/tetryon-bench/tree/master/rpc-bench) is a Node.js project that tests the integration of the [modified AVM](https://github.com/ali-sharif/avm) within the [modified Aion Java Kernel](https://github.com/aionnetwork/aion/tree/tetryon). This project leverages the JSON RPC API (using the [Aion Web3 Javascript client](https://www.npmjs.com/package/aion-web3)) to deploy and exercise functionality of a  ZoKrates-generated SNARK verifier contract. 

In order to run these tests, a little bit of environment setup needs to be performed: 

1. Get the latest Tetryon-enabled Aion Kernel from the [releases page](https://github.com/aionnetwork/tetryon/releases) of the Tetryon repository. 
2. Modify the kernel configuration (`config/tetryon/config.xml`) to run node in standalone mode by emptying out the `<nodes> .. </nodes>` xml node.
3. Generate a new account using the aion.sh CLI `./aion.sh -n Tetryon --account c`. Make sure to note down the private key and address for this account. The private key can be obtained by "exporting" the account (`./aion.sh -n Tetryon --account e <generated address>`) once a keystore file is generated.
4. Modify the genesis file (`config/tetryon/genesis.json`) by replacing one of the addresses listed in the `alloc` list with the address you just generated (and know the private key for). This endows the address with some pre-mined balance, for testing purposes.
5. Make sure RPC server is enabled on the IP address `http://127.0.0.1:8545`.  
6. Start a standalone Aion node in Tetryon mode (`aion.sh --n tetryon`).

So far, we've gotten a standalone, Tetryon-enabled Aion node running. Now, in order to run the RPC test bench: 

1. Make sure that you have the latest stable distribution of [node.js](https://github.com/nvm-sh/nvm)
2. Create a file in the npm project root (`./rpc-bench`) called `.env`. This file should contain the following environment variables
    ```
    ACC_SK=<private key for a Tetryon account with positive balance>
    NODE_IP=http://127.0.0.1:8545
    ```
3. Install project dependencies by executing the command `npm install`
4. To run the [Mocha](https://mochajs.org/) tests that deploy and exercise functionality of a ZoKrates-generated SNARK verifier contract in the standalone kernel environment, execute the command 
    ```
    npm test
    ```







