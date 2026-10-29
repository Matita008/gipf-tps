package gipf.tests.net;

import gipf.shared.Events;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import lombok.*;
import lombok.experimental.*;

@UtilityClass @FieldDefaults(level = AccessLevel.PUBLIC)
public class TestData {
    final int port = 12234;

    /**
     * Prints a debug message with the event name and data.
     */
    void printEvent(String name, Object data) {
        System.out.println("[TEST] event " + name + " data: <" + data.getClass() + "> " + data);
    }

    void handlePing(String type, Boolean resp) {
        System.out.println("[" + type + "] " + (resp ? "ping" : "pong"));
    }

    void init() {
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
