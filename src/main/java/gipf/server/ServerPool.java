package gipf.server;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.*;
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

    public static void shutdown() {
        connectionAdder = null;
        for(var th : threads) {
            th.active.set(false);
        }
    }

    private void initExecutor() {
        String type = System.getProperty("gipf.poolType", "");
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
                if (!type.equalsIgnoreCase("thread_per_client") && !type.isBlank()) {
                    System.err.println("[INIT] Warning: gipf.poolType is set to an unknown value: " + type);
                }
                connectionAdder = client -> {
                    var th = new NettyThread();
                    th.addClient(client);
                    th.start();
                    th.terminateOnEmpty = true;
                    threads.add(th);
                };
                break;
        }
    }

    static {
        initExecutor();
        try {
            idleWait = Integer.parseInt(System.getProperty("gipf.idleWait", "500" /*.5s*/));
        } catch (Exception e) {
            System.err.println("[INIT] `gipf.idleWait` non è valido: " + e.getClass() + ": " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private class NettyThread extends Thread {
        final List<ServerConnector> threadClients = new CopyOnWriteArrayList<>(); //COWal: muuuu
        public boolean terminateOnEmpty = false;
        public final AtomicBoolean active = new AtomicBoolean(true);

        public NettyThread() {
            super(tg, "netty-" + threadCount.getAndIncrement());
        }

        //aggiungi un socket a quelli gestiti da questo thread
        void addClient(ServerConnector client) {
            threadClients.add(client);
        }

        @Override
        public void run() {
            while (active.get()) {
                //if allowed and no clients are connected
                if (terminateOnEmpty && threadClients.isEmpty()) {
                    active.set(false);
                    continue;
                }

                List<ServerConnector> toRemove = new LinkedList<>();
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

                    if (!client.connActive.get()) {
                        toRemove.add(client);
                    }
                }

                for(var del : toRemove) {
                    threadClients.remove(del);

                    //remove from room
                    if (del.room != null) {
                        del.room.removeClient(del);
                    }
                }

                //se nessun socket ha gestito alcun evento aspetta prima di rieseguire il ciclo
                if (!didSomething) {
                    try {
                        Thread.yield();
                        //noinspection BusyWait
                        Thread.sleep(idleWait);
                    } catch (InterruptedException ignored) { }
                }
            }

            active.set(false);
            threads.remove(this);
        }
    }
}
