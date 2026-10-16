package gipf.shared;

import gipf.shared.data.*;
import lombok.*;
import lombok.experimental.*;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

@UtilityClass
public final class Events {
    /** Fired by the client when the player made a move */
    public final Event<MoveRequest> MOVE_REQUEST = new Event<>();
    /** Fired by the server when a move is elaborated */
    public final Event<MoveResponse> MOVE_RESPONSE = new Event<>();
    /** fired by the server when the player turn ended */
    public final Event<Object> TURN_END = new Event<>();

    public static Event getEventByCode(int code) {
        return Event.handlers.stream().filter(e -> e.code == code).findFirst().orElse(null);
    }

    public record Event <T> (List<Consumer<T>> hand, int code) implements Serializable {
        @Serial private static final long serialVersionUID = 789321L;
        private static int prog = 0;
        private static final List<Event> handlers = new LinkedList<>();

        private Event() {
            this(new LinkedList<>(), prog++);
            handlers.add(this);
        }

        public void addHandler(Consumer<T> c) {
            hand.add(c);
        }

        public void fire(T event) {
            hand.forEach(c -> c.accept(event));
        }
    }
}
