package gipf.shared.data;

import java.io.Serial;
import java.io.Serializable;

public class EventData {
    public record MoveRequest(Posizione cella, Posizione direzione) implements Serializable {
            @Serial private static final long serialVersionUID = -426967L;
    }

    public enum GameResult {
        UKNOWN,
        WIN,
        LOSE,
        DRAW,
        /// the other player disconnected
        WIN_QUIT,
        /// the current player resigned
        RESIGN,
        /// the other player resigned
        WIN_RESIGN
    }
}
