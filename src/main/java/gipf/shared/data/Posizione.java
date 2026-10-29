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
        } else if (x < 14) {
            return true;
        } else {
            int ty = y < 5 ? y : 8 - y;
            return (16 - x + ty) > 3;
        }
    }

    ///helper (born for debugger, but it skips breakpoints)
    private static boolean c(int x, int y) {
        return (new Posizione((short)x, (short)y)).isValid();
    }

    /// a manual sanity test (can't be bothered with junit rn)
    public static void main(String[] args) {
        System.out.println("false = " + c(0,0));
        System.out.println("false = " + c(0,1));
        System.out.println("false = " + c(0,2));
        System.out.println("false = " + c(0,3));
        System.out.println("true  = " + c(0,4));
        System.out.println("false = " + c(0,5));
        System.out.println("false = " + c(0,6));
        System.out.println("false = " + c(0,7));
        System.out.println("false = " + c(0,8));

        System.out.println("false = " + c(1,0));
        System.out.println("false = " + c(1,1));
        System.out.println("false = " + c(1,2));
        System.out.println("true  = " + c(1,3));
        System.out.println("false = " + c(1,4));
        System.out.println("true  = " + c(1,5));
        System.out.println("false = " + c(1,6));
        System.out.println("false = " + c(1,7));
        System.out.println("false = " + c(1,8));

        System.out.println("false = " + c(2,0));
        System.out.println("false = " + c(2,1));
        System.out.println("true  = " + c(2,2));
        System.out.println("false = " + c(2,3));
        System.out.println("true  = " + c(2,4));
        System.out.println("false = " + c(2,5));
        System.out.println("true  = " + c(2,6));
        System.out.println("false = " + c(2,7));
        System.out.println("false = " + c(2,8));

        System.out.println("false = " + c(11,0));
        System.out.println("true  = " + c(1,1));

        System.out.println("false = " + c(15,4));
        System.out.println("true  = " + c(15,5));
        System.out.println("false = " + c(16,5));
        System.out.println("true  = " + c(16,4));
    }
}
