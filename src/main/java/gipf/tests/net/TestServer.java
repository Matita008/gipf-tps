package gipf.tests.net;

import gipf.server.*;
import gipf.shared.Events;

import java.net.ServerSocket;
import lombok.*;

/// A test server
public class TestServer {
    @SneakyThrows
    public static void main(String[] args) {
        TestData.init();
        ServerListeners.registerListeners();
        ServerListeners.registerStreamedEvents();
        ServerPool.ensureInitialized();

        // Be nice, register them before to not incur in any race condition/lose the first ones
        ServerConnector.manageEvent(Events.TEST_SERVER, true);
        // reply to clients
        Events.TEST_CLIENT.addHandler(e -> Events.TEST_SERVER.fire("server: data"));

        @Cleanup
        ServerSocket s = new ServerSocket(TestData.port);
        System.out.println("listening!");

        ServerConnector.init(s);

        System.out.println("Enter 'f' to force-quit, anything else to quit");
        System.out.println("(note that the server will keep running until you input something)");
        if (System.in.read() == 'f') {
            System.exit(0);
        }
    }
}
