package gipf.server;

import gipf.shared.data.EventData.MoveRequest;

import java.util.*;

import lombok.*;

@RequiredArgsConstructor
public class GameRoom {
    private static final Map<String, GameRoom> rooms = new HashMap<>();
    private static final Object roomsLock = new Object();
    public final String code;
    @Getter(onMethod_ = @Locked.Read)
    private ServerConnector p1;
    @Getter(onMethod_ = @Locked.Read)
    private ServerConnector p2;

    public static GameRoom getRoom(String code) {
        synchronized (roomsLock) {
            rooms.putIfAbsent(code, new GameRoom(code));
            return rooms.get(code);
        }
    }

    @Locked.Read
    public ServerConnector getOtherPlayer(ServerConnector player) {
        if (player == p1) return p2;
        else if (player == p2) return p1;
        else return null;
    }

    /**
     * Adds a player to a room
     * @param room The room code
     * @param player The player to add
     * @return Whether the player was successfully added to the room (eg: the room didn't exist (and was created)
     * or had a spot free)
     */
    public static boolean addPlayerToRoom(String room, ServerConnector player) {
        synchronized (roomsLock) {
            GameRoom broom = getRoom(room);
            return broom.addPlayer(player);
        }
    }

    @Locked.Write
    public boolean addPlayer(ServerConnector player) {
        if (p1 == player || p2 == player) return false;

        if (p1 == null) {
            p1 = player;
        } else if (p2 == null) {
            p2 = player;
        } else return false;

        player.room = this;
        return true;
    }

    @Locked.Write
    public ServerConnector removeClient(ServerConnector remove) {
        ServerConnector ret;
        if (remove == p1) {
            p1 = null;
            ret = p2;
        } else if (remove == p2) {
            p2 = null;
            ret = p1;
        } else {
            return null;
        }

        if (ret == null) {
            rooms.remove(code);
        }
        remove.room = null;

        return ret;
    }

    public static List<String> getFreeRoomList() {
        ArrayList<String> ret = new ArrayList<>();

        rooms.forEach((code, room) -> {
            if (room.p1 == null || room.p2 == null) {
                ret.add(code);
            }
        });

        return ret;
    }

    //------- Move logic -------

    @Locked.Read
    public void move(MoveRequest move) {
    }
}
