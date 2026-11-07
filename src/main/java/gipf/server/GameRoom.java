package gipf.server;

import gipf.shared.Events;
import gipf.shared.data.*;
import gipf.shared.data.EventData.MoveRequest;

import java.util.*;

import lombok.*;

@RequiredArgsConstructor
public class GameRoom {
    private static final Map<String, GameRoom> rooms = new HashMap<>();
    private static final Object roomsLock = new Object();
    public final String code;
    @Getter(onMethod_ = @Locked.Read)
    private ServerConnector bianco;
    @Getter(onMethod_ = @Locked.Read)
    private ServerConnector nero;
    private boolean turnoBianco = true;
    @Getter
    private Tabellone board = new Tabellone();

    public static GameRoom getRoom(String code) {
        synchronized (roomsLock) {
            rooms.putIfAbsent(code, new GameRoom(code));
            return rooms.get(code);
        }
    }

    @Locked.Read
    public ServerConnector getOtherPlayer(ServerConnector player) {
        if (player == bianco) return nero;
        else if (player == nero) return bianco;
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
        if (bianco == player || nero == player) return false;

        if (bianco == null) {
            bianco = player;
        } else if (nero == null) {
            nero = player;
        } else return false;

        player.room = this;
        return true;
    }

    @Locked.Write
    public ServerConnector removeClient(ServerConnector remove) {
        ServerConnector ret;
        if (remove == bianco) {
            bianco = null;
            ret = nero;
        } else if (remove == nero) {
            nero = null;
            ret = bianco;
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
            if (room.bianco == null || room.nero == null) {
                ret.add(code);
            }
        });

        return ret;
    }

    //------- Move logic -------

    private void fail() {
        Events.fire(Events.MOVE_RESPONSE, new EventData.MoveResponse(false, null));
    }

    @Locked.Read
    public void move(MoveRequest move) {
        // invalid data
        if (move == null || move.cella() == null || move.direzione() == null) {
            fail();
            return;
        }

        if (ServerConnector.getCurrentConnector().equals(bianco) != turnoBianco) {
            fail();
            return;
        }

        final Posizione origine = move.cella(); //cella sul bordo in cui è stata inserita la pedina
        final Posizione dir = move.direzione(); //cella verso cui la pedina si muove

        if (!origine.isBorder() || dir.isBorder() || origine.isAdjacent(dir)) {
            fail();
            return;
        }

        int xOffset = dir.getX() - origine.getX();
        int yOffset = dir.getY() - origine.getY();

        Tabellone nuovo = new Tabellone(board);
        Tabellone.StatoCella cellaPrecedente = turnoBianco ? Tabellone.StatoCella.BIANCO : Tabellone.StatoCella.NERO;
        Posizione cella = dir;

        while (!cella.isBorder() && cellaPrecedente != Tabellone.StatoCella.VUOTO) {
            Tabellone.StatoCella cur = board.getCell(cella);
            nuovo.setCell(cella, cellaPrecedente);
            cellaPrecedente = cur;
            cella = cella.add(xOffset, yOffset);
        }

        // tutta la riga è piena, mossa non accettabile
        if (cella.isBorder()) {
            fail();
            return;
        }

        turnoBianco = !turnoBianco;
        board = nuovo;
        Events.fire(Events.MOVE_RESPONSE, new EventData.MoveResponse(true, nuovo));
    }
}
