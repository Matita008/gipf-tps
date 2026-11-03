package gipf.shared;

import gipf.shared.data.*;
import gipf.shared.data.EventData.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import lombok.experimental.*;

@UtilityClass
public final class Events {
    /*** test event */
    public final EventHandler<Object> TEST_CLIENT = new EventHandler<>("TEST_CLIENT");
    public final EventHandler<Object> TEST_SERVER = new EventHandler<>("TEST_SERVER");
    /** Fired by the client when the player made a move */
    public final EventHandler<MoveRequest> MOVE_REQUEST = new EventHandler<>("MOVE_REQUEST");
    /** Fired by the server when a move is elaborated */
    public final EventHandler<MoveResponse> MOVE_RESPONSE = new EventHandler<>("MOVE_RESPONSE");
    /** fired by the server when the player turn ended */
    public final EventHandler<Object> TURN_END = new EventHandler<>("TURN_END");
    /** fired by the server when the game ends */
    public final EventHandler<GameResult> GAME_END = new EventHandler<>("GAME_END");
    /** fired by the client to signal a graceful disconnection */
    public final EventHandler<Object> CLIENT_QUIT = new EventHandler<>("CLIENT_QUIT");
    // those 2 send a ping: the bool is if the event is a reply or not (false = reply) to stop any loops
    /// fired from the server/server reply
    public final EventHandler<Boolean> SERVER_PING = new EventHandler<>("SERVER_PING");
    /// fired from the client/client reply
    public final EventHandler<Boolean> CLIENT_PING = new EventHandler<>("CLIENT_PING");
    /// fired from a client to validate the connection parameters
    public final EventHandler<SharedConstants.InitPacket> CLIENT_CONNECT = new EventHandler<>("CLIENT_CONNECT");
    /// fired from the server in reply to the CLIENT_CONNECT event if the init packet is valid
    /// (e.g.: the shared fields match or are compatible)
    public final EventHandler<SharedConstants.InitPacket> SERVER_CONNECTION_ACCEPTED = new EventHandler<>("SERVER_CONNECTION_ACCEPTED");
    /// fired from the server in reply to the CLIENT_CONNECT event if the init packet is not valid
    /// (e.g.: the shared fields don't match and are not compatible)
    /// the reply is the server init packet
    public final EventHandler<SharedConstants.InitPacket> SERVER_CONNECTION_REJECTED = new EventHandler<>("SERVER_CONNECTION_REJECTED");

    public final EventHandler<Object> CLIENT_LIST_ROOMS = new EventHandler<>("CLIENT_LIST_ROOMS");
    public final EventHandler<List<String>> SERVER_LIST_ROOMS = new EventHandler<>("SERVER_LIST_ROOMS");
    public final EventHandler<String> CLIENT_JOIN_ROOM = new EventHandler<>("CLIENT_JOIN_ROOM");
    public final EventHandler<Boolean> SERVER_JOIN_ROOM_ACK = new EventHandler<>("SERVER_JOIN_ROOM_ACK");

    public static <T> void on(EventHandler<T> event, Consumer<T> handler) {
        event.addHandler(handler);
    }

    public static EventHandler<?> getEventByCode(int code) {
        return EventHandler.handlers.stream().filter(e -> e.code == code).findFirst().orElse(null);
    }

    SharedConstants.InitPacket buildInitPacket() {
        return new SharedConstants.InitPacket(SharedConstants.version, EventHandler.handlers.size());
    }

    public static <T> void fire(EventHandler<T> event, T data) {
        event.fire(data);
    }

    public record EventHandler<T> (List<Consumer<T>> hand, int code, String name) {
        private static int prog = 0;
        private static final List<EventHandler<?>> handlers = new LinkedList<>();

        private EventHandler(String name) {
            this(new LinkedList<>(), prog++, name);
            handlers.add(this);
        }

        public void addHandler(Consumer<T> c) {
            hand.add(c);
        }

        // lancia l'evento
        public void fire(T event) {
            hand.forEach(c -> c.accept(event));
        }

        //questa funzione chiama fire, solo che disabilita il type-checking
        /*** Don't run this, as there is no type-checking at build-time */
        @SuppressWarnings("unchecked")
        public void fireUnsafe(Object event) {
            fire((T) event);
        }
    }

    /**
     * Stores an event that is going to be sent via the socket/network.
     * <p>
     * Note that we use a code (tbh we could've used the name, but that came later for debugging purposes)
     * as we need to be able to identify the event on the other side
     * (and serialization is impossible as each event does not have a different class)
     * </p>
     */
    public record Event(int code, Object event) implements Serializable {
        @Serial private static final long serialVersionUID = -543231;

        @Override
        public String toString() {
            return "Event[" +
                    "event=" + event +
                    ", code=" + code +
                    // Add also the event if found
                    (Events.getEventByCode(code) != null ? " (" + Events.getEventByCode(code).name() + ")" : "") +
                    ']';
        }
    }
}
