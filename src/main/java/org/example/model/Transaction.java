package org.example.model;

import sun.security.provider.DSAPublicKeyImpl;

import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.Signature;
import java.security.SignatureException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;

public class Transaction implements Serializable {
    private byte[] from;
    private String fromFX;
    private byte[] to;
    private String toFX;
    private Integer value;
    private String timeStamp;
    private byte[] signature;

    public byte[] getFrom() {
        return from;
    }

    public void setFrom(byte[] from) {
        this.from = from;
    }

    public String getFromFX() {
        return fromFX;
    }

    public void setFromFX(String fromFX) {
        this.fromFX = fromFX;
    }

    public byte[] getTo() {
        return to;
    }

    public void setTo(byte[] to) {
        this.to = to;
    }

    public String getToFX() {
        return toFX;
    }

    public void setToFX(String toFX) {
        this.toFX = toFX;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public String getSignatureFX() {
        return signatureFX;
    }

    public void setSignatureFX(String signatureFX) {
        this.signatureFX = signatureFX;
    }

    public Integer getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(Integer ledgerId) {
        this.ledgerId = ledgerId;
    }

    private String signatureFX;
    private Integer ledgerId;

    public Transaction(byte[] from, String fromFX, byte[] to, String toFX, Integer value, String timeStamp, byte[] signature, String signatureFX, Integer ledgerId) {
        this.from = from;
        this.fromFX = fromFX;
        this.to = to;
        this.toFX = toFX;
        this.value = value;
        this.timeStamp = timeStamp;
        this.signature = signature;
        this.signatureFX = signatureFX;
        this.ledgerId = ledgerId;
    }


    //Constructor for creating a new transaction and signing it.
    public Transaction(Wallet fromWallet,
                       byte[] toAddress,
                       Integer value, Integer ledgerId, Signature signing) throws InvalidKeyException, SignatureException {
        Base64.Encoder encoder = Base64.getEncoder();
        this.from = fromWallet.getPublicKey().getEncoded();
        this.fromFX = encoder.encodeToString(fromWallet.getPublicKey().getEncoded());
        this.to = toAddress;
        this.toFX = encoder.encodeToString(toAddress);
        this.value = value;
        this.ledgerId = ledgerId;
        this.timeStamp = LocalDateTime.now().toString();
        signing.initSign(fromWallet.getPrivateKey());
        String sr = this.toString();
        signing.update(sr.getBytes());
        this.signature = signing.sign();
        this.signatureFX = encoder.encodeToString(this.signature);
    }

    public Boolean isVerified(Signature sign) throws SignatureException ,InvalidKeyException{
        sign.initVerify(new DSAPublicKeyImpl(this.getFrom()));
        sign.update(this.toString().getBytes());
        return sign.verify(this.signature);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "from=" + Arrays.toString(from) +
                ", to=" + Arrays.toString(to) +
                ", value=" + value +
                ", timeStamp= " + timeStamp +
                ", ledgerId=" + ledgerId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;
        Transaction that = (Transaction) o;
        return Arrays.equals(getSignature(), that.getSignature());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getSignature());
    }
}
