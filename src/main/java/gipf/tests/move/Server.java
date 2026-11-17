package gipf.tests.move;

import gipf.server.*;
import gipf.tests.TestData;

public class Server {
    public static void main(String[] args) {
        TestData.init();

        Thread helper = new Thread(() -> {
            while(true) {
                try {
                    Runtime.getRuntime().wait(1_000);
                    Thread.yield();
                } catch (Throwable ignored) { }
            }
        }, "test-helper-server");

        helper.setPriority(Thread.MIN_PRIORITY);
        helper.setDaemon(true);
        helper.start();

        ServerMain.main(args);

        for(int i = 0; i <10; i++) {
            GameRoom.getRoom("auto-" + i).keep=true;
        }
    }
}
