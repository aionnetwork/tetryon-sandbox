const Web3 = require("aion-web3");
const path = require('path');
const fs = require('fs');
const BN = require('bn.js');
const helper = require('./helper.js');
const assert = require('assert');
require('dotenv').config()

// parameters 
const kernelRpcIp = "http://127.0.0.1:8545";
const privateKey = process.env.ACC_SK;
const deploymentAddr = "0xa024c818ec649Ebf920aF5aFcbB2C582D0d7Cc1D859427Ee86C0300022176b89";

const web3 = new Web3(new Web3.providers.HttpProvider(kernelRpcIp));
const acc = web3.eth.accounts.privateKeyToAccount(privateKey);

console.log(acc.address);

var ProofSystem = {
  G16: 'g16',
  GM17: 'gm17',
  PGHR13: 'pghr13'
};

const WORD_SIZE = 64;

const hexStrToByteArray = (str) => { 
    /*const resultLength = str.length / 2;
    const result = new Uint8Array(resultLength);
    for (let i=0; i<resultLength; i++) {
      result[i] = parseInt(str.substring(i*2, (i*2)+2));
    }
    return result;*/
    var result = [];
    while (str.length >= 2) { 
        result.push(parseInt(str.substring(0, 2), 16));
        str = str.substring(2, str.length);
    }
    return result;
}

const parseG1 = (x) => {
  if (Array.isArray(x) && x.length == 2) {
    let encoding = "";
    x.forEach((v, i) => {  
      encoding += v.substring(2);
    });

    if (encoding.length == 2*WORD_SIZE)
      return encoding;
  }

  throw "cannot decode G1 point";
}

const parseG2 = (x) => {
  if (Array.isArray(x) && x.length == 2) {
    let encoding = "";
    x.forEach((v, i) => {  
      if (Array.isArray(v) && v.length == 2)
        encoding += v[1].substring(2) + v[0].substring(2);
    });

    if (encoding.length == 4*WORD_SIZE)
      return encoding;
  }

  throw "cannot decode G2 point";
}


const parseVerifyArgs = (path, proofSystem=ProofSystem.G16) => {
  const args = JSON.parse(fs.readFileSync(path));
    
  let proof = null;
  switch (proofSystem) {
    case ProofSystem.G16:
      const a = parseG1(args.proof.a);  
      const b = parseG2(args.proof.b);
      const c = parseG1(args.proof.c);

      proof = a + b + c; // concat
      break;
    default: 
      throw "only groth 16 proof system supported (for now)";
  }

  if (!Array.isArray(args.inputs))
    throw "input should be an array";

  let input = [];
  args.inputs.forEach((k,i) => {
    let val = k;
    if ((typeof k === 'string' || k instanceof String) && k.startsWith("0x"))
      val = k.substring(2, val.length);

    input.push(new BN(val,16));
  });

  return [input, hexStrToByteArray(proof)];
} 

const getSignedTransaction = async (data, to=null, _nonce=null) => {
  const type = (to == null) ? "0x2" : "0x1";
  const gas = (to == null) ? 5000000 : 2000000;

  const tx = {
    to: to,
    from: acc.address,
    data: data,
    gasPrice: 10000000000,
    gas: gas,
    type: type,
    nonce: _nonce
  };

  const receipt = await web3.eth.accounts.signTransaction(tx, acc.privateKey);
  return receipt;
}

// blocks returns when trasaction has either made it into the chain or failed to. 
const sendTransaction = async (signedTx) => {
  const retries = 15;
  const success = await helper.waitForFinality(web3, 1, acc.address, async () => {  
    web3.eth.sendSignedTransaction(signedTx.rawTransaction)
  }, retries);
  
  if (!success) 
    throw "failed to complete transaction";
  
  // return receipt
  return await web3.eth.getTransactionReceipt(signedTx.messageHash);
}

const deploy = async () => {
  let jarPath = path.join(__dirname,'artifacts','SquarePreimageVerifier.jar');
  let deploymentData = web3.avm.contract.deploy(jarPath).init();
  
  const signedTx = await getSignedTransaction(deploymentData);
  const receipt = await sendTransaction(signedTx);
  console.log("Contract Deployed @ " + receipt.contractAddress);
}

const validateVerify = (receipt) => {
  try {
    assert(Array.isArray(receipt.logs) && receipt.logs.length == 1);
  
    const topic0 = receipt.logs[0].topics[0];
    assert(topic0 && topic0.startsWith("0x566572696679536e61726b"))

    const data = receipt.logs[0].data;
    assert(data && (new BN(data.substring(2, data.length), 16)).eq(new BN(1)));

    return true;
  } catch (e) {
    console.log(e);
  }

  return false;
}


const verify = async () => {
  const inputValues = parseVerifyArgs(path.join(__dirname,'artifacts','verify.json'));
  let callData = web3.avm.contract.method('verify').inputs(['BigInteger[]','byte[]'], inputValues).encode();
  
  const signedTx = await getSignedTransaction(callData, deploymentAddr);
  const receipt = await sendTransaction(signedTx);
  

  if (validateVerify(receipt))
    console.log("verify() = true");
  else
    console.log("[verify() = true] FAILED");
}

const validateReject = (receipt) => {
  try {
    assert(Array.isArray(receipt.logs) && receipt.logs.length == 1);
  
    const topic0 = receipt.logs[0].topics[0];
    assert(topic0 && topic0.startsWith("0x566572696679536e61726b"))

    const data = receipt.logs[0].data;
    assert(data && (new BN(data.substring(2, data.length), 16)).isZero());

    return true;
  } catch (e) {
    console.log(e);
  }

  return false;
}

const reject = async () => {
  const inputValues = parseVerifyArgs(path.join(__dirname,'artifacts','reject.json'));
  let callData = web3.avm.contract.method('verify').inputs(['BigInteger[]','byte[]'], inputValues).encode();
  
  const signedTx = await getSignedTransaction(callData, deploymentAddr);
  const receipt = await sendTransaction(signedTx);
  
  if (validateReject(receipt))
    console.log("verify() = false");
  else
    console.log("[verify() = false] FAILED");
}

const receipt = async (hash) => {
  const receipt = await web3.eth.getTransactionReceipt(hash);
  console.log(receipt);
}

const loadTest = async () => {
  const verifyValues = parseVerifyArgs(path.join(__dirname,'artifacts','verify.json'));
  const verifyData = web3.avm.contract.method('verify').inputs(['BigInteger[]','byte[]'], verifyValues).encode();
  

  const rejectValues = parseVerifyArgs(path.join(__dirname,'artifacts','reject.json'));
  const rejectData = web3.avm.contract.method('verify').inputs(['BigInteger[]','byte[]'], rejectValues).encode();
  

  const BATCH_COUNT = 250;

  while (true) {
    let txHashList = [];

    try {
      const success = await helper.waitForFinality(web3, BATCH_COUNT, acc.address, async () => {  
        let nonce = await web3.eth.getTransactionCount(acc.address);
        

        for (let i=0; i < BATCH_COUNT; i++) {  
          if (Math.random() < 0.5) {
            const verifySignedTx = await getSignedTransaction(verifyData, deploymentAddr, nonce+i);
            txHashList.push({type:'verify', hash: verifySignedTx.messageHash});
            web3.eth.sendSignedTransaction(verifySignedTx.rawTransaction);
          }
          else {
            const rejectSignedTx = await getSignedTransaction(rejectData, deploymentAddr, nonce+i);
            txHashList.push({type:'reject', hash: rejectSignedTx.messageHash});
            web3.eth.sendSignedTransaction(rejectSignedTx.rawTransaction);
          }
        }

        console.log("Sent " + BATCH_COUNT + " transactions to " + deploymentAddr);
      });      
      
      if (!success) {
        console.log("Failed to complete transaction batch!");
        continue;
      }

      const failedCount = 0;
      txHashList.forEach(async (k,i) => {
        const receipt = await web3.eth.getTransactionReceipt(k.hash);
        let result = false;

        if (k.type == 'verify') {
          result = validateVerify(receipt);
        } else if (k.type == 'reject') {
          result = validateReject(receipt);
        }

        if (result == false)
          failedCount ++;
      });

      if (failedCount == 0)
        console.log("All " + BATCH_COUNT + " transactions PASSED validation.");
      else
        console.log(failedCount + " of " + BATCH_COUNT + " transactions FAILED validation.");

    } catch (e) {
      console.log("Error: ", e);
      continue;
    }
  }
}

(() => {
  console.log("-------------------------------------------");
  console.log("Test Bench");
  console.log("-------------------------------------------");

  loadTest()
  //receipt('0xbcfd55cc0e4f6dae665e7b5d9272f53c484a1f9383e1ef2ff2a1f49ef510363b')
  .then(() => {
    console.log("QED ...");
  }, (reason) => { // rejection
    console.log(reason);
  })
})();




//let callData = '0x21000676657269667931230002230301bba1230101110100108e813adc141aa467a3e50337b9e3d3a3faeda81f3daad90c95f015852a04a420d7e7407aa8d658239b6db47335b69bca5117a615acaf308eacad68cf93d06824b9283863460840d673477640563dce0342c76a1885d0e033128473380dfaeb2aee1a37f5f04bbd69bf351b75cae4f61ee06cf932c078e725a701f9b97aedf21aeb7fc68506205291d8fa68821e22061daf29d12f0b4e98d3c975d72f5718ba298d0af2da05bb04e81245d3f4963bcd64952a7b84186a846b973ae590d807dc1930b873043e8d3fb2499f75f6db70e2fe38961d44bea345eefcb4dfcef5ef7a042c118a8a96575ef5be6693e21fe43070c53efc1e5a4188c354706510d8cb82';
  
// 210006766572696679312300022 30301bba1230101110100                                                                                                         108e813adc141aa467a3e50337b9e3d3a3faeda81f3daad90c95f015852a04a420d7e7407aa8d658239b6db47335b69bca5117a615acaf308eacad68cf93d06824b9283863460840d673477640563dce0342c76a1885d0e033128473380dfaeb2aee1a37f5f04bbd69bf351b75cae4f61ee06cf932c078e725a701f9b97aedf21aeb7fc68506205291d8fa68821e22061daf29d12f0b4e98d3c975d72f5718ba298d0af2da05bb04e81245d3f4963bcd64952a7b84186a846b973ae590d807dc1930b873043e8d3fb2499f75f6db70e2fe38961d44bea345eefcb4dfcef5ef7a042c118a8a96575ef5be6693e21fe43070c53efc1e5a4188c354706510d8cb82
// 210006766572696679312300022 31c03222f81113f38902f9a959b355699fcacadff2100000000000056b9231c03222f81113f38902f9a959b355699fcacadff210000000000000001110000 108e813adc141aa467a3e50337b9e3d3a3faeda81f3daad90c95f015852a04a420d7e7407aa8d658239b6db47335b69bca5117a615acaf308eacad68cf93d06824b9283863460840d673477640563dce0342c76a1885d0e033128473380dfaeb2aee1a37f5f04bbd69bf351b75cae4f61ee06cf932c078e725a701f9b97aedf21aeb7fc68506205291d8fa68821e22061daf29d12f0b4e98d3c975d72f5718ba298d0af2da05bb04e81245d3f4963bcd64952a7b84186a846b973ae590d807dc1930b873043e8d3fb2499f75f6db70e2fe38961d44bea345eefcb4dfcef5ef7a042c118a8a96575ef5be6693e21fe43070c53efc1e5a4188c354706510d8cb82

// 210006766572696679312300022 31c03222f81113f38902f9a959b355699fcacadff2100000000000056b9231c03222f81113f38902f9a959b355699fcacadff210000000000000001110000 108e813adc141aa467a3e50337b9e3d3a3faeda81f3daad90c95f015852a04a420d7e7407aa8d658239b6db47335b69bca5117a615acaf308eacad68cf93d06824b9283863460840d673477640563dce0342c76a1885d0e033128473380dfaeb2aee1a37f5f04bbd69bf351b75cae4f61ee06cf932c078e725a701f9b97aedf21aeb7fc68506205291d8fa68821e22061daf29d12f0b4e98d3c975d72f5718ba298d0af2da05bb04e81245d3f4963bcd64952a7b84186a846b973ae590d807dc1930b873043e8d3fb2499f75f6db70e2fe38961d44bea345eefcb4dfcef5ef7a042c118a8a96575ef5be6693e21fe43070c53efc1e5a4188c354706510d8cb82

// a0b3b0c479085c474bfebf4aff38ff9f4d3be287a3e8088a65d2324de2a3fcf4


// 2100 0676 6572 6966 7931 2300 02 230301bba1 230101 11 0100 108e813adc141aa467a3e50337b9e3d3a3faeda81f3daad90c95f015852a04a420d7e7407aa8d658239b6db47335b69bca5117a615acaf308eacad68cf93d06824b9283863460840d673477640563dce0342c76a1885d0e033128473380dfaeb2aee1a37f5f04bbd69bf351b75cae4f61ee06cf932c078e725a701f9b97aedf21aeb7fc68506205291d8fa68821e22061daf29d12f0b4e98d3c975d72f5718ba298d0af2da05bb04e81245d3f4963bcd64952a7b84186a846b973ae590d807dc1930b873043e8d3fb2499f75f6db70e2fe38961d44bea345eefcb4dfcef5ef7a042c118a8a96575ef5be6693e21fe43070c53efc1e5a4188c354706510d8cb82
// 2100 0676 6572 6966 7931 2300 02 230301bba1 230101 11 0000 108e813adc141aa467a3e50337b9e3d3a3faeda81f3daad90c95f015852a04a420d7e7407aa8d658239b6db47335b69bca5117a615acaf308eacad68cf93d06824b9283863460840d673477640563dce0342c76a1885d0e033128473380dfaeb2aee1a37f5f04bbd69bf351b75cae4f61ee06cf932c078e725a701f9b97aedf21aeb7fc68506205291d8fa68821e22061daf29d12f0b4e98d3c975d72f5718ba298d0af2da05bb04e81245d3f4963bcd64952a7b84186a846b973ae590d807dc1930b873043e8d3fb2499f75f6db70e2fe38961d44bea345eefcb4dfcef5ef7a042c118a8a96575ef5be6693e21fe43070c53efc1e5a4188c354706510d8cb82

// 21000676657269667931230002230301bba1230101110000108e813adc141aa467a3e50337b9e3d3a3faeda81f3daad90c95f015852a04a420d7e7407aa8d658239b6db47335b69bca5117a615acaf308eacad68cf93d06824b9283863460840d673477640563dce0342c76a1885d0e033128473380dfaeb2aee1a37f5f04bbd69bf351b75cae4f61ee06cf932c078e725a701f9b97aedf21aeb7fc68506205291d8fa68821e22061daf29d12f0b4e98d3c975d72f5718ba298d0af2da05bb04e81245d3f4963bcd64952a7b84186a846b973ae590d807dc1930b873043e8d3fb2499f75f6db70e2fe38961d44bea345eefcb4dfcef5ef7a042c118a8a96575ef5be6693e21fe43070c53efc1e5a4188c354706510d8cb82

// 21000676657269667931230002230301bba1230101110100108e813adc141aa467a3e50337b9e3d3a3faeda81f3daad90c95f015852a04a420d7e7407aa8d658239b6db47335b69bca5117a615acaf308eacad68cf93d06824b9283863460840d673477640563dce0342c76a1885d0e033128473380dfaeb2aee1a37f5f04bbd69bf351b75cae4f61ee06cf932c078e725a701f9b97aedf21aeb7fc68506205291d8fa68821e22061daf29d12f0b4e98d3c975d72f5718ba298d0af2da05bb04e81245d3f4963bcd64952a7b84186a846b973ae590d807dc1930b873043e8d3fb2499f75f6db70e2fe38961d44bea345eefcb4dfcef5ef7a042c118a8a96575ef5be6693e21fe43070c53efc1e5a4188c354706510d8cb82



// 21000676657269667931230002230301bba12301011100000a085103000e010043000003250000000000000001030000005f000f55020400140000280700003a1709060049230009003311000f00001e08000044005d004418001c263f2e082800492f4c28380300032a000612550000210c5449260000000200012500000400450023014b0000000100060020004e00190001000007000001000700550614345b0000445201160601001d000200046200004b00023912001d08000000050004000c2d0000600300405f020754120654066103005a000700131e0049040308030031094b00004600002660012c00002d000000000000000704020b08086039050000425d0001001e4600030001052958003646410a000052

// 21000676657269667931230002230301bba1230101110000108e813adc141aa467a3e50337b9e3d3a3faeda81f3daad90c95f015852a04a420d7e7407aa8d658239b6db47335b69bca5117a615acaf308eacad68cf93d06824b9283863460840d673477640563dce0342c76a1885d0e033128473380dfaeb2aee1a37f5f04bbd69bf351b75cae4f61ee06cf932c078e725a701f9b97aedf21aeb7fc68506205291d8fa68821e22061daf29d12f0b4e98d3c975d72f5718ba298d0af2da05bb04e81245d3f4963bcd64952a7b84186a846b973ae590d807dc1930b873043e8d3fb2499f75f6db70e2fe38961d44bea345eefcb4dfcef5ef7a042c118a8a96575ef5be6693e21fe43070c53efc1e5a4188c354706510d8cb82

// 21000676657269667931230002230301bba1230101110100108e813adc141aa467a3e50337b9e3d3a3faeda81f3daad90c95f015852a04a420d7e7407aa8d658239b6db47335b69bca5117a615acaf308eacad68cf93d06824b9283863460840d673477640563dce0342c76a1885d0e033128473380dfaeb2aee1a37f5f04bbd69bf351b75cae4f61ee06cf932c078e725a701f9b97aedf21aeb7fc68506205291d8fa68821e22061daf29d12f0b4e98d3c975d72f5718ba298d0af2da05bb04e81245d3f4963bcd64952a7b84186a846b973ae590d807dc1930b873043e8d3fb2499f75f6db70e2fe38961d44bea345eefcb4dfcef5ef7a042c118a8a96575ef5be6693e21fe43070c53efc1e5a4188c354706510d8cb82