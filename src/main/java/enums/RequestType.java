package enums;

public enum RequestType {
    GET,
    POST,
    PUT,
    DELETE;

    public static RequestType fromString(String requestType) {
        return switch (requestType.toUpperCase()) {
            case "GET" -> GET;
            case "POST" -> POST;
            case "PUT" -> PUT;
            case "DELETE" -> DELETE;
            default -> null;
        };
    }
}
