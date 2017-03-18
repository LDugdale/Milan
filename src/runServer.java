import Server.*;

public class runServer {

    public static void main(String[] args) {
        Server server = new Server(54321);
        server.run();
    }
}
