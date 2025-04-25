import enums.ContentType;
import enums.ResponseStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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

            Socket clientSocket = serverSocket.accept(); // Wait for connection from client
            PrintWriter outMessage = new PrintWriter(clientSocket.getOutputStream(), true);

            String request = handleRequestInput(clientSocket.getInputStream());
            String target = getRequestTarget(request);
            String message = switch (target) {
                case "", "echo" -> buildStatusLine(ResponseStatus.OK);
                default -> buildStatusLine(ResponseStatus.NOT_FOUND);
            };

            String echo = "";
            if (target.equals("echo")) {
                echo = getEcho(request);
            }


            Map<String, Object> headers = switch (target) {
                case "echo" -> buildHeader(echo);
                default -> new HashMap<>();
            };

            outMessage.println(buildResponse(message, headers, echo));
            clientSocket.close();
            serverSocket.close();
            outMessage.close();
            System.out.println("accepted new connection");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static String getEcho(String request) {
        request = request.replaceAll("\r\n", "");
        return request.split(" ")[1].split("/")[2];
    }

    private static Map<String, Object> buildHeader(String echo) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(ContentType.KEY, ContentType.TEXT.getName());
        headers.put("Content-Length", String.valueOf(echo.length()));

        return headers;
    }

    private static String buildStatusLine(ResponseStatus responseStatus) {
        final String httpVersion = "HTTP/1.1";
       return httpVersion + " " + responseStatus.code + " " + responseStatus.message;
    }

    private static String buildResponse(String statusLine, Map<String, Object> headers, String body){
        String headersAsString = headers.entrySet().stream().map(entry -> {
            return entry.getKey() + ": " + entry.getValue().toString();
        }).collect(Collectors.joining("\r\n"));
        return statusLine.concat("\r\n").concat(headersAsString).concat("\r\n").concat(body);
    }

    private static String getRequestTarget(String request) {
        request = request.replaceAll("\r\n", "");
        return request.split(" ")[1].split("/")[1];
    }

    private static String handleRequestInput(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        return reader.readLine();
    }
}
