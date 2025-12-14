package org.example;


import javafx.application.Application;
import javafx.stage.Stage;
import org.example.ServiceData.BlockChainData;
import org.example.ServiceData.WalletData;
import org.example.model.Block;
import org.example.model.Transaction;
import org.example.model.Wallet;
import org.example.threads.MiningThread;
import org.example.threads.PeerClient;
import org.example.threads.PeerServer;
import org.example.threads.UI;
import java.security.*;
import java.sql.*;
import java.time.LocalDateTime;

public class Econ extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        new UI().start(stage);
        new PeerClient().start();
        new PeerServer(6000).start();
        new MiningThread().start();
    }

    @Override
    public void init() {
        try {
            //This creates your wallet if there is none and gives you a KeyPair.
            //We will create it in separate db for better security and ease of portability.

            Connection walletConnection = DriverManager.getConnection("jdbc:sqlite:/home/amirziya/Desktop/BlockChain/db/WALLET.db");
            Statement walletStatement = walletConnection.createStatement();
            walletStatement.executeUpdate("CREATE  TABLE IF NOT EXISTS WALLET (" +
                    " PRIVATE_KEY BLOB NOT NULL UNIQUE, " +
                    " PUBLIC_KEY BLOB NOT NULL UNIQUE, " +
                    " PRIMARY KEY (PRIVATE_KEY, PUBLIC_KEY)" +
                    ") "
            );
            ResultSet resultSet = walletStatement.executeQuery("SELECT * FROM WALLET");
            if(!resultSet.next()) {
                Wallet neWallet = new Wallet();
                byte[] pubBlob = neWallet.getPublicKey().getEncoded();
                byte[] prvBlob = neWallet.getPrivateKey().getEncoded();
                PreparedStatement pstmt = walletConnection
                        .prepareStatement("INSERT INTO WALLET(PRIVATE_KEY, PUBLIC_KEY) " +
                                " VALUES (?,?) ");
                pstmt.setBytes(1, prvBlob);
                pstmt.setBytes(2, pubBlob);
                pstmt.executeUpdate();
            }
            resultSet.close();
            walletStatement.close();
            walletConnection.close();
            WalletData.getInstance().loadWallet();

            //This will create the db tables with columns for the Blockchain.
            Connection blockchainConnection = DriverManager.getConnection("jdbc:sqlite:main.BLOCKCHAIN");
            Statement blockchainStatement = blockchainConnection.createStatement();
            blockchainStatement.executeUpdate("CREATE TABLE IF NOT EXISTS BLOCKCHAIN ( " +
                    " ID INTEGER NOT NULL UNIQUE, " +
                    " PREVIOUS_HASH BLOB UNIQUE, " +
                    " CURRENT_HASH BLOB UNIQUE, " +
                    " LEDGER_ID INTEGER NOT NULL UNIQUE, " +
                    " CREATED_ON  TEXT, " +
                    " CREATED_BY  BLOB, " +
                    " MINING_POINTS  TEXT, " +
                    " LUCK  NUMERIC, " +
                    " PRIMARY KEY( ID AUTOINCREMENT) " +
                    ")"
            );
            ResultSet resultSetBlocChain = blockchainStatement.executeQuery("SELECT * FROM BLOCKCHAIN");
            Transaction initBlockRewardTransaction = null;
            if(!resultSetBlocChain.next()) {
                Block firstBlock = new Block();
                firstBlock.setMinedBy(WalletData.getInstance().getWallet().getPublicKey().getEncoded());
                firstBlock.setTimeStamp(LocalDateTime.now().toString());

                Signature singnature = Signature.getInstance("SHA256withDSA");
                singnature.initSign(WalletData.getInstance().getWallet().getPrivateKey());
                singnature.update(firstBlock.toString().getBytes());
                firstBlock.setCurrentHash(singnature.sign());
                PreparedStatement pstmt = blockchainConnection
                        .prepareStatement("INSERT INTO BLOCKCHAIN(PREVIOUS_HASH, CURRENT_HASH , LEDGER_ID," +
                                " CREATED_ON, CREATED_BY,MINING_POINTS,LUCK ) " +
                                " VALUES (?,?,?,?,?,?,?) ");
                pstmt.setBytes(1,firstBlock.getPrevHash());
                pstmt.setBytes(2,firstBlock.getCurrentHash());
                pstmt.setInt(3,firstBlock.getLedgerId());
                pstmt.setString(4,firstBlock.getTimeStamp());
                pstmt.setBytes(5,WalletData.getInstance().getWallet().getPublicKey().getEncoded());
                pstmt.setInt(6,firstBlock.getMiningPoints());
                pstmt.setDouble(7,firstBlock.getLuck());
                pstmt.executeUpdate();
                Signature transSignature = Signature.getInstance("SHA256withDSA");
                initBlockRewardTransaction = new Transaction(WalletData.getInstance().getWallet()
                        ,WalletData.getInstance().getWallet().getPublicKey().getEncoded()
                        ,100,
                        1,
                        transSignature);
            }
            resultSetBlocChain.close();

            blockchainStatement.executeUpdate("CREATE TABLE IF NOT EXISTS TRANSACTIONS ( " +
                    " ID INTEGER NOT NULL UNIQUE, " +
                    " \"FROM\" BLOB, " +
                    " \"TO\" BLOB, " +
                    " LEDGER_ID INTEGER, " +
                    " VALUE INTEGER, " +
                    " SIGNATURE BLOB UNIQUE, " +
                    " CREATED_ON TEXT, " +
                    " PRIMARY KEY(ID AUTOINCREMENT) " +
                    ")"
            );
            if(initBlockRewardTransaction != null) {
                BlockChainData.getInstance().addTransaction(initBlockRewardTransaction,true);
                BlockChainData.getInstance().addTransactionState(initBlockRewardTransaction);
            }
            blockchainStatement.close();
            blockchainConnection.close();
        }catch (SQLException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            System.out.println("db failed: " + e.getClass());
        } catch (GeneralSecurityException e) {
            System.out.println("GeneralSecurityException: " + e.getClass());
        }

        BlockChainData.getInstance().loadBlockChain();
    }
}
