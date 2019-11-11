const path = require('path');
const {deploy, verify, reject, loadTest, loadTestValue} = require('./core.js');
const assert = require('chai').assert;
const {describe, before, it} = require('mocha');

describe('Tetryon Test Bench', function () {
  this.timeout(false); // we have timeouts internally in waitForFinality
  let deployAddr = null;

  // deploy the contract
  before(function (done) {
    console.log("-----------------------------------------------------");
    console.log("Deploying Contract");
    console.log("-----------------------------------------------------");

    // first, set the parameters
    const jarPath = path.join(__dirname + '/../artifacts/SquarePreimageVerifier.jar');
    deploy(jarPath)
      .then((_addr) => {
        deployAddr = _addr;
        console.log("Contract Deployed @ " + deployAddr);
        done();
      })
      .catch((e) => {
        console.log(e);
        done(e);
      });
  });

  it('Verify() Positive', function (done) {
    console.log("-----------------------------------------------------");
    console.log("Running Verify Positive");
    console.log("-----------------------------------------------------");

    assert(deployAddr && deployAddr.length === 66, "Contract is not deployed.");
    const jsonPath = path.join(__dirname,'../artifacts/verify.json');

    verify(deployAddr, jsonPath)
      .then((success) => {
        assert(success === true, "Verification Failed");
        done();
      })
      .catch((e) => {
        console.log(e);
        done(e);
      });
  });

  it('Verify() Negative', function (done) {
    console.log("-----------------------------------------------------");
    console.log("Running Verify Negative");
    console.log("-----------------------------------------------------");

    assert(deployAddr && deployAddr.length === 66, "Contract is not deployed.");
    const jsonPath = path.join(__dirname,'../artifacts/reject.json');

    reject(deployAddr, jsonPath)
      .then((success) => {
        assert(success === true, "Reject Failed");
        done();
      })
      .catch((e) => {
        console.log(e);
        done(e);
      });
  });

  it('Load Test Snark', function (done) {
    console.log("-----------------------------------------------------");
    console.log("Running Load Test Snark");
    console.log("-----------------------------------------------------");

    assert(deployAddr && deployAddr.length === 66, "Contract is not deployed.");
    const verifyPath = path.join(__dirname,'../artifacts/verify.json');
    const rejectPath = path.join(__dirname,'../artifacts/reject.json');

    loadTest(deployAddr, verifyPath, rejectPath)
        .then((success) => {
          assert(success === true, "Load Test Failed");
          done();
        })
        .catch((e) => {
          console.log(e);
          done(e);
        });
  });

  it('Load Test Value Transfer', function (done) {
    console.log("-----------------------------------------------------");
    console.log("Running Load Test Value Transfer");
    console.log("-----------------------------------------------------");

    loadTestValue()
        .then((success) => {
          assert(success === true, "Load Test Failed");
          done();
        })
        .catch((e) => {
          console.log(e);
          done(e);
        });
  });
});