1. Have an account that has at-least 10 Aion. Put the secret key in the .env file. 
2. Have a connection to the Tetryon testnet or a local Aion node in Tetryon mode
3. Give the connection string to the testbench through command line argument. If no argument is given, http://127.0.0.1:8545 is used. 
4. The testbench currently tests the SquarePreimage SNARK verifier by: 
	a. Deploying the contract
	b. Check if positive test case passes
	c. Check if negative test case passes
	d. Run a load test. 