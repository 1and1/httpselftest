package net.oneandone.httpselftest.http.presenter;

import java.util.List;
import java.util.Optional;

import com.github.cliftonlabs.json_simple.Jsoner;

import net.oneandone.httpselftest.http.Headers;
import net.oneandone.httpselftest.http.HttpDetails;

public class JsonEntityPresenter implements HttpPresenter {

    @Override
    public String id() {
        return "json";
    }

    @Override
    public Optional<String> parse(Headers headers, HttpDetails details) {
        List<String> contentType = headers.get("Content-Type");
        if (contentType == null || contentType.size() != 1 || !contentType.get(0).contains("application/")
                || !contentType.get(0).contains("json")) {
            return Optional.empty();
        }
        String body = details.bodyBlock();
        if (body == null || body.isEmpty()) {
            return Optional.empty();
        }

        try {
            Jsoner.deserialize(body); // prettyPrint accepts everything
            return Optional.of(details.headerBlock() + Jsoner.prettyPrint(body, 2));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
