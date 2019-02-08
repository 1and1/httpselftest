package net.oneandone.httpselftest.test.run;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.oneandone.httpselftest.test.api.Context;

public class SimpleContext implements Context {

    private final Map<String, String> valueStore;
    private List<String> clues;

    public SimpleContext() {
        valueStore = new HashMap<>();
        clues = new LinkedList<>();
    }

    void reset() {
        clues = new LinkedList<>();
    }

    @Override
    public void store(String key, String value) {
        Objects.requireNonNull(key, "key");
        valueStore.put(key, value);
    }

    @Override
    public String retrieve(String key) {
        return valueStore.get(key);
    }

    @Override
    public void addClue(String clue) {
        clues.add(clue);
    }

    public List<String> getClues() {
        return clues;
    }

}
