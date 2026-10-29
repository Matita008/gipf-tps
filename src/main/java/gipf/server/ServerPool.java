package gipf.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import lombok.experimental.*;

@UtilityClass
public class ServerPool {
    private final ThreadGroup tg = new ThreadGroup("netty");
    private final AtomicInteger threadCount = new AtomicInteger(0);
    private int idleWait = 100;
    private final List<NettyThread> threads = new ArrayList<>();
    private Consumer<ServerConnector> connectionAdder;

    public void addConnection(ServerConnector client) {
        connectionAdder.accept(client);
    }

    public void ensureInitialized() {
        //this should be impossible (see <clinit> {Clint for stardew friends})
        if (connectionAdder == null) throw new IllegalStateException();
    }

    private void initExecutor() {
        String type = System.getProperty("gipf.poolType", "THREAD_PER_ROOM");
        switch (type.toLowerCase()) {
            case "single_thread":
                var netty = new NettyThread();
                netty.start();
                threads.add(netty);
                connectionAdder = netty::addClient;
                break;
            case "pool":
                int count = Runtime.getRuntime().availableProcessors() * 2;
                try {
                    count = Integer.parseInt(System.getProperty("gipf.poolSize"));
                } catch (Exception e) {
                    System.out.println("[INIT] Errore nel parsing del numero di thread da usare: " + e.getClass() + ": " + e.getMessage());
                }

                IntStream.range(0, count).mapToObj(i -> new NettyThread()).forEach(th -> {
                    th.start();
                    threads.add(th);
                });

                AtomicInteger nextTh = new AtomicInteger(0);
                connectionAdder = client -> {
                    threads.get(nextTh.getAndIncrement() % threads.size()).addClient(client);
                };
                break;
            default:
                System.out.println("[INIT] Warning: gipf.poolType is set to an unknown value");
            case "thread_per_client":
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
        final List<ServerConnector> threadClients = new CopyOnWriteArrayList<>(); ///muuuu

        public NettyThread() {
            super(tg, "netty-" + threadCount.getAndIncrement());
        }

        void addClient(ServerConnector client) {
            threadClients.add(client);
        }

        @Override
        public void run() {
            boolean didSomething = false;
            for (var client : threadClients) {
                try {
                    didSomething |= client.poolEvents();
                } catch (Exception e) {
                    System.err.println("[NETTY] [" + getName() + "] Errore durante il polling degli eventi: " + e.getClass() + ": " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }

            if (!didSomething) {
                try {
                    Thread.sleep(idleWait);
                } catch (InterruptedException ignored) { }
            }
        }
    }
}
