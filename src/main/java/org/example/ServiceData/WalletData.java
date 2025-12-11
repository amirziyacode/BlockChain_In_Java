package org.example.ServiceData;

import org.example.model.Wallet;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.*;

public class WalletData {

    private Wallet wallet;

    private static WalletData instance;

    static {
        instance = new WalletData();
    }

    public static WalletData getInstance() {
        return instance;
    }

    public void loadWallet() throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException{
        Connection walletConnection = DriverManager.getConnection("jdbc:sqlite:wallet.db");
        Statement walletStatement = walletConnection.createStatement();
        ResultSet resultSet = walletStatement.executeQuery("SELECT * FROM Wallet");
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        PublicKey publicKey = null;
        PrivateKey privateKey = null;
        while (resultSet.next()) {
            publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(resultSet.getBytes("PUBLIC_KEY")));
            privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(resultSet.getBytes("PRIVATE_KEY")));
        }
        this.wallet = new Wallet(publicKey, privateKey);
    }

    public Wallet getWallet() {
        return wallet;
    }
}
