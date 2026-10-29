package gipf.server;

import gipf.shared.Events;
import gipf.shared.SharedConstants;
import gipf.shared.data.EventData.GameResult;

public class ServerListeners {
    public static void registerListeners() {
        Events.CLIENT_PING.addHandler(e -> {
            if (e) Events.SERVER_PING.fire(false);
        });

        Events.on(Events.CLIENT_QUIT, e -> {
            var sth = ServerConnector.getCurrentConnector();
            if (sth == null) return;

            if (sth.room != null) {
                var other = sth.room.removeClient(sth);
                sth.room = null;

                if (other != null) {
                    other.run(() -> Events.GAME_END.fire(GameResult.WIN_QUIT));

                }
            }
        });

        Events.on(Events.CLIENT_CONNECT, initPacket -> {
            if (initPacket == null) return;
            if (initPacket.equals(SharedConstants.initPacket)) {
                Events.fire(Events.SERVER_CONNECTION_ACCEPTED, SharedConstants.initPacket);

                System.out.println("Connection accepted!");
            } else Events.fire(Events.SERVER_CONNECTION_REJECTED, SharedConstants.initPacket);
        });

        Events.on(Events.CLIENT_LIST_ROOMS, e -> Events.fire(Events.SERVER_LIST_ROOMS, GameRoom.getFreeRoomList()));

        Events.on(Events.CLIENT_JOIN_ROOM, code -> {
            if (ServerConnector.getCurrentConnector().room != null) {
                // Client is already in a room
                Events.fire(Events.SERVER_JOIN_ROOM_ACK, false);
                return;
            }

            boolean result = GameRoom.addPlayerToRoom(code, ServerConnector.getCurrentConnector());
            Events.fire(Events.SERVER_JOIN_ROOM_ACK, result);
        });
    }

    public static void registerStreamedEvents() {
        ServerConnector.manageEvent(Events.SERVER_PING, false);
        ServerConnector.manageEvent(Events.SERVER_CONNECTION_ACCEPTED, false);
        ServerConnector.manageEvent(Events.SERVER_CONNECTION_REJECTED, false);
        ServerConnector.manageEvent(Events.SERVER_LIST_ROOMS, false);
        ServerConnector.manageEvent(Events.SERVER_JOIN_ROOM_ACK, false);
    }
}
