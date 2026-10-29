package gipf.server;

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

        int port = -1;
        try {
            port = Integer.parseInt(System.getProperty("port"));
        } catch (Exception e) {
            System.err.println("[INIT] porta non valida: " + e.getMessage());
        }

        if (port <= 0 || port > Math.pow(16, 2) - 1) {
            System.out.println("Nessuna porta specificata o inserita porta non valida");
            port = 5000;
        }


        try (ServerSocket s = new ServerSocket(port)) {
            System.out.println("Listening on port " + port);

            ServerConnector.init(s);

            System.out.println("press a key to start shutdown sequence");
            //noinspection ResultOfMethodCallIgnored
            System.in.read();
        } catch (Exception e) {
            System.err.println("[MAIN] Errore " + e.getClass() + ": " + e.getLocalizedMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
