package gipf.client;

import gipf.shared.Events;
import lombok.*;
import lombok.experimental.*;

import java.io.*;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class to manage connection to the server
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
            while (active.get()) {
                try {
                    poolEvent();
                } catch (Exception e) {
                    System.err.println("An error occurred in the network thread: " + e.getLocalizedMessage());
                    e.printStackTrace(System.err);
                }
            }
        }, "network-thread-connector");

        thread.setDaemon(true);
        thread.start();
    }

    public void quit() {
        if (active.getAndSet(false)) thread.interrupt();
        qin.clear();
    }

    /** add an event to the list that are sent to the server */
    public void manageEvent(Events.EventHandler<?> event) {
        event.addHandler(data -> handleEvent(event.code(), data));
    }

    void handleEvent(int code, Object data) {
        Events.Event e = new Events.Event(code, data);
        qin.add(e);
    }

    private static void poolEvent() throws IOException {
        boolean didSomething = false;
        { // scoped so the jvm may reclaim it a bit early
            Events.Event event = qin.peek();
            if (event != null) {
                didSomething = true;
                qin.remove();
                //oii.writeBoolean(true);
                oii.writeObject(event);
                oii.flush();
                System.out.println("sent!");
            }
        }

        try {
            Object data = ois.readObject();
            didSomething = true;

            if (!(data instanceof Events.Event(int code, Object event))) {
                return; //TODO: how? (mitm)
            }
            Events.getEventByCode(code).fireUnsafe(event);
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
