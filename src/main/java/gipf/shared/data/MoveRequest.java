package gipf.shared.data;

import lombok.*;

import java.io.*;

@Value
public class MoveRequest implements Serializable {
    @Serial private static final long serialVersionUID = -426967L;
    Posizione cella;
    Posizione direzione;
}
