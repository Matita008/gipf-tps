package gipf.shared.data;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class EventData {
    public record MoveRequest(Posizione cella, Posizione direzione) implements Serializable {
        @Serial private static final long serialVersionUID = -426967L;
    }

    @Value @RequiredArgsConstructor
    public static class MoveResponse implements Serializable {
        @Serial private static final long serialVersionUID = -65461321L;
        boolean success;
        Tabellone nuovoTabellone;
        @NonNull
        List<Posizione> fixes = List.of();
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
