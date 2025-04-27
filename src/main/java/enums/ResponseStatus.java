package enums;

public enum ResponseStatus {

    OK(200, "OK"),
    NOT_FOUND(404, "Not Found"),
    CREATED(201, "Created");

    public final int code;
    public final String message;

    ResponseStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
