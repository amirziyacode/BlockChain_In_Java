package org.example.threads;

import java.io.IOException;
import java.net.ServerSocket;

public class PeerServer extends Thread {

    private ServerSocket serverSocket;

    public PeerServer(int socketPort) throws IOException {
        this.serverSocket = new ServerSocket(socketPort);
    }

    @Override
    public void run() {
        while (true) {
            try {
                new PeerRequestThread(serverSocket.accept()).start();
            }catch (IOException ex){
                ex.printStackTrace();
            }
        }
    }
}
