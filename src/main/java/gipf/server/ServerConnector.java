package gipf.server;

import gipf.shared.Events;
import lombok.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public class ServerConnector extends Thread {
    private static final AtomicBoolean active = new AtomicBoolean(false);
    private static int count = 0;
    @Getter private static Thread listener;
    private static final ThreadGroup tg = new ThreadGroup("connection-receiver");
    private final ObjectInputStream ois;
    private final ObjectOutputStream oii;
    private final Queue<Events.Event> qin = new LinkedList<>();

    public ServerConnector(Socket s) throws IOException {
        super(tg, "network-handler-" + (count++));

        var out = s.getOutputStream();
        oii = new ObjectOutputStream(new BufferedOutputStream(out));
        oii.flush();
        out.flush(); // force flush
        ois = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
    }

    public static void init(ServerSocket sock) throws IOException {
        if (active.getAndSet(true)) {
            throw new IllegalStateException("Listening thread already running");
        }
        sock.setSoTimeout(1000); // 1s

        listener = new Thread(() -> {
            while (active.get()) {
                try {
                    Socket inbound = sock.accept();
                    if (inbound == null) throw new Exception("nope");

                    //TODO: explain/thinker
                    inbound.setSoTimeout(100); //.1s blocking max

                    ServerConnector conn = new ServerConnector(inbound);
                    conn.manageEvent(Events.TEST_SERVER); //TODO: testing only (see poolEvent() comment on DuplicatedCode)
                    conn.start();
                } catch (Exception ignored) {} /* TODO: should only ignore some, the others should be logged */
            }
        }, "network-thread-connector");

        listener.start();
    }

    public void quit() {
        if (active.getAndSet(false)) listener.interrupt();
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

    @Override
    public void run() {
        while (active.get()) {
            try {
                poolEvent();
            } catch (SocketException se) {
                System.out.println("Client disconnected!...");
                return;
            } catch (Exception e) {
                System.out.println("error in th " + getName() + ": " + e.getLocalizedMessage());
                e.printStackTrace(System.err);
            }
        }
    }

    @SuppressWarnings("DuplicatedCode") //need a bit of though (ThreadLocal with id to filter as events are run on caller thread?)
    private void poolEvent() throws IOException {
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

        try {/*
            if (ois.readBoolean()) {
                didSomething = true;
                System.out.println("DB: avail data");
                Events.TEST_SERVER.fire("server: data");
            } else {
                System.out.println("[DBG] readBool returned false");
            }*/

            Object data = ois.readObject();
            didSomething = true;

            if (!(data instanceof Events.Event(int code, Object event))) {
                return; //TODO: how? (mitm)
            }
            Events.getEventByCode(code).fireUnsafe(event);

            Events.TEST_SERVER.fire("server: data");
        } catch (SocketTimeoutException | EOFException ignored) {
            //no data available
        } catch (ClassNotFoundException ex) {
            throw new AssertionError(ex);
        }

        if (!didSomething) {
            Thread.onSpinWait();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) { }
        }
    }
}
