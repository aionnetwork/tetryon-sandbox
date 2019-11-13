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
public class Gm17Proof implements Proof {
    private final ProvingScheme provingScheme = ProvingScheme.GM17;

    private final G1Point a;
    private final G2Point b;
    private final G1Point c;

    public Gm17Proof(G1Point a, G2Point b, G1Point c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    // serialized as a | b | c
    public byte[] serialize() {
        byte[] s = new byte[Fp.ELEMENT_SIZE*8];

        byte[] a = this.a.serialize();
        byte[] b = this.b.serialize();
        byte[] c = this.c.serialize();

        System.arraycopy(a, 0, s, 0, a.length);
        System.arraycopy(b, 0, s, 6*Fp.ELEMENT_SIZE - b.length, b.length);
        System.arraycopy(c, 0, s, 8*Fp.ELEMENT_SIZE - c.length, c.length);

        return s;
    }

    public static Gm17Proof deserialize(byte[] data) {
        G1Point a = G1Point.deserialize(Arrays.copyOfRange(data, 0, 2*Fp.ELEMENT_SIZE));
        G2Point b = G2Point.deserialize(Arrays.copyOfRange(data, 2*Fp.ELEMENT_SIZE, 6*Fp.ELEMENT_SIZE));
        G1Point c = G1Point.deserialize(Arrays.copyOfRange(data, 6*Fp.ELEMENT_SIZE, 8*Fp.ELEMENT_SIZE));

        return new Gm17Proof(a, b, c);
    }

    public static Gm17Proof parseJson(String proof) throws Exception {
        JSONParser jsonParser = new JSONParser();
        JSONObject p = (JSONObject) jsonParser.parse(proof);

        JSONArray a = (JSONArray) p.get("a");
        G1Point p_a = new G1Point(
                trimHexPrefix((String) a.get(0)),
                trimHexPrefix((String) a.get(1)));

        JSONArray b = (JSONArray) p.get("b");
        JSONArray b_x = (JSONArray) b.get(0);
        JSONArray b_y = (JSONArray) b.get(1);

        G2Point p_b = new G2Point(
                trimHexPrefix((String) b_x.get(1)), trimHexPrefix((String) b_x.get(0)),
                trimHexPrefix((String) b_y.get(1)), trimHexPrefix((String) b_y.get(0)));

        JSONArray c = (JSONArray) p.get("c");
        G1Point p_c = new G1Point(
                trimHexPrefix((String) c.get(0)),
                trimHexPrefix((String) c.get(1)));

        return new Gm17Proof(p_a, p_b, p_c);
    }

    public ProvingScheme getProvingScheme() { return provingScheme; }
}
