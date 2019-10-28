package org.aion.zokrates;

public enum ProvingScheme {
    G16("g16"),
    GM17("gm17"),
    PGHR13("pghr13");

    private String name;

    ProvingScheme(String envUrl) {
        this.name = envUrl;
    }

    @Override
    public String toString() {
        return name;
    }
}
