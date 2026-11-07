package gipf.server;

import gipf.shared.Events;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.*;

public class ServerConnector {
    private static final ThreadLocal<ServerConnector> netThreads = new ThreadLocal<>();
    private static final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicBoolean connActive = new AtomicBoolean(true);
    @Getter private static Thread listener;
    private final ObjectInputStream inputStream;
    private final ObjectOutputStream oii;
    private final Queue<Events.Event> qin = new LinkedList<>();
    public GameRoom room;

    public ServerConnector(Socket s) throws IOException {
        var out = s.getOutputStream();
        oii = new ObjectOutputStream(new BufferedOutputStream(out));
        oii.flush();
        out.flush(); // force flush
        inputStream = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
        netThreads.set(this);
    }

    public static void init(ServerSocket sock) throws IOException {
        if (active.getAndSet(true)) {
            throw new IllegalStateException("Listener thread already running");
        }
        //
        sock.setSoTimeout(1000); // 1s

        listener = new Thread(() -> {
            while (active.get()) {
                try {
                    Socket inbound = sock.accept();
                    if (inbound == null) continue;

                    //imposta un tempo di attesa massimo nella ricezione di dati (se dopo .1s non è stato ricevuto niente le chiamate a ois ritornano null)
                    inbound.setSoTimeout(100); //.1s blocking max

                    ServerPool.addConnection(new ServerConnector(inbound));
                } catch (Exception ignored) {} /* TODO: should only ignore some, the others should be logged */
            }
        }, "network-thread-connector");

        listener.start();
    }

    /**
     * Runs a runnable in a new thread with the same connector as the one from the current thread
     * @param r the runnable to run
     * @return the thread that runs the runnable (not started)
     */
    public static Thread runDeferred(Runnable r) {
        ServerConnector sth = netThreads.get();
        return new Thread(() -> {
            netThreads.set(sth);
            r.run();
        });
    }

    /**
     * Runs a runnable in the current thread with the connector set to {@code this}
     * @param r the runnable to run
     */
    public void run(Runnable r) {
        ServerConnector sth = netThreads.get();
        netThreads.set(this);
        r.run();
        netThreads.set(sth);
    }

    public static ServerConnector getCurrentConnector() {
        return netThreads.get();
    }

    /**
     * Makes an event be forwarded to the client(s)
     *
     * @param event the event type to forward
     * @param broadcastToRoom if true and in a room, the event is broadcasted to all clients in the same game room,
     *                         otherwise it's only sent to the client connected with the current thread
     */
    public static void manageEvent(Events.EventHandler<?> event, boolean broadcastToRoom) {
        event.addHandler(data -> handleEvent(event.code(), data, broadcastToRoom));
    }

    // aggiungi un evento in coda a quelli da essere inviati
    private static void handleEvent(int code, Object data, boolean broadcastToRoom /* invia anche al compagno */) {
        Events.Event e = new Events.Event(code, data);

        ServerConnector sth = netThreads.get();
        if (sth == null) return;

        sth.qin.add(e);
        if (broadcastToRoom && sth.room != null) {
            sth.room.getOtherPlayer(sth).qin.add(e);
        }
    }

    // metodo per inviare il primo evento dalla coda e/o ricevere un evento dal socket
    @SuppressWarnings("DuplicatedCode") //need a bit of though (ThreadLocal with id to filter as events are run on caller thread?)
    boolean poolEvents() throws IOException {
        try {
            boolean didSomething = false;
            { // scoped so the jvm may reclaim it a bit early
                Events.Event event = qin.peek();
                //se c'è un evento in coda da inviare
                if (event != null) {
                    if (ServerMain.debugPackets) System.out.println("[NET] sending: " + event);
                    didSomething = true;
                    //rimuovilo
                    qin.remove();
                    //invialo
                    oii.writeObject(event);
                    //assicurati che sia stato inviato
                    oii.flush();
                }
            }

            try {
                //prova a leggere dati dal socket
                Object data = inputStream.readObject();
                didSomething = true;

                if (!(data instanceof Events.Event(int code, Object event))) {
                    throw new AssertionError(); //TODO: how? (mitm)
                }
                Events.getEventByCode(code).fireUnsafe(event);
                if (ServerMain.debugPackets) System.out.println("[NET] received: " + data);
            } catch (SocketTimeoutException | EOFException ignored) {
                //non è stato ricevuto niente, prosegui normalmente
            } catch (ClassNotFoundException ex) {
                throw new AssertionError(ex);
            }

            return didSomething;
        } catch (SocketException clientDisconnected) {
            // il client si è disconnesso
            connActive.set(false);
            return true;
        }
    }
}
