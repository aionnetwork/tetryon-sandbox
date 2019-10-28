package org.aion.zokrates.proof;

import org.aion.zokrates.ProvingScheme;

public interface Proof {
    ProvingScheme getProvingScheme();
    byte[] serialize();
}
