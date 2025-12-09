package org.example.model;

import java.io.Serializable;
import java.security.*;

public class Wallet  implements Serializable {

    private final KeyPair keyPair;

    public Wallet() throws NoSuchAlgorithmException {
        this(2048, KeyPairGenerator.getInstance("DSA"));
    }

    public Wallet(Integer keySize, KeyPairGenerator keyPairGenerator) {
        keyPairGenerator.initialize(keySize);
        this.keyPair = keyPairGenerator.generateKeyPair();
    }

    public Wallet(PublicKey publicKey, PrivateKey privateKey) {
        this.keyPair = new KeyPair(publicKey, privateKey);
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public PublicKey getPublicKey() {return this.keyPair.getPublic();}
    public PrivateKey getPrivateKey() {return this.keyPair.getPrivate();}
}
