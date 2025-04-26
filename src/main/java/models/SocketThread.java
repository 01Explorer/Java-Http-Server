package models;

import enums.ContentType;
import enums.ResponseStatus;
import utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.Scanner;
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
                case "", "echo", "user-agent", "files" -> buildStatusLine(ResponseStatus.OK);
                default -> buildStatusLine(ResponseStatus.NOT_FOUND);
            };

            String echo = "";
            if (target.equals("echo") || target.equals("files")) {
                echo = getEcho(request.getRequestLine());
            }
            if (target.equals("user-agent")) {
                echo = request.getHeaders().get("User-Agent");
            }
            if (target.equals("files")) {
                try {
                    echo = handleFilesRequest(echo);
                } catch (FileNotFoundException e) {
                    message = buildStatusLine(ResponseStatus.NOT_FOUND);
                }
            }


            Map<String, Object> headers = switch (target) {
                case "echo", "user-agent" -> buildHeader(echo);
                case "files" -> buildFilesHeader(echo);
                default -> new HashMap<>();
            };

            if (target.equals("files") && message.contains("404")) {
                headers = new HashMap<>();
                echo = "";
            }

            outMessage.println(buildResponse(message, headers, echo));
            outMessage.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    private String handleFilesRequest(String echo) throws FileNotFoundException {
        File file = new File("/tmp/data/codecrafters.io/http-server-tester/".concat(echo));
        Scanner scanner = new Scanner(file);
        StringBuilder content = new StringBuilder();
        while (scanner.hasNextLine()) {
            content.append(scanner.nextLine());
        }
        scanner.close();
        return content.toString();
    }

    private Map<String, Object> buildFilesHeader(String content) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(ContentType.KEY, ContentType.OCTET_STREAM.getName());
        headers.put("Content-Length", content.length());
        return headers;
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

    private String buildResponse(String statusLine, Map<String, Object> headers, String body) {
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
            if (inputLine.contains(": ")) {
                headers.add(inputLine);
                continue;
            }
            if (inputLine.contains("HTTP")) {
                builder.setRequestLine(inputLine);
                continue;
            }

            builder.setRequestBody(inputLine);
        }
        builder.setHeaders(String.join(Utils.CRLF, headers));
        return builder.build();
    }
}
