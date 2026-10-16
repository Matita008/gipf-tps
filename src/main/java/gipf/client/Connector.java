package gipf.client;

import gipf.shared.Events;
import gipf.shared.data.*;
import lombok.*;
import lombok.experimental.*;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class to manage connection to the server
 */
@UtilityClass
public class Connector {
    Socket socket;
    private OutputStream out; //buffer?
    private InputStream in;   //buffer?
    private final Thread helper = getNetThread();
    private final Queue<Events.Event> qin = new LinkedList<>();
    //meh, qua non abbiamo problemi, se il netTh si blocca non dovrebbero esserci problemi (TODO: controlla)
    //private final Queue<Events.Event> qout = new LinkedList<>();
    private final AtomicBoolean active = new AtomicBoolean(true);

    //TODO: is it shared?
    private Thread getNetThread() {
        Thread th =  new Thread(() -> {
            ObjectOutputStream oii;
            ObjectInputStream ois;
            try {
                oii = new ObjectOutputStream(out);
                ois = new ObjectInputStream(in);
            } catch (IOException e) {
                throw new RuntimeException(e); //TODO: manage me better m***y
            }

            //TODO: send version to validate
            //also the first one could be a code (to auth the lobby/game)
            while(active.get()) {
                if (!qin.isEmpty()) {
                    var event = qin.peek();
                    qin.remove();
                    try {
                        //TOOD: it should be a int (event handler code)-obj (the event)
                        oii.writeObject(event);
                    } catch (IOException e) {
                        System.err.println("Errore nell'invio dei dati");
                        throw new RuntimeException(e); //TODO: manage me better m***y (event?)
                    }
                }

                try {
                    if (in.available() > 0) {
                        ois.readObject();
                        //
                    }
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "network-thread");
        th.setDaemon(true);
        th.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            active.set(false);
        }));
        return th;
    }

    public void init(String host, int port) throws IOException {
        socket = new Socket(host, port);
        reconnect();
    }

    public void reconnect() throws IOException {
        if (!socket.isClosed()) socket.connect(socket.getRemoteSocketAddress());
        out = socket.getOutputStream();
        in = socket.getInputStream();
    }

    static void send(Object data) {
        try {

        } catch (Exception e) {
            throw new RuntimeException(e); //TODO: real handling
        }
    }

    static {
        Events.MOVE_REQUEST.addHandler(e -> {
            //SwingUtilities.invokeAndWait(()-> {/*codice*/})
            //SwingUtilities.invokeAndWait(funzioneStatica)
        });
        /*
        Object e = new Object();
        Events.MOVE_REQUEST.fire(e);
         */
    }
}
