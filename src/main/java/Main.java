import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");
        final String httpVersion = "HTTP/1.1";

        // Uncomment this block to pass the first stage

        try {
            ServerSocket serverSocket = new ServerSocket(4221);

            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            final int statusCode = 200;
            final String statusPhrase = "OK";

            Socket clientSocket = serverSocket.accept(); // Wait for connection from client
            PrintWriter outMessage = new PrintWriter(clientSocket.getOutputStream(), true);
            String statusLine = httpVersion + " " + statusCode + " " + statusPhrase;
            outMessage.println(statusLine.concat("\r\n").concat("\r\n"));
            System.out.println("accepted new connection");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
