package models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RequestTest {
    private Request request;
    private String requestLine = "GET /user-agent HTTP/1.1";
    private String headersString = "Host: localhost:4221\r\nUser-Agent: foobar/1.2.3\r\nAccept: */*\r\n";
    private String requestBody = "Body";

    @Test
    public void shouldUnwrapSuccessfullyRequestLines(){
        Request.Builder builder = new Request.Builder();
        builder.setRequestLine(requestLine);
        builder.setHeaders(headersString);
        builder.setRequestBody(requestBody);
        request = builder.build();
        assertEquals(requestLine, request.getRequestLine());
        assertEquals(requestBody, request.getRequestBody());
        assertTrue(request.getHeaders().containsKey("Host"));
        assertTrue(request.getHeaders().containsKey("User-Agent"));
        assertTrue(request.getHeaders().containsKey("Accept"));
        assertEquals("localhost:4221", request.getHeaders().get("Host"));
        assertEquals("foobar/1.2.3", request.getHeaders().get("User-Agent"));
        assertEquals("*/*", request.getHeaders().get("Accept"));
    }
}
