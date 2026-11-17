package gipf.tests.move;

import gipf.client.*;
import gipf.shared.Events;
import gipf.shared.SharedConstants;
import gipf.tests.TestData;
import lombok.*;

public class Client {
    @SneakyThrows
    public static void main(String[] args) {
        TestData.init();
        ClientConnector.init("localhost", SharedConstants.defaultPort);
        ClientManager.registerListeners();

        //ClientManager.joinRandomRoom();

        Events.CLIENT_PING.fire(true);

        Thread helper;
        if (args == null) {
            //client2
            helper = new Thread(() -> {
                while(true) {
                    try {
                        Runtime.getRuntime().wait(1_000);
                        Thread.yield();
                    } catch (Throwable ignored) { }
                }
            }, "test-helper-client2");
        } else {
            helper = new Thread(() -> {
                while(true) {
                    try {
                        Runtime.getRuntime().wait(1_000);
                        Thread.yield();
                    } catch (Throwable ignored) { }
                }
            }, "test-helper-client");
        }
        helper.setPriority(Thread.MIN_PRIORITY);
        helper.start();

        ClientManager.joinRandomRoom();
    }
}
