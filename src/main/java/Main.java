import enums.ResponseStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");
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

            String request = handleRequestInput(clientSocket.getInputStream());
            String target = getRequestTarget(request);
            System.out.println(target);
            String message = switch (target) {
                case "" -> buildOutputMessage(ResponseStatus.OK);
                default -> buildOutputMessage(ResponseStatus.NOT_FOUND);
            };

            outMessage.println(message);
            System.out.println("accepted new connection");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static String buildOutputMessage(ResponseStatus responseStatus) {
        final String httpVersion = "HTTP/1.1";
       return httpVersion + " " + responseStatus.code + " " + responseStatus.message;
    }

    private static String getRequestTarget(String request) {
        request = request.replaceAll("\r\n", "");
        return request.split(" ")[1].replace("/", "");
    }

    private static String handleRequestInput(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        return reader.readLine();
    }
}
