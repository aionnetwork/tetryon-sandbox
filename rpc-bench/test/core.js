const path = require('path');
require('dotenv').config({path: path.resolve(__dirname,'../.env')});

const Web3 = require("aion-web3");
const {readFileSync} = require('fs');
const BN = require('bn.js');
const helper = require('./helper.js');
const assert = require('assert');

// parameters 
let kernelRpcIp = process.env.NODE_IP ? process.env.NODE_IP : "http://127.0.0.1:8545";
let privateKey = process.env.ACC_SK;

const web3 = new Web3(new Web3.providers.HttpProvider(kernelRpcIp));
const acc = web3.eth.accounts.privateKeyToAccount(privateKey);

const ProofSystem = {
  G16: 'g16',
  GM17: 'gm17',
  PGHR13: 'pghr13'
};

const WORD_SIZE = 64;

const hexStrToByteArray = (str) => {
    let result = [];
    while (str.length >= 2) { 
        result.push(parseInt(str.substring(0, 2), 16));
        str = str.substring(2, str.length);
    }
    return result;
};

const parseG1 = (x) => {
  if (Array.isArray(x) && x.length === 2) {
    let encoding = "";
    x.forEach((v, i) => {  
      encoding += v.substring(2);
    });

    if (encoding.length === 2*WORD_SIZE)
      return encoding;
  }

  throw "cannot decode G1 point";
};

const parseG2 = (x) => {
  if (Array.isArray(x) && x.length === 2) {
    let encoding = "";
    x.forEach((v, i) => {  
      if (Array.isArray(v) && v.length === 2)
        encoding += v[1].substring(2) + v[0].substring(2);
    });

    if (encoding.length === 4*WORD_SIZE)
      return encoding;
  }

  throw "cannot decode G2 point";
};

const parseVerifyArgs = (path, proofSystem=ProofSystem.G16) => {
  const args = JSON.parse(readFileSync(path).toString());
    
  let proof = null;
  // noinspection JSRedundantSwitchStatement
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
};

const getSignedTransaction = async (_data, _to=null, _nonce=null, _value=null) => {
  const type = (_to == null) ? "0x2" : "0x1";
  const gas = (_to == null) ? 5000000 : 2000000;

  let tx = {
    to: _to,
    from: acc.address,
    gasPrice: 10000000000,
    gas: gas,
    type: type
  };

  if (_data)
    tx.data = _data;

  if(_nonce)
    tx.nonce = _nonce;

  if (_value)
    tx.value = _value;

  return await web3.eth.accounts.signTransaction(tx, acc.privateKey);
};

// blocks returns when transaction has either made it into the chain or failed to.
const sendTransaction = async (signedTx) => {
  const retries = 15;
  const success = await helper.waitForFinality(web3, 1, acc.address, async () => {
    web3.eth.sendSignedTransaction(signedTx.rawTransaction)
    .catch(e => console.log(e));
  }, retries);
  
  if (!success) 
    throw "failed to complete transaction";
  
  // return receipt
  return await web3.eth.getTransactionReceipt(signedTx.messageHash);
};

const deploy = async (jarPath) => {
  let deploymentData = web3.avm.contract.deploy(jarPath).init();

  const signedTx = await getSignedTransaction(deploymentData);
  const receipt = await sendTransaction(signedTx);

  return receipt.contractAddress;
};
module.exports.deploy = deploy;

const validateVerify = (receipt) => {
  try {
    assert(Array.isArray(receipt.logs) && receipt.logs.length === 1);
  
    const topic0 = receipt.logs[0].topics[0];
    assert(topic0 && topic0.startsWith("0x566572696679536e61726b"))

    const data = receipt.logs[0].data;
    assert(data && (new BN(data.substring(2, data.length), 16)).eq(new BN(1)));

    return true;
  } catch (e) {
    console.log(e);
  }

  return false;
};

const verify = async (deploymentAddr, jsonPath) => {
  const inputValues = parseVerifyArgs(jsonPath);
  let callData = web3.avm.contract.method('verify').inputs(['BigInteger[]','byte[]'], inputValues).encode();
  
  const signedTx = await getSignedTransaction(callData, deploymentAddr);
  const receipt = await sendTransaction(signedTx);

  return validateVerify(receipt);
};
module.exports.verify = verify;

const validateReject = (receipt) => {
  try {
    assert(Array.isArray(receipt.logs) && receipt.logs.length === 1);
  
    const topic0 = receipt.logs[0].topics[0];
    assert(topic0 && topic0.startsWith("0x566572696679536e61726b"))

    const data = receipt.logs[0].data;
    assert(data && (new BN(data.substring(2, data.length), 16)).isZero());

    return true;
  } catch (e) {
    console.log(e);
  }

  return false;
};

const reject = async (deploymentAddr, jsonPath) => {
  const inputValues = parseVerifyArgs(jsonPath);
  let callData = web3.avm.contract.method('verify').inputs(['BigInteger[]','byte[]'], inputValues).encode();
  
  const signedTx = await getSignedTransaction(callData, deploymentAddr);
  const receipt = await sendTransaction(signedTx);
  
  return validateReject(receipt);
};
module.exports.reject = reject;

const loadTest = async (deploymentAddr, verifyPath, rejectPath) => {
  const verifyValues = parseVerifyArgs(verifyPath);
  const verifyData = web3.avm.contract.method('verify').inputs(['BigInteger[]','byte[]'], verifyValues).encode();

  const rejectValues = parseVerifyArgs(rejectPath);
  const rejectData = web3.avm.contract.method('verify').inputs(['BigInteger[]','byte[]'], rejectValues).encode();

  const BATCH_COUNT = 100;
  const MAX_RETRIES = 85;

  try {
    let txHashList = [];
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
    }, MAX_RETRIES);

    if (!success) {
      console.log("Failed to complete transaction batch!");
    }

    let failedCount = BATCH_COUNT;
    for (let i=0; i<txHashList.length; i++) {
      const k = txHashList[i];
      const receipt = await web3.eth.getTransactionReceipt(k.hash);
      let result = false;

      if (k.type === 'verify') {
        result = validateVerify(receipt);
      } else if (k.type === 'reject') {
        result = validateReject(receipt);
      }

      if (result === true)
        failedCount --;
    }

    if (failedCount === 0) {
      console.log("All " + BATCH_COUNT + " transactions PASSED validation.");
      return true;
    } else {
      console.log(failedCount + " of " + BATCH_COUNT + " transactions FAILED validation.");
    }

  } catch (e) {
    console.log("Error: ", e);
  }

  return false;
};
module.exports.loadTest = loadTest;

const randHex = function(len) {
  var maxlen = 8,
      min = Math.pow(16,Math.min(len,maxlen)-1) 
      max = Math.pow(16,Math.min(len,maxlen)) - 1,
      n   = Math.floor( Math.random() * (max-min+1) ) + min,
      r   = n.toString(16);
  while ( r.length < len ) {
     r = r + randHex( len - maxlen );
  }
  return r;
};

const generateRandomAddress = function() {
  return "0xa0"+randHex(62);
}

const loadTestValue = async () => {
  const BATCH_COUNT = 500;
  const MAX_RETRIES = 85;

  try {
      const success = await helper.waitForFinality(web3, BATCH_COUNT, acc.address, async () => {
      let nonce = await web3.eth.getTransactionCount(acc.address);

      const batch = new web3.BatchRequest();

      for (let i=0; i < BATCH_COUNT; i++) {
          const signedTx = await getSignedTransaction(null, generateRandomAddress(), nonce+i, 1);
          batch.add(web3.eth.sendSignedTransaction.request(signedTx.rawTransaction));
      }

      await batch.execute();
      console.log("Sent " + BATCH_COUNT + " value transfers.");
    }, MAX_RETRIES);

    return success;
  } catch (e) {
    console.log("Error: ", e);
  }

  return false;
};
module.exports.loadTestValue = loadTestValue;