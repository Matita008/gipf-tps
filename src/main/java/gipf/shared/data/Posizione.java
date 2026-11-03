package gipf.shared.data;

import java.io.Serial;
import java.io.Serializable;
import lombok.*;

@Value
public class Posizione implements Serializable {
    @Serial private static final long serialVersionUID = 54131314269L;
    short x;
    short y;

    public boolean isValid() {
        if (y < 0 || y > 8) return false;
        if (x < 0 || x > 16) return false;
        if ((x + y) %2 != 0) return false;

        // check if the point is in one of the triangles in the border
        if (x < 3) {
            int ty = y < 5 ? y : 8 - y;
            return (x + ty) > 3;
        } else if (x >= 14) {
            int ty = y < 5 ? y : 8 - y;
            return (16 - x + ty) > 3;
        }

        return true;
    }

    public boolean isBorder() {
        if (!isValid()) return false;

        //is in the middle part?
        if (x > 2 && x < 14) return true;

        //one of the triangles in the borders
        if (x < 3) {
            int ty = y < 5 ? y : 8 - y;
            return (x + ty) == 4;
        } else {
            int ty = y < 5 ? y : 8 - y;
            return (16 - x + ty) == 4;
        }
    }

    ///helper (born for debugger, but it skips breakpoints)
    private static void t(int x, int y, boolean valid) {
        t(x, y, valid, false);
    }

    private static void t(int x, int y, boolean valid, boolean border) {
        var p = new Posizione((short) x, (short) y);

        System.out.print("[" + x + ", " + y + "]: ");

        if (p.isValid() == valid) {
            if (p.isBorder() == border) System.out.println("ok (" + valid + ")");
            else System.out.println("valid ok (" + valid + "), border fail (expect " + border + ")");
        } else {
            System.out.println("valid wrong (expect " + valid + ")");
        }
    }

    /// a manual sanity test (can't be bothered with junit rn)
    public static void main(String[] args) {
        t(0,0, false);
        t(0,1, false);
        t(0,2, false);
        t(0,3, false);
        t(0,4, true, true);
        t(0,5, false);
        t(0,6, false);
        t(0,7, false);
        t(0,8, false);

        t(1,0, false);
        t(1,1, false);
        t(1,2, false);
        t(1,3, true, true);
        t(1,4, false);
        t(1,5, true, true);
        t(1,6, false);
        t(1,7, false);
        t(1,8, false);

        t(2,0, false);
        t(2,1, false);
        t(2,2, true, true);
        t(2,3, false);
        t(2,4, true);
        t(2,5, false);
        t(2,6, true, true);
        t(2,7, false);
        t(2,8, false);

        t(11,0, false);

        t(15,4, false);
        t(15,5, true, true);
        t(16,5, false);
        t(16,4, true, true);
    }
}
