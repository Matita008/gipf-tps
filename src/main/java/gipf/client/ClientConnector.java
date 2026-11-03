package gipf.client;

import gipf.shared.Events;
import gipf.shared.SharedConstants;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.*;
import lombok.experimental.*;

/**
 * Class to manage the connection to the server
 */
@UtilityClass
public class ClientConnector {
    private final AtomicBoolean active = new AtomicBoolean(false);
    @Getter private Thread networkThread;
    private ObjectInputStream inputStream   /* ois */;
    private ObjectOutputStream outputStream /* oii */;
    private final Queue<Events.Event> eventQueue = new LinkedList<>();
    private static Socket socket;

    public void init(String host, int port) throws IOException {
        socket = new Socket(host, port);
        socket.setSoTimeout(100); //.1s blocking max

        init(socket.getInputStream(), socket.getOutputStream());
    }

    public void init(InputStream in, OutputStream out) throws IOException {
        if (active.getAndSet(true)) {
            throw new IllegalStateException("Thread already running");
        }

        outputStream = new ObjectOutputStream(new BufferedOutputStream(out));
        outputStream.flush();
        out.flush(); // force flush
        inputStream = new ObjectInputStream(new BufferedInputStream(in));

        networkThread = new Thread(() -> {
            while (active.get() || !eventQueue.isEmpty()) {
                try {
                    poolEvent();
                } catch (Exception e) {
                    System.err.println("An error occurred in the network thread: " + e.getLocalizedMessage());
                    e.printStackTrace(System.err);
                }
            }
            System.out.println("[NET] disconnected\n[NET]\tbye!");
        }, "network-thread-connector");

        networkThread.setDaemon(true);
        networkThread.start();

        Events.fire(Events.CLIENT_CONNECT, SharedConstants.initPacket);
    }

    public void close() {
        if (!active.getAndSet(false)) return;
        eventQueue.add(new Events.Event(Events.CLIENT_QUIT.code(), null));
        System.out.println("Client disconnecting; queue: " + eventQueue.size());
    }

    public void shutdown() {
        if (active.get()) throw new IllegalStateException("ClientConnector.close() was not called before shutdown()");
        if (networkThread == null) return;
        try {
            networkThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        networkThread = null;
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Errore nella chiusura del socket: " + e.getLocalizedMessage());
            e.printStackTrace(System.err);
        }
        socket = null;
    }

    /** add an event to the list that is sent to the server */
    public void manageEvent(Events.EventHandler<?> event) {
        event.addHandler(data -> handleEvent(event.code(), data));
    }

    void handleEvent(int code, Object data) {
        if (!active.get()) return;
        Events.Event e = new Events.Event(code, data);
        eventQueue.add(e);
    }

    private void poolEvent() throws IOException {
        boolean didSomething = false;
        { // scoped so the jvm may reclaim it a bit early
            Events.Event event = eventQueue.peek();
            if (event != null) {
                System.out.println("[NET] sending: " + event);
                didSomething = true;
                eventQueue.remove();
                outputStream.writeObject(event);
                outputStream.flush();
            }
        }

        try {
            Object data = inputStream.readObject();
            didSomething = true;

            if (!(data instanceof Events.Event(int code, Object event))) {
                return; //TODO: how? (mitm)
            }
            Events.getEventByCode(code).fireUnsafe(event);
            System.out.println("[NET] received: " + data);
        } catch (SocketTimeoutException | EOFException ignored) {
            //no data available
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }

        if (!didSomething) {
            Thread.onSpinWait();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) { }
        }
    }
}
