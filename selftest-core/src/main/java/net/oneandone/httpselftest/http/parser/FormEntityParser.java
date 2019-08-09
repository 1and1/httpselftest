package net.oneandone.httpselftest.http.parser;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Optional;

import net.oneandone.httpselftest.http.FullTestResponse;

public class FormEntityParser implements HttpContentParser {

    @Override
    public String id() {
        return "form";
    }

    @Override
    public Optional<String> parse(FullTestResponse resp) {
        String contentType = resp.getTestResponse().getHeader("Content-Type");
        if (contentType == null || !contentType.contains("application/x-www-form-urlencoded")) {
            return Optional.empty();
        }
        String body = resp.bodyBlock();
        if (body == null || body.isEmpty()) {
            return Optional.empty();
        }

        try {
            String parsedBody = convertUrlEncodedForm(body);
            return Optional.of(resp.headerBlock() + parsedBody);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private String convertUrlEncodedForm(String body) {
        StringBuilder sb = new StringBuilder();

        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] split = pair.split("=", 2);
            sb.append(urldecode(split[0]));
            if (split.length > 1) {
                sb.append(" = ").append(urldecode(split[1]));
            }
            sb.append("\n");
        }

        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private String urldecode(String string) {
        try {
            return URLDecoder.decode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
