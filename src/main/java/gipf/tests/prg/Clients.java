package gipf.tests.prg;

import gipf.client.*;
import gipf.server.ServerMain;

public class Clients {
    static class Srv {
        public static void main(String[] args) {
            ServerMain.main(args);
        }
    }
    static class C1 {
        public static void main(String[] args) {
            System.setProperty("debug", "true");
            GipfBoardFrame.main(args);
        }
    }

    static class C2 {
        public static void main(String[] args) {
            System.setProperty("debug", "true");
            GipfBoardFrame.main(args);
        }
    }

    static class C3 {
        public static void main(String[] args) {
            System.setProperty("debug", "true");
            GipfBoardFrame.main(args);
        }
    }
}
