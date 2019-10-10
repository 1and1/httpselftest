package net.oneandone.httpselftest.http.presenter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

import com.github.cliftonlabs.json_simple.JsonException;
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
            Jsoner.deserialize(body); // prettyPrint accepts everything. deserialize to verify valid json.
            return Optional.of(details.headerBlock() + prettyPrint(body));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // based on Jsoner.prettyPrint(Reader, Writer, String, String) v3.1.0
    private static String prettyPrint(String json) {
        StringWriter writer = new StringWriter();
        try {
            Jsoner.prettyPrint(new StringReader(json), writer, "  ", "\n");
        } catch (IOException ignored) {
            // See java.io.StringReader.
            // See java.io.StringWriter.
        } catch (JsonException ignored) {
            // Would have been caused by a an IO exception while lexing, but the StringReader does not throw them. See
            // java.io.StringReader.
        }
        return writer.toString();
    }

}
