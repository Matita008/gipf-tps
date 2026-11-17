package gipf.tests.move;

/// this exist so that both clients can have a debugger
public class Client2 {
    public static void main(String[] args) {
        Client.main(null);
    }
}
class C3 {
    public static void main(String[] args) {
        Client2.main(null);
    }
}
class C4 {
    public static void main(String[] args) {
        Client2.main(null);
    }
}
class C5 {
    public static void main(String[] args) {
        Client2.main(null);
    }
}
