package models;

import enums.ContentType;
import enums.ResponseStatus;
import utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SocketThread extends Thread {
    protected Socket clientSocket;

    public SocketThread(Socket socket) {
        this.clientSocket = socket;
    }

    public void run() {
        try {
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
            outMessage.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    private String getEcho(String request) {
        request = request.replaceAll(Utils.CRLF, "");
        return request.split(" ")[1].split("/")[2];
    }

    private Map<String, Object> buildHeader(String echo) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(ContentType.KEY, ContentType.TEXT.getName());
        headers.put("Content-Length", String.valueOf(echo.length()));

        return headers;
    }

    private String buildStatusLine(ResponseStatus responseStatus) {
        final String httpVersion = "HTTP/1.1";
        return httpVersion + " " + responseStatus.code + " " + responseStatus.message;
    }

    private String buildResponse(String statusLine, Map<String, Object> headers, String body){
        String headersAsString = headers.entrySet().stream().map(entry -> {
            return entry.getKey() + ": " + entry.getValue().toString();
        }).collect(Collectors.joining(Utils.CRLF));
        return statusLine.concat(Utils.CRLF).concat(headersAsString).concat("\r\n\r\n").concat(body);
    }

    private String getRequestTarget(String request) {
        if (Objects.isNull(request) || request.isEmpty()) return "";
        request = request.replaceAll(Utils.CRLF, "");
        String target = request.split(" ")[1].replaceFirst("/", "");
        if (target.contains("/")) {
            return request.split(" ")[1].split("/")[1];
        }
        return target;
    }

    private Request handleRequestInput(InputStream inputStream) throws IOException {
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
        builder.setHeaders(String.join(Utils.CRLF, headers));
        return builder.build();
    }
}
