package models;

import enums.ContentType;
import enums.RequestType;
import enums.ResponseStatus;
import utils.Utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
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
import java.util.zip.GZIPOutputStream;

public class SocketThread extends Thread {
    protected Socket clientSocket;
    private String[] args;

    public SocketThread(Socket socket, String[] args) {
        this.clientSocket = socket;
        this.args = args;
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
                    if (request.getRequestType().equals(RequestType.GET)) {
                        echo = handleGetFilesRequest(echo);
                    }
                    if (request.getRequestType().equals(RequestType.POST)) {
                        handlePostFilesRequest(request, echo);
                        echo = "";
                        message = buildStatusLine(ResponseStatus.CREATED);
                    }
                } catch (FileNotFoundException e) {
                    message = buildStatusLine(ResponseStatus.NOT_FOUND);
                }
            }


            Map<String, Object> headers = switch (target) {
                case "echo", "user-agent" -> buildHeader(echo);
                case "files" -> buildFilesHeader(echo);
                default -> new HashMap<>();
            };

            byte[] encoded = new byte[]{};
            if(verifyCanEncode(headers, request)){
                headers.put("Content-Encoding", "gzip");
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                GZIPOutputStream gzip = new GZIPOutputStream(byteArrayOutputStream);
                gzip.write(echo.getBytes());
                gzip.close();
                encoded = byteArrayOutputStream.toByteArray();
                byteArrayOutputStream.close();
                headers.put("Content-Length", encoded.length);
                clientSocket.getOutputStream().write(buildResponse(message, headers, "").getBytes());
                clientSocket.getOutputStream().write(encoded);
                clientSocket.getOutputStream().flush();
                clientSocket.close();
                outMessage.close();
                return;
            }

            if (target.equals("files") && (message.contains("404") || request.getRequestType().equals(RequestType.POST))) {
                headers = new HashMap<>();
                echo = "";
            }

            outMessage.println(buildResponse(message, headers, echo));
            if (request.getHeaders().getOrDefault("Connection", "open").equals("close")) {
                clientSocket.close();
                outMessage.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    private boolean verifyCanEncode(Map<String, Object> headers, Request request) {
        if (!request.getHeaders().containsKey("Accept-Encoding")) return false;

        return request.getHeaders().get("Accept-Encoding").contains("gzip");
    }

    private void handlePostFilesRequest(Request request, String fileName) throws IOException {
        File file = new File(args[1], fileName);
        FileWriter writer = new FileWriter(file);
        writer.write(request.getRequestBody());
        writer.flush();
        writer.close();
    }

    private String handleGetFilesRequest(String echo) throws FileNotFoundException {
        File file = new File(args[1], echo);
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
        int contentLength = 0;

        while (!(inputLine = reader.readLine()).isEmpty()) {
            if (inputLine.contains(": ")) {
                headers.add(inputLine);
            }
            if (inputLine.contains("HTTP")) {
                builder.setRequestLine(inputLine);
            }
            if (inputLine.contains("Content-Length")) {
                contentLength = Integer.parseInt(inputLine.substring(inputLine.indexOf(':') + 1).trim());
            }
        }
        builder.setHeaders(String.join(Utils.CRLF, headers));

        if (contentLength > 0) {
            char[] buffer = new char[contentLength];
            reader.read(buffer, 0, contentLength);
            String body = new String(buffer);
            builder.setRequestBody(body);
        }
        return builder.build();
    }
}
