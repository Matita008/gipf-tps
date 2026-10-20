package gipf.shared;

import gipf.shared.data.*;
import lombok.experimental.*;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

@UtilityClass
public final class Events {
    /*** test event */
    public final EventHandler<Object> TEST_CLIENT = new EventHandler<>();
    public final EventHandler<Object> TEST_SERVER = new EventHandler<>();
    /** Fired by the client when the player made a move */
    public final EventHandler<MoveRequest> MOVE_REQUEST = new EventHandler<>();
    /** Fired by the server when a move is elaborated */
    public final EventHandler<MoveResponse> MOVE_RESPONSE = new EventHandler<>();
    /** fired by the server when the player turn ended */
    public final EventHandler<Object> TURN_END = new EventHandler<>();
    // /** fired before the network thread/connection terminates */
    //public final EventHandler<Object> NETWORK_DISCONNECT = new EventHandler<>();

    public static EventHandler<?> getEventByCode(int code) {
        return EventHandler.handlers.stream().filter(e -> e.code == code).findFirst().orElse(null);
    }

    public record EventHandler<T> (List<Consumer<T>> hand, int code) {
        private static int prog = 0;
        private static final List<EventHandler<?>> handlers = new LinkedList<>();

        private EventHandler() {
            this(new LinkedList<>(), prog++);
            handlers.add(this);
        }

        public void addHandler(Consumer<T> c) {
            hand.add(c);
        }

        public void fire(T event) {
            hand.forEach(c -> c.accept(event));
        }

        /*** Don't run this, as there is no type-checking at build-time */
        @SuppressWarnings("unchecked")
        public void fireUnsafe(Object event) {
            fire((T) event);
        }
    }

    /** Stores an event that is sent via the socket/network */
    public record Event(int code, Object event) implements Serializable {
        @Serial
        private static final long serialVersionUID = -543231;
    }
}
