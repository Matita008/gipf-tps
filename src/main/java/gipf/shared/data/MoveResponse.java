package gipf.shared.data;

import lombok.*;

import java.io.*;
import java.util.List;

@Value @RequiredArgsConstructor
public class MoveResponse implements Serializable {
    @Serial private static final long serialVersionUID = -65461321L;
    boolean success;
    @NonNull List<Posizione> fixes = List.of();
}
