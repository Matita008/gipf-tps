package gipf.tests.net;

import gipf.shared.Events;
import lombok.*;
import lombok.experimental.*;

@UtilityClass @FieldDefaults(level = AccessLevel.PUBLIC)
public class TestData {
    final int port = 12234;

    void handleTestEvent(Object data) {
        System.out.println("[TEST] event data: " + data.toString());
    }

    void init() {
        Events.TEST_CLIENT.addHandler(TestData::handleTestEvent);
        Events.TEST_SERVER.addHandler(TestData::handleTestEvent);
    }
}
