package gipf.tests.net;

import gipf.server.ServerConnector;
import gipf.shared.Events;
import lombok.*;

import java.net.*;

public class Server {
    @SneakyThrows
    public static void main(String[] args) {
        TestData.init();
        @Cleanup
        ServerSocket s = new ServerSocket(TestData.port);
        System.out.println("listening!");

        ServerConnector.init(s);

        System.in.read();
    }
}
