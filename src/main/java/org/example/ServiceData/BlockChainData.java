package org.example.ServiceData;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.model.Block;
import org.example.model.Transaction;
import org.example.model.Wallet;
import sun.security.provider.DSAPublicKeyImpl;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

public class BlockChainData {

    private final ObservableList<Transaction> newBlockTransactionsFX;
    private final ObservableList<Transaction> newBlockTransactions;
    private final LinkedList<Block> currentBlockChain = new LinkedList<>();
    private Block latestBlock;
    private boolean exit = false;
    private int miningPoints;
    private static final int TIMEOUT_INTERVAL = 65;
    private static final int MINING_INTERVAL = 60;
    //helper class.
    final private Signature signing = Signature.getInstance("SHA256withDSA");

    //singleton class
    private static BlockChainData instance;

    static {
        try {
            instance = new BlockChainData();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("NoSuchAlgorithmException");
        }
    }
    public BlockChainData() throws NoSuchAlgorithmException {
        newBlockTransactions = FXCollections.observableArrayList();
        newBlockTransactionsFX = FXCollections.observableArrayList();
    }

    Comparator<Transaction> transactionComparator = Comparator.comparing(Transaction::getTimeStamp);
    public ObservableList<Transaction> getTransactionLedgerFX() {
        newBlockTransactionsFX.clear();
        newBlockTransactions.sort(transactionComparator);
        newBlockTransactionsFX.addAll(newBlockTransactions);
        return FXCollections.observableArrayList(newBlockTransactionsFX);
    }

    public String getWalletBalanceFX(){
        return getBalance(currentBlockChain,newBlockTransactions,WalletData.getInstance().getWallet().getPublicKey()).toString();
    }

    private Integer getBalance(LinkedList<Block> blockChain, ObservableList<Transaction> currentLedger, PublicKey wallet) {
        Integer balance = 0;
        for(Block block : blockChain){
            for(Transaction transaction:block.getTransactionLedger()){
                if(Arrays.equals(transaction.getFrom(),wallet.getEncoded())){
                    balance -= transaction.getValue();
                }if(Arrays.equals(transaction.getTo(),wallet.getEncoded())){
                    balance += transaction.getValue();
                }
            }
        }


        for (Transaction transaction : currentLedger) {
            if(Arrays.equals(transaction.getFrom(),wallet.getEncoded())){
                balance -= transaction.getValue();
            }
        }


        return balance;
    }

    public void verifyBlockChain(LinkedList<Block> currentBlockChain) throws GeneralSecurityException {
        for (Block block : currentBlockChain) {
            if(!block.isVerified(signing)){
                throw new GeneralSecurityException("Block is not verified");
            }
            ArrayList<Transaction> transactionArrays = block.getTransactionLedger();
            for (Transaction transaction: transactionArrays){
                if(!transaction.isVerified(signing)){
                    throw new GeneralSecurityException("Transaction validation failed");
                }
            }
        }
    }

    public void addTransactionState(Transaction transaction){
        newBlockTransactions.add(transaction);
        newBlockTransactions.sort(transactionComparator);
    }

    public void addTransaction(Transaction transaction,boolean blockReward) throws GeneralSecurityException {
        try {
            if(getBalance(currentBlockChain,
                    newBlockTransactions,new DSAPublicKeyImpl(transaction.getFrom())) < transaction.getValue() && !blockReward){
                throw new GeneralSecurityException("Not enough funds by sender to record transaction");

            }else {
                Connection connection = DriverManager.getConnection("jdbc:sqlite:blockchain.db");

                PreparedStatement pstmt;
                pstmt = connection.prepareStatement("INSERT INTO TRANSACTIONS" +
                        "(\"FROM\", \"TO\", LEDGER_ID, VALUE, SIGNATURE, CREATED_ON) " +
                        " VALUES (?,?,?,?,?,?) ");
                pstmt.setBytes(1, transaction.getFrom());
                pstmt.setBytes(2, transaction.getTo());
                pstmt.setInt(3, transaction.getLedgerId());
                pstmt.setInt(4, transaction.getValue());
                pstmt.setBytes(5, transaction.getSignature());
                pstmt.setString(6, transaction.getTimeStamp());
                pstmt.executeUpdate();

                pstmt.close();
                connection.close();
            }
        } catch (SQLException e) {
            System.out.println("Problem with DB: " + e.getMessage());
        }
    }

    public void loadBlockChain() {
        try {
            Connection connection = DriverManager.getConnection
                    ("jdbc:sqlite:C:\\Users\\spiro\\IdeaProjects\\e-coin\\db\\blockchain.db");
            Statement stmt = connection.createStatement();
            ResultSet resultSet = stmt.executeQuery(" SELECT * FROM BLOCKCHAIN ");
            while (resultSet.next()) {
                this.currentBlockChain.add(new Block(
                        resultSet.getBytes("PREVIOUS_HASH"),
                        resultSet.getBytes("CURRENT_HASH"),
                        resultSet.getString("CREATED_ON"),
                        resultSet.getBytes("CREATED_BY"),
                        resultSet.getInt("LEDGER_ID"),
                        resultSet.getInt("MINING_POINTS"),
                        resultSet.getDouble("LUCK"),
                        loadTransactionLedger(resultSet.getInt("LEDGER_ID"))
                ));
            }

            latestBlock = currentBlockChain.getLast();
            Transaction transaction = new Transaction(new Wallet(),
                    WalletData.getInstance().getWallet().getPublicKey().getEncoded(),
                    100, latestBlock.getLedgerId() + 1, signing);
            newBlockTransactions.clear();
            newBlockTransactions.add(transaction);
            verifyBlockChain(currentBlockChain);
            resultSet.close();
            stmt.close();
            connection.close();
        } catch (SQLException | NoSuchAlgorithmException e) {
            System.out.println("Problem with DB: " + e.getMessage());
        } catch (GeneralSecurityException e) {
            System.out.println("Problem with GeneralSecurityException: " + e.getMessage());
        }
    }

    private ArrayList<Transaction> loadTransactionLedger(Integer ledgerID) throws SQLException {
        ArrayList<Transaction> transactions = new ArrayList<>();
        try {
            Connection connection = DriverManager.getConnection
                    ("jdbc:sqlite:blockchain.db");
            PreparedStatement stmt = connection.prepareStatement
                    (" SELECT  * FROM TRANSACTIONS WHERE LEDGER_ID = ?");
            stmt.setInt(1, ledgerID);
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                transactions.add(new Transaction(
                        resultSet.getBytes("FROM"),
                        resultSet.getBytes("TO"),
                        resultSet.getInt("VALUE"),
                        resultSet.getBytes("SIGNATURE"),
                        resultSet.getInt("LEDGER_ID"),
                        resultSet.getString("CREATED_ON")
                ));
            }
            resultSet.close();
            stmt.close();
            connection.close();
        } catch (SQLException e) {
            System.out.println("problem with DB: " + e.getMessage());
        }
        return transactions;
    }

    public void mineBlock() {
        try {
            finalizeBlock(WalletData.getInstance().getWallet());
            addBlock(latestBlock);
        } catch (SQLException | GeneralSecurityException e) {
            System.out.println("Problem with DB: " + e.getMessage());
        }
    }

    private void finalizeBlock(Wallet minersWallet) throws GeneralSecurityException, SQLException {
        latestBlock = new Block(BlockChainData.getInstance().currentBlockChain);
        latestBlock.setTransactionLedger(new ArrayList<>(newBlockTransactions));
        latestBlock.setTimeStamp(LocalDateTime.now().toString());
        latestBlock.setMinedBy(minersWallet.getPublicKey().getEncoded());
        latestBlock.setMiningPoints(miningPoints);
        signing.initSign(minersWallet.getPrivateKey());
        signing.update(latestBlock.toString().getBytes());
        latestBlock.setCurrentHash(signing.sign());
        currentBlockChain.add(latestBlock);
        miningPoints = 0;
        //Reward transaction
        latestBlock.getTransactionLedger().sort(transactionComparator);
        addTransaction(latestBlock.getTransactionLedger().getFirst(), true);
        Transaction transaction = new Transaction(new Wallet(), minersWallet.getPublicKey().getEncoded(),
                100, latestBlock.getLedgerId() + 1, signing);
        newBlockTransactions.clear();
        newBlockTransactions.add(transaction);
    }
    private void addBlock(Block block){
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:blockchain.db");
            PreparedStatement pstmt;
            pstmt = connection.prepareStatement
                    ("INSERT INTO BLOCKCHAIN(PREVIOUS_HASH, CURRENT_HASH, LEDGER_ID, CREATED_ON," +
                            " CREATED_BY, MINING_POINTS, LUCK) VALUES (?,?,?,?,?,?,?) ");
            pstmt.setBytes(1, block.getPrevHash());
            pstmt.setBytes(2, block.getCurrentHash());
            pstmt.setInt(3, block.getLedgerId());
            pstmt.setString(4, block.getTimeStamp());
            pstmt.setBytes(5, block.getMinedBy());
            pstmt.setInt(6, block.getMiningPoints());
            pstmt.setDouble(7, block.getLuck());
            pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        }
        catch (SQLException e){
            System.out.println("Problem with DB: " + e.getMessage());
        }
    }

    private void replaceBlockchainInDatabase(LinkedList<Block> receivedBC) {
        try {
            Connection connection = DriverManager.getConnection
                    ("jdbc:sqlite:blockchain.db");
            Statement clearDBStatement = connection.createStatement();
            clearDBStatement.executeUpdate(" DELETE FROM BLOCKCHAIN ");
            clearDBStatement.executeUpdate(" DELETE FROM TRANSACTIONS ");
            clearDBStatement.close();
            connection.close();
            for (Block block : receivedBC) {
                addBlock(block);
                boolean rewardTransaction = true;
                block.getTransactionLedger().sort(transactionComparator);
                for (Transaction transaction : block.getTransactionLedger()) {
                    addTransaction(transaction, rewardTransaction);
                    rewardTransaction = false;
                }
            }
        } catch (SQLException | GeneralSecurityException e) {
            System.out.println("Problem with DB: " + e.getMessage());
        }
    }








    public static BlockChainData getInstance() {
        return instance;
    }
}
