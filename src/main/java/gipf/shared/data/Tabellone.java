package gipf.shared.data;

import java.io.*;
import java.util.*;

public class Tabellone implements Serializable {
    @Serial private static final long serialVersionUID = -2515531541103L;
    public final List<Posizione> bianco = new ArrayList<>();
    public final List<Posizione> nero = new ArrayList<>();
    public int riserveBianco;
    public int riserveNero;

    public void disegna() {

    }
}
