package net.oneandone.httpselftest.http;

public class DataBasedHttpDetails implements HttpDetails {

    private final String firstLine;
    private final Headers headers;
    private final String body;

    public DataBasedHttpDetails(String firstLine, Headers headers, String body) {
        this.firstLine = firstLine;
        this.headers = headers;
        this.body = body;
    }

    @Override
    public String headerBlock() {
        StringBuilder sb = new StringBuilder(firstLine).append('\n');
        headers.stream().forEach(pair -> sb.append(pair.left + ": " + pair.right + "\n"));
        if (body != null) {
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public String bodyBlock() {
        return body;
    }

}
