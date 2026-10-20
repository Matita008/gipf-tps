package gipf.shared.data;

import lombok.*;

import java.io.*;

@Value
public class Posizione implements Serializable {
    @Serial private static final long serialVersionUID = 54131314269L;
    char x;
    short y;
}
