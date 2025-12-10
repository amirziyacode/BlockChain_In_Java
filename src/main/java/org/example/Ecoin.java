package org.example;


import javafx.application.Application;
import javafx.stage.Stage;
import org.example.threads.MiningThread;
import org.example.threads.PeerClient;
import org.example.threads.PeerServer;
import org.example.threads.UI;

public class Ecoin extends Application {
    @Override
    public void start(Stage stage) throws Exception {
//        new UI().start(primaryStage);
        new PeerClient().start();
        new PeerServer(6000).start();
        new MiningThread().start();
    }


}
