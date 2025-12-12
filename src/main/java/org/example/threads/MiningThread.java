package org.example.threads;

import org.example.ServiceData.BlockChainData;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class MiningThread extends  Thread {
    @Override
    public void run() {
        while (true) {
            long lastMinedBlock = LocalDateTime.parse(BlockChainData.getInstance().getCurrentBlockChain().getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
             if((lastMinedBlock + BlockChainData.getTimeoutInterval()) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)){
                 System.out.println("BlockChain is too old for mining! Update it from peers");
             }
             else if((lastMinedBlock+BlockChainData.getMiningInterval()) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) > 0){
                 System.out.println("BlockChain is current, mining will commence in " +
                         ((lastMinedBlock + BlockChainData.getMiningInterval()) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) ) + " seconds");
             }else{
                 System.out.println("MINING NEW BLOCK");
                 BlockChainData.getInstance().mineBlock();
                 System.out.println(BlockChainData.getInstance().getWalletBallanceFX());
             }
            System.out.println(LocalDateTime.parse(BlockChainData.getInstance()
                    .getCurrentBlockChain().getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC));
            try {
                Thread.sleep(2000);
                if (BlockChainData.getInstance().isExit()) { break; }
                BlockChainData.getInstance().setMiningPoints(BlockChainData.getInstance().getMiningPoints() + 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
