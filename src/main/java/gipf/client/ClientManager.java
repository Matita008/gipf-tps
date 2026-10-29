package gipf.client;

import gipf.shared.Events;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.experimental.*;

/**
 * Class to manage the
 */
@UtilityClass
public class ClientManager {
    // if the room to be joined is random (if so, a failed attempt is retried)
    private final AtomicBoolean joinRandomRoom = new AtomicBoolean(false);
    private String joinRoomName = null;
    // if waiting for an ack from the server
    private final AtomicBoolean joinRoomPending = new AtomicBoolean(false);
    public final Random RNG = new Random();

    public void joinRandomRoom() {
        joinRandomRoom.set(true);
        Events.CLIENT_LIST_ROOMS.fire(null);
    }

    public void joinRoom(String roomName) {
        if (joinRoomPending.getAndSet(true)) return;
        Events.CLIENT_JOIN_ROOM.fire(roomName);
    }

    public void registerListeners() {
        registerStreamedEvents();

        Events.SERVER_PING.addHandler(e -> {
            if (e) Events.CLIENT_PING.fire(false);
        });

        Events.SERVER_LIST_ROOMS.addHandler(e -> {
            if (joinRandomRoom.get()) {
                // we are already trying to join a room, be nice and don't spam the server
                if (joinRoomPending.getAndSet(true)) return;
                // if the server returned no empty room, create one
                //  (the room name is a hex cause it was the first thing I thought of)
                if (e.isEmpty()) Events.CLIENT_JOIN_ROOM.fire(Long.toHexString(RNG.nextLong()));
                // there is at least an open room, join it.
                else Events.CLIENT_JOIN_ROOM.fire(e.get(RNG.nextInt(e.size())));
            }
        });

        Events.SERVER_JOIN_ROOM_ACK.addHandler(e -> {
            if (!e) {
                if (joinRoomName == null) {
                    // someone else was faster, retry
                    Events.CLIENT_LIST_ROOMS.fire(null);
                }
                return;
            }
            joinRoomPending.set(false);
            joinRoomName = null;
        });
    }

    private void registerStreamedEvents() {
        ClientConnector.manageEvent(Events.CLIENT_PING);
        ClientConnector.manageEvent(Events.CLIENT_CONNECT);
        ClientConnector.manageEvent(Events.CLIENT_LIST_ROOMS);
        ClientConnector.manageEvent(Events.CLIENT_JOIN_ROOM);
    }
}
