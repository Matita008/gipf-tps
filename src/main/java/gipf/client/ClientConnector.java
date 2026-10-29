package gipf.client;

import gipf.shared.Events;
import gipf.shared.SharedConstants;

import java.io.*;
import java.net.SocketTimeoutException;
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
    @Getter private Thread thread;
    private ObjectInputStream ois;
    private ObjectOutputStream oii;
    private final Queue<Events.Event> qin = new LinkedList<>();

    public void init(InputStream in, OutputStream out) throws IOException {
        if (active.getAndSet(true)) {
            throw new IllegalStateException("Thread already running");
        }

        oii = new ObjectOutputStream(new BufferedOutputStream(out));
        oii.flush();
        out.flush(); // force flush
        ois = new ObjectInputStream(new BufferedInputStream(in));

        thread = new Thread(() -> {
            while (active.get() || !qin.isEmpty()) {
                try {
                    poolEvent();
                } catch (Exception e) {
                    System.err.println("An error occurred in the network thread: " + e.getLocalizedMessage());
                    e.printStackTrace(System.err);
                }
            }
            System.out.println("[NET] disconnected\n[NET]\tbye!");
        }, "network-thread-connector");

        thread.setDaemon(true);
        thread.start();

        Events.fire(Events.CLIENT_CONNECT, SharedConstants.initPacket);
    }

    public void close() {
        if (!active.getAndSet(false)) return;
        qin.add(new Events.Event(Events.CLIENT_QUIT.code(), null));
        System.out.println("Client disconnecting; queue: " + qin.size());
    }

    public void shutdown() {
        if (active.get()) throw new IllegalStateException("ClientConnector.close() was not called before shutdown()");
        if (thread == null) return;
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        thread = null;
    }

    /** add an event to the list that is sent to the server */
    public void manageEvent(Events.EventHandler<?> event) {
        event.addHandler(data -> handleEvent(event.code(), data));
    }

    void handleEvent(int code, Object data) {
        if (!active.get()) return;
        Events.Event e = new Events.Event(code, data);
        qin.add(e);
    }

    private void poolEvent() throws IOException {
        boolean didSomething = false;
        { // scoped so the jvm may reclaim it a bit early
            Events.Event event = qin.peek();
            if (event != null) {
                System.out.println("[NET] sending: " + event);
                didSomething = true;
                qin.remove();
                oii.writeObject(event);
                oii.flush();
            }
        }

        try {
            Object data = ois.readObject();
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
