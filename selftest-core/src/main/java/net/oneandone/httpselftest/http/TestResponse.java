package net.oneandone.httpselftest.http;

import java.util.List;

public interface TestResponse {

    int getStatus();

    String getHeader(String headerName);

    List<String> getHeaderAllValues(String headerName);

    String getBody();

    String wireRepresentation();

}
