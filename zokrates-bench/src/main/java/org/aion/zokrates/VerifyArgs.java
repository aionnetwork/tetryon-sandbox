package org.aion.zokrates;

import org.aion.zokrates.proof.Gm17Proof;
import org.aion.zokrates.proof.Groth16Proof;
import org.aion.zokrates.proof.Pghr13Proof;
import org.aion.zokrates.proof.Proof;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.math.BigInteger;

import static org.aion.zokrates.Util.trimHexPrefix;

public class VerifyArgs {
    private final Proof proof;
    private final ProvingScheme provingScheme;
    private final BigInteger[] input;

    public VerifyArgs(ProvingScheme provingScheme, Proof proof, BigInteger[] input) {
        this.provingScheme = provingScheme;
        this.proof = proof;
        this.input = input;
    }

    public Proof getProof() { return proof; }
    public ProvingScheme getProvingScheme() { return provingScheme; }
    public BigInteger[] getInput() { return input; }

    public static VerifyArgs parseJson(String json, ProvingScheme provingScheme) throws Exception {
        JSONParser jsonParser = new JSONParser();
        JSONObject obj = (JSONObject) jsonParser.parse(json);

        String p = ((JSONObject) obj.get("proof")).toJSONString();
        Proof proof;

        //noinspection SwitchStatementWithTooFewBranches
        switch (provingScheme) {
            case G16:
                proof = Groth16Proof.parseJson(p);
                break;
            case GM17:
                proof = Gm17Proof.parseJson(p);
                break;
            case PGHR13:
                proof = Pghr13Proof.parseJson(p);
                break;
            default:
                throw new IllegalArgumentException("unsupported proving scheme");
        }

        JSONArray i = (JSONArray) obj.get("inputs");

        BigInteger[] input = new BigInteger[i.size()];
        for (int x = 0; x < input.length; x++) {
            input[x] = new BigInteger(trimHexPrefix((String) i.get(x)), 16);
        }

        return new VerifyArgs(provingScheme, proof, input);
    }
}
