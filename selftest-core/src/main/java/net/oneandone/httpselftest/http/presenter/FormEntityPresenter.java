package net.oneandone.httpselftest.http.presenter;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Optional;

import net.oneandone.httpselftest.http.Headers;
import net.oneandone.httpselftest.http.HttpDetails;

public class FormEntityPresenter implements HttpPresenter {

    @Override
    public String id() {
        return "form";
    }

    @Override
    public Optional<String> parse(Headers headers, HttpDetails details) {
        List<String> contentType = headers.get("Content-Type");
        if (contentType == null || contentType.size() != 1 || !contentType.get(0).contains("application/x-www-form-urlencoded")) {
            return Optional.empty();
        }
        String body = details.bodyBlock();
        if (body == null || body.isEmpty()) {
            return Optional.empty();
        }

        String parsedBody = convertUrlEncodedForm(body);
        return Optional.of(details.headerBlock() + parsedBody);
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
            throw new RuntimeException(e); // should never happen
        }
    }

}
