package net.oneandone.httpselftest.http.parser;

import java.util.Optional;

import net.oneandone.httpselftest.http.Headers;
import net.oneandone.httpselftest.http.HttpDetails;

public interface HttpPresenter {

    String id();

    Optional<String> parse(Headers headers, HttpDetails requestOrResponse);

}
