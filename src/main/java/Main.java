import enums.ContentType;
import enums.ResponseStatus;
import models.Request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

            Request request = handleRequestInput(clientSocket.getInputStream());
            String target = getRequestTarget(request.getRequestLine());
            String message = switch (target) {
                case "", "echo", "user-agent" -> buildStatusLine(ResponseStatus.OK);
                default -> buildStatusLine(ResponseStatus.NOT_FOUND);
            };

            String echo = "";
            if (target.equals("echo")) {
                echo = getEcho(request.getRequestLine());
            }
            if (target.equals("user-agent")) {
                echo = request.getHeaders().get("User-Agent");
            }


            Map<String, Object> headers = switch (target) {
                case "echo", "user-agent" -> buildHeader(echo);
                default -> new HashMap<>();
            };

            outMessage.println(buildResponse(message, headers, echo));
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
        return statusLine.concat("\r\n").concat(headersAsString).concat("\r\n\r\n").concat(body);
    }

    private static String getRequestTarget(String request) {
        request = request.replaceAll("\r\n", "");
        String target = request.split(" ")[1].replaceFirst("/", "");
        if (target.contains("/")) {
            return request.split(" ")[1].split("/")[1];
        }
        return target;
    }

    private static Request handleRequestInput(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String inputLine;
        Request.Builder builder = new Request.Builder();
        List<String> headers = new ArrayList<>();
        while (reader.ready()) {
            inputLine = reader.readLine();
           if(inputLine.contains(": ")){
               headers.add(inputLine);
               continue;
           }
           if (inputLine.contains("HTTP")){
               builder.setRequestLine(inputLine);
               continue;
           }

           builder.setRequestBody(inputLine);
        }
        builder.setHeaders(String.join("\r\n", headers));
        return builder.build();
    }
}
