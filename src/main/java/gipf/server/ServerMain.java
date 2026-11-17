package gipf.server;

import gipf.shared.SharedConstants;

import java.net.ServerSocket;

public class ServerMain {
    public static boolean debugPackets = getOpt("gipf.server.printPackets");

    static boolean getOpt(String key) {
        return Boolean.parseBoolean(System.getProperty(key, "false"));
    }

    public static void main(String[] args) {
        ServerListeners.registerListeners();
        ServerListeners.registerStreamedEvents();
        ServerPool.ensureInitialized();

        int port = SharedConstants.defaultPort;
        if (System.getProperty("port") != null) {
            try {
                port = Integer.parseInt(System.getProperty("port"));
            } catch (Exception e) {
                System.err.println("[INIT] porta non valida: " + e.getMessage());
            }

            if (port <= 0 || port > Math.pow(16, 2) - 1) {
                System.out.println("Inserita porta non valida");
                port = SharedConstants.defaultPort;
            }
        }


        try (ServerSocket s = new ServerSocket(port)) {
            System.out.println("Listening on port " + port);

            ServerConnector.init(s);

            System.out.println("press a key to start shutdown sequence");
            //noinspection ResultOfMethodCallIgnored
            System.in.read();
            System.out.println("Shutting down, bye!");

            ServerConnector.active.set(false);
            ServerPool.shutdown();
        } catch (Exception e) {
            System.err.println("[MAIN] Errore " + e.getClass() + ": " + e.getLocalizedMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
