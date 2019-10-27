package org.aion.zokrates;

import org.aion.tetryon.G1Point;

import static org.aion.tetryon.Verifier.Proof;

import org.aion.tetryon.G2Point;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.math.BigInteger;


public class Util {

    public static String sanitizeHex(String s) {
        if (s != null && s.toLowerCase().startsWith("0x"))
            return s.substring(2);

        return s;
    }

    public static Proof parseProof(String proofStr) throws ParseException {
        JSONParser jsonParser = new JSONParser();
        JSONObject obj = (JSONObject) jsonParser.parse(proofStr);

        // construct proof
        JSONObject p = (JSONObject) obj.get("proof");

        JSONArray a = (JSONArray) p.get("a");
        G1Point p_a = new G1Point(
                sanitizeHex((String) a.get(0)),
                sanitizeHex((String) a.get(1)));

        JSONArray b = (JSONArray) p.get("b");
        JSONArray b_x = (JSONArray) b.get(0);
        JSONArray b_y = (JSONArray) b.get(1);

        G2Point p_b = new G2Point(
                sanitizeHex((String) b_x.get(1)), sanitizeHex((String) b_x.get(0)),
                sanitizeHex((String) b_y.get(1)), sanitizeHex((String) b_y.get(0)));

        JSONArray c = (JSONArray) p.get("c");
        G1Point p_c = new G1Point(
                sanitizeHex((String) c.get(0)),
                sanitizeHex((String) c.get(1)));

        return new Proof(p_a, p_b, p_c);
    }

    public static BigInteger[] parseInput(String inputStr) throws ParseException {
        JSONParser jsonParser = new JSONParser();
        JSONObject obj = (JSONObject) jsonParser.parse(inputStr);

        JSONArray i = (JSONArray) obj.get("inputs");

        BigInteger[] input = new BigInteger[i.size()];
        for (int x = 0; x < input.length; x++) {
            input[x] = new BigInteger(sanitizeHex((String) i.get(x)), 16);
        }

        return input;
    }
}
