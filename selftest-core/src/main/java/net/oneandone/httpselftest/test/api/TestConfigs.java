package net.oneandone.httpselftest.test.api;

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Models available input parameters as well as pre-configured data sets for a set of test cases.
 */
public class TestConfigs {

    private final List<String> parameterNames;

    private final Map<String, Values> configsMap;

    public TestConfigs(String... parameterNamesArray) {
        assertThat(parameterNamesArray != null, "Parameter names must not be null.");
        parameterNames = Collections.unmodifiableList(Arrays.asList(parameterNamesArray));
        final Set<String> parameterSet = new HashSet<>(parameterNames);

        assertThat(!parameterSet.contains(null), "Parameter names must not be null.");
        assertThat(parameterNames.size() == parameterSet.size(), "Parameter names must be unique.");

        configsMap = new HashMap<>();
    }

    public Values create(String activeConfigId, Map<String, String> parameterMap) {
        assertThat(activeConfigId != null, "Config id must not be null.");
        assertThat(getIds().contains(activeConfigId), "Unknown config id: " + activeConfigId);

        return new Values(Optional.of(activeConfigId), parameterMap);
    }

    public Values create(Map<String, String> parameterMap) {
        return new Values(parameterMap);
    }

    public Values createEmpty() {
        return new Values(parameterNames.stream().collect(toMap(name -> name, name -> "")));
    }

    public void put(String id, String... parameterValues) {
        assertThat(id != null, "Config id must not be null.");
        assertThat(!configsMap.containsKey(id), "Config for id '" + id + "' already exists.");
        assertThat(parameterValues != null, "Parameter values must not be null.");
        @SuppressWarnings("null")
        boolean correctSize = parameterValues.length == parameterNames.size();
        assertThat(correctSize, "Invalid number of parameter values. Expected " + parameterNames.size() + ", got "
                + parameterValues.length + ".");

        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < parameterValues.length; i++) {
            map.put(parameterNames.get(i), parameterValues[i]);
        }

        configsMap.put(id, new Values(Optional.of(id), map));
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }

    public Set<String> getIds() {
        Set<String> backedKeys = configsMap.keySet();
        return unmodifiableSet(backedKeys);
    }

    public Values getValues(String configId) {
        assertThat(configId != null, "Config id must not be null.");
        final Values config = configsMap.get(configId);
        assertThat(config != null, "Config with id '" + configId + "' not found.");
        return config;
    }

    public boolean isEmpty() {
        return configsMap.isEmpty();
    }

    public class Values implements TestValues {
        private final Map<String, String> parameterMap;

        private final Optional<String> activeConfigId;

        private Values(Map<String, String> testParams) {
            this(Optional.empty(), testParams);
        }

        private Values(Optional<String> activeConfigId, Map<String, String> testParams) {
            assertThat(activeConfigId != null, "Config id must not be null.");
            assertThat(testParams != null, "Parameter map must not be null.");

            this.activeConfigId = activeConfigId;

            Map<String, String> processedParams = new HashMap<>();
            parameterNames.forEach(name -> {
                String value = testParams.getOrDefault(name, "");
                assertThat(value != null, "Parameter '" + name + "' must not be null");
                processedParams.put(name, value);
            });
            parameterMap = new HashMap<>(processedParams);
        }

        @Override
        public String get(String name) {
            assertThat(parameterNames.contains(name), "Unknown parameter name '" + name + "'.");
            return parameterMap.get(name);
        }

        public Optional<String> activeConfigId() {
            return activeConfigId;
        }

    }

    private static void assertThat(boolean assumption, String message) {
        if (!assumption) {
            throw new IllegalArgumentException(message);
        }
    }

}
