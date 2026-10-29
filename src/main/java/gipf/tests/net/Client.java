package gipf.tests.net;

import gipf.client.ClientConnector;
import gipf.shared.Events;
import lombok.*;

import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

/// A test client with no gui and fixed events
public class Client {
    @SneakyThrows
    public static void main(String[] args) {
        TestData.init();
        @Cleanup
        Socket s = new Socket("localhost", TestData.port);
        s.setSoTimeout(100); //.1s blocking max

        ClientConnector.init(s.getInputStream(), s.getOutputStream());

        ClientConnector.manageEvent(Events.TEST_CLIENT);
        AtomicBoolean exit = new AtomicBoolean(true);
        Events.TEST_SERVER.addHandler(e -> exit.set(false));

        Events.CLIENT_PING.fire(true);
        Events.TEST_CLIENT.fire("client: halo!");

        while(exit.get()) Thread.onSpinWait();

        ClientConnector.close();
        ClientConnector.shutdown();
        System.out.println("Client exiting");
    }
}
