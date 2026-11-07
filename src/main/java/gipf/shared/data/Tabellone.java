package gipf.shared.data;

import lombok.*;

import java.io.*;
import java.util.*;

public class Tabellone implements Serializable {
    @Serial private static final long serialVersionUID = -2515531541103L;
    public final List<Posizione> bianco;
    public final List<Posizione> nero;
    public int riserveBianco;
    public int riserveNero;

    public Tabellone() {
        bianco = new ArrayList<>();
        nero = new ArrayList<>();
        riserveBianco = 0;
        riserveNero = 0;
    }

    public Tabellone(Tabellone o) {
        bianco = new ArrayList<>(o.bianco);
        nero = new ArrayList<>(o.nero);
        riserveBianco = o.riserveBianco;
        riserveNero = o.riserveNero;
    }

    public StatoCella getCell(Posizione pos) {
        if (pos == null || !pos.isValid()) return StatoCella.VUOTO;

        if (bianco.contains(pos)) return StatoCella.BIANCO;
        if (nero.contains(pos)) return StatoCella.NERO;

        return StatoCella.VUOTO;
    }

    public void setCell(Posizione pos, StatoCella stato) {
        if (pos == null || !pos.isValid()) return;
        switch (stato) {
            case VUOTO:
                if (bianco.contains(pos)) {
                    bianco.remove(pos);
                } else nero.remove(pos);
                break;
            case NERO:
                if (nero.contains(pos)) return;
                bianco.remove(pos);
                nero.add(pos);
                break;
            case BIANCO:
                if (bianco.contains(pos)) return;
                nero.remove(pos);
                bianco.add(pos);
                break;
        }
    }

    public enum StatoCella {
        BIANCO, NERO, VUOTO
    }
}
