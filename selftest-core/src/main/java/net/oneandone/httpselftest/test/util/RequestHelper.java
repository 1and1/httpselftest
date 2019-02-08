package net.oneandone.httpselftest.test.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.StringJoiner;

import net.oneandone.httpselftest.test.api.TestValues;

public class RequestHelper {

    private RequestHelper() {
    }

    public static void addFormParamsUtf8(StringJoiner sj, TestValues config, String... keys) {
        for (String key : keys) {
            addFormParamUtf8(sj, key, config.get(key));
        }
    }

    public static void addFormParamUtf8(StringJoiner sj, String key, String value) {
        sj.add(urlEncodeUtf8(key) + "=" + urlEncodeUtf8(value));
    }

    public static String urlEncodeUtf8(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // NOSONAR should never happen
        }
    }

}
