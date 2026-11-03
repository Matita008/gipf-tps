package gipf.client;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            ClientConnector.init("localhost", 5000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ClientManager.registerListeners();

        ClientManager.joinRandomRoom();
    }
}
