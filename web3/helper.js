const moment = require('moment');

const timeout = async (seconds) => {
  return new Promise(resolve => setTimeout(resolve, seconds * 1000));
}

const log = (s) => {
  const now = moment();
  console.log("[" + now.format("h:mm:ss") + "] " + s);
}

const MAX_RETRIES = 80;

module.exports.waitForFinality = async (_web3, _nonceCount, _address, _callback, _maxRetries=MAX_RETRIES, _timeoutSeconds=5) => {
  const n0 = await _web3.eth.getTransactionCount(_address);
  log("Polling every " + _timeoutSeconds + "s to observe nonce move from " + n0 + " to " + (n0 + _nonceCount) + ".");

  await _callback();

  // now wait for the last transaction to be included in a block
  let retries = 0;
  while(true) {
    const n1 = await _web3.eth.getTransactionCount(_address);
    log("Current nonce: " + n1.toString(10));

    const diff = n1 - n0;

    if(diff == _nonceCount) 
      break;
    
    if(diff > _nonceCount || retries >= _maxRetries) {
      log("waitForNonceIncrementBy failed somehow ... returning.");
      return false;
    }

    retries++;
    await timeout(_timeoutSeconds);
  }

  return true;
}