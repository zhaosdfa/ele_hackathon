import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

	public static void main(String[] args) {

        try {

            MyHttpServer server = new MyHttpServer();
            server.init(8888, 1000);

            server.createContext("/login", new LoginHandler());

            server.start();

        } catch (IOException e) {
            System.out.println("Exception: " + e.toString());
        }

	}

}

