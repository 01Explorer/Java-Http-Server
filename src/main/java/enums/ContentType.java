package enums;

public enum ContentType {
    TEXT("text/plain");


    public static String KEY = "Content-Type";
    private final String name;
    private ContentType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
