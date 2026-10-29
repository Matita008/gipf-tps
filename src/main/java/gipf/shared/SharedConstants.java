package gipf.shared;

import java.io.Serial;
import java.io.Serializable;

public class SharedConstants {
    public static final String version = "1.0.0";
    public static final InitPacket initPacket = Events.buildInitPacket();

    public record InitPacket(String version, int eventSize) implements Serializable {
        @Serial
        private static final long serialVersionUID = 789486413L;
    }
}
