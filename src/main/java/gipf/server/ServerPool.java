package gipf.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import lombok.experimental.*;

/**
 * Classe per gestire le associazione socket - thread
 */
@UtilityClass
public class ServerPool {
    private final ThreadGroup tg = new ThreadGroup("netty");
    private final AtomicInteger threadCount = new AtomicInteger(0);
    // delay che viene eseguito (in ms) se un thread non esegue operazioni di I/O
    private int idleWait = 100;
    private final List<NettyThread> threads = new ArrayList<>();
    // funzione chiamata quando viene creato un socket
    private Consumer<ServerConnector> connectionAdder;

    public void addConnection(ServerConnector client) {
        connectionAdder.accept(client);
    }

    /// funzione vuota che serve a popolare connectionAdder
    public void ensureInitialized() {
        //this should be impossible (see <clinit> {Clint for stardew friends})
        if (connectionAdder == null) throw new IllegalStateException();
    }

    private void initExecutor() {
        String type = System.getProperty("gipf.poolType", "THREAD_PER_ROOM");
        switch (type.toLowerCase()) {
            //singolo thread: tutti i socket usano lo stesso thread
            case "single_thread":
                var netty = new NettyThread();
                netty.start();
                threads.add(netty);
                connectionAdder = netty::addClient;
                break;
            // usa un gruppo fisso di thread
            case "pool":
                // default: numero di core * 2
                int count = Runtime.getRuntime().availableProcessors() * 2;
                try {
                    count = Integer.parseInt(System.getProperty("gipf.poolSize"));
                } catch (Exception e) {
                    System.out.println("[INIT] Errore nel parsing del numero di thread da usare: " + e.getClass() + ": " + e.getMessage());
                }

                for (int i = 0; i < count; i++) {
                    NettyThread nettyThread = new NettyThread();
                    nettyThread.start();
                    threads.add(nettyThread);
                }

                AtomicInteger nextTh = new AtomicInteger(0);
                connectionAdder = client -> {
                    // cerca di bilanciare il numero di socket in ciascun thread
                    threads.get(nextTh.getAndIncrement() % threads.size()).addClient(client);
                };
                break;
            // default: usa un thread separato per ciascun socket
            default:
                if (!type.equalsIgnoreCase("thread_per_client")) {
                    System.out.println("[INIT] Warning: gipf.poolType is set to an unknown value");
                }
                connectionAdder = client -> {
                    var th = new NettyThread();
                    th.addClient(client);
                    th.start();
                    threads.add(th);
                };
                break;
        }
    }

    static {
        initExecutor();
        try {
            idleWait = Integer.parseInt(System.getProperty("gipf.idleWait"));
        } catch (Exception e) {
            System.err.println("[INIT] `gipf.idleWait` non è valido: " + e.getClass() + ": " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private class NettyThread extends Thread {
        final List<ServerConnector> threadClients = new CopyOnWriteArrayList<>(); //COWal: muuuu

        public NettyThread() {
            super(tg, "netty-" + threadCount.getAndIncrement());
        }

        //aggiungi un socket a quelli gestiti da questo thread
        void addClient(ServerConnector client) {
            threadClients.add(client);
        }

        @Override
        public void run() {
            boolean didSomething = false;
            //per ciascun socket
            for (var client : threadClients) {
                try {
                    // prova a inviare/ricevere eventi
                    didSomething |= client.poolEvents();
                } catch (Exception e) {
                    System.err.println("[NETTY] [" + getName() + "] Errore durante il polling degli eventi: " + e.getClass() + ": " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }

            //se nessun socket ha gestito alcun evento aspetta prima di rieseguire il ciclo
            if (!didSomething) {
                try {
                    Thread.sleep(idleWait);
                } catch (InterruptedException ignored) { }
            }
        }
    }
}
