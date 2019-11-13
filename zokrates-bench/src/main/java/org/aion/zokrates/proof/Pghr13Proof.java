package org.aion.zokrates.proof;

import org.aion.zokrates.ProvingScheme;
import org.aion.zokrates.bn128.Fp;
import org.aion.zokrates.bn128.G1Point;
import org.aion.zokrates.bn128.G2Point;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.Arrays;

import static org.aion.zokrates.Util.trimHexPrefix;

@SuppressWarnings("WeakerAccess")
public class Pghr13Proof implements Proof {
    private final ProvingScheme provingScheme = ProvingScheme.PGHR13;

    private final G1Point a;
    private final G1Point a_p;
    private final G2Point b;
    private final G1Point b_p;
    private final G1Point c;
    private final G1Point c_p;
    private final G1Point k;
    private final G1Point h;

    public Pghr13Proof(G1Point a, G1Point a_p,
                 G2Point b, G1Point b_p,
                 G1Point c, G1Point c_p,
                 G1Point k, G1Point h) {
        this.a = a;
        this.a_p = a_p;
        this.b = b;
        this.b_p = b_p;
        this.c = c;
        this.c_p = c_p;
        this.k = k;
        this.h = h;
    }

    // serialized as a | b | c
    public byte[] serialize() {
        byte[] s = new byte[Fp.ELEMENT_SIZE*18];

        byte[] aByte = this.a.serialize();
        byte[] apByte = this.a_p.serialize();
        byte[] bByte = this.b.serialize();
        byte[] bpByte = this.b_p.serialize();
        byte[] cByte = this.c.serialize();
        byte[] cpByte = this.c_p.serialize();
        byte[] kByte = this.k.serialize();
        byte[] hByte = this.h.serialize();

        System.arraycopy(aByte, 0, s, 0, aByte.length);
        System.arraycopy(apByte, 0, s, 2*Fp.ELEMENT_SIZE, apByte.length);
        System.arraycopy(bByte, 0, s, 4*Fp.ELEMENT_SIZE, bByte.length);
        System.arraycopy(bpByte, 0, s, 8*Fp.ELEMENT_SIZE, bpByte.length);
        System.arraycopy(cByte, 0, s, 10*Fp.ELEMENT_SIZE, cByte.length);
        System.arraycopy(cpByte, 0, s, 12*Fp.ELEMENT_SIZE, cpByte.length);
        System.arraycopy(kByte, 0, s, 14*Fp.ELEMENT_SIZE, kByte.length);
        System.arraycopy(hByte, 0, s, 16*Fp.ELEMENT_SIZE, hByte.length);

        return s;
    }

    public static Pghr13Proof deserialize(byte[] data) {
        G1Point a = G1Point.deserialize(Arrays.copyOfRange(data, 0, 2*Fp.ELEMENT_SIZE));
        G1Point a_p = G1Point.deserialize(Arrays.copyOfRange(data, 2*Fp.ELEMENT_SIZE, 4*Fp.ELEMENT_SIZE));
        G2Point b = G2Point.deserialize(Arrays.copyOfRange(data, 4*Fp.ELEMENT_SIZE, 8*Fp.ELEMENT_SIZE));
        G1Point b_p = G1Point.deserialize(Arrays.copyOfRange(data, 8*Fp.ELEMENT_SIZE, 10*Fp.ELEMENT_SIZE));
        G1Point c = G1Point.deserialize(Arrays.copyOfRange(data, 10*Fp.ELEMENT_SIZE, 12*Fp.ELEMENT_SIZE));
        G1Point c_p = G1Point.deserialize(Arrays.copyOfRange(data, 12*Fp.ELEMENT_SIZE, 14*Fp.ELEMENT_SIZE));
        G1Point k = G1Point.deserialize(Arrays.copyOfRange(data, 14*Fp.ELEMENT_SIZE, 16*Fp.ELEMENT_SIZE));
        G1Point h = G1Point.deserialize(Arrays.copyOfRange(data, 16*Fp.ELEMENT_SIZE, 18*Fp.ELEMENT_SIZE));

        return new Pghr13Proof(a, a_p, b, b_p, c, c_p, k, h);
    }

    public static Pghr13Proof parseJson(String proof) throws Exception {
        JSONParser jsonParser = new JSONParser();
        JSONObject p = (JSONObject) jsonParser.parse(proof);

        JSONArray aJson = (JSONArray) p.get("a");
        G1Point a = new G1Point(
                trimHexPrefix((String) aJson.get(0)),
                trimHexPrefix((String) aJson.get(1)));

        JSONArray apJson = (JSONArray) p.get("a_p");
        G1Point a_p = new G1Point(
                trimHexPrefix((String) apJson.get(0)),
                trimHexPrefix((String) apJson.get(1)));

        JSONArray bJson = (JSONArray) p.get("b");
        JSONArray bxJson = (JSONArray) bJson.get(0);
        JSONArray byJson = (JSONArray) bJson.get(1);

        G2Point b = new G2Point(
                trimHexPrefix((String) bxJson.get(1)), trimHexPrefix((String) bxJson.get(0)),
                trimHexPrefix((String) byJson.get(1)), trimHexPrefix((String) byJson.get(0)));

        JSONArray bpJson = (JSONArray) p.get("b_p");
        G1Point b_p = new G1Point(
                trimHexPrefix((String) bpJson.get(0)),
                trimHexPrefix((String) bpJson.get(1)));

        JSONArray cJson = (JSONArray) p.get("c");
        G1Point c = new G1Point(
                trimHexPrefix((String) cJson.get(0)),
                trimHexPrefix((String) cJson.get(1)));

        JSONArray cpJson = (JSONArray) p.get("c_p");
        G1Point c_p = new G1Point(
                trimHexPrefix((String) cpJson.get(0)),
                trimHexPrefix((String) cpJson.get(1)));

        JSONArray kJson = (JSONArray) p.get("k");
        G1Point k = new G1Point(
                trimHexPrefix((String) kJson.get(0)),
                trimHexPrefix((String) kJson.get(1)));

        JSONArray hJson = (JSONArray) p.get("h");
        G1Point h = new G1Point(
                trimHexPrefix((String) hJson.get(0)),
                trimHexPrefix((String) hJson.get(1)));

        return new Pghr13Proof(a, a_p, b, b_p, c, c_p, k, h);
    }

    public ProvingScheme getProvingScheme() { return provingScheme; }
}
