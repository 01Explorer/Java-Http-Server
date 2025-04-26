package models;

import utils.Utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Request {
    private final String requestLine;
    private final Map<String, String> headers;
    private final String requestBody;

    private Request(Builder builder) {
        this.requestLine = builder.requestLine;
        this.headers = processStringHeaders(builder.headers);
        this.requestBody = builder.requestBody;
    }

    private Map<String, String> processStringHeaders(String headersString) {
        if (Objects.isNull(headersString) || headersString.isEmpty()) return new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        Arrays.stream(headersString.split(Utils.CRLF)).forEach(s -> {
            String[] keyValuePair = s.trim().split(": ");
            headers.put(keyValuePair[0], keyValuePair[1]);
        });

        return headers;
    }

    public String getRequestLine() {
        return requestLine;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public static Builder getBuilder(){
        return new Builder();
    }

    public static class Builder {
        private String requestLine;
        private String headers;
        private String requestBody;

        public Builder setRequestLine(String requestLine) {
            this.requestLine = requestLine;
            return this;
        }

        public Builder setHeaders(String headers) {
            this.headers = headers;
            return this;
        }

        public Builder setRequestBody(String requestBody) {
            this.requestBody = requestBody;
            return this;
        }

        public Request build() {
            return new Request(this);
        }

    }
}
