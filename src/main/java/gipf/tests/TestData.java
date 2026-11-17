package gipf.tests;

import gipf.shared.Events;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import lombok.*;
import lombok.experimental.*;

/**
 * Class to keep test data & code
 *
 * <b>Use only from {@code gipf.tests.*}</b>
 */
@UtilityClass @FieldDefaults(level = AccessLevel.PUBLIC)
public class TestData {
    public final int port = 12234;

    /**
     * Prints a debug message with the event name and data.
     */
    void printEvent(String name, Object data) {
        System.out.println("[TEST] event " + name + " data: <" + (data == null ? "null" : data.getClass()) + "> " + data);
    }

    void handlePing(String type, Boolean resp) {
        System.out.println("[" + type + "] " + (resp ? "ping" : "pong"));
    }

    public void init() {
        // add to all events (with the help of some reflection magic) the printEvent handler
        Arrays.stream(Events.class.getFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .filter(f -> f.canAccess(null))
                .forEach(f -> {
                    try {
                        Events.on((Events.EventHandler<?>) f.get(null), e -> printEvent(f.getName(), e));
                    } catch (Exception e) {
                        throw new AssertionError(e);
                    }
                });
        Events.CLIENT_PING.addHandler(type -> handlePing("client", type));
        Events.SERVER_PING.addHandler(type -> handlePing("server", type));
    }
}
