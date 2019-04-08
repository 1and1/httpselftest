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

    private final List<String> allParameterNames;

    private List<String> fixedParameterNames = Collections.emptyList();
    private boolean fixed = false;

    private final Map<String, Values> configsMap;

    // FIXME switch to builder
    public TestConfigs(String... parameterNamesArray) {
        assertThat(parameterNamesArray != null, "Parameter names array must not be null.");
        allParameterNames = Collections.unmodifiableList(Arrays.asList(parameterNamesArray));
        final Set<String> parameterSet = new HashSet<>(allParameterNames);

        assertThat(!parameterSet.contains(null), "Parameter names must not be null.");
        assertThat(!parameterSet.contains(""), "Parameter names must not be empty.");
        assertThat(allParameterNames.size() == parameterSet.size(), "Parameter names must be unique.");

        configsMap = new HashMap<>();
    }

    public void fixed(String... parameterNamesArray) {
        assertThat(fixed == false, "Fixed parameters already set.");
        assertThat(configsMap.isEmpty(), "Fixed parameters must be set before adding values.");
        this.fixed = true;

        assertThat(parameterNamesArray != null, "Fixed parameter names array must not be null.");
        assertThat(parameterNamesArray.length > 0, "Fixed parameter names array must not be empty.");
        fixedParameterNames = Collections.unmodifiableList(Arrays.asList(parameterNamesArray));
        final Set<String> parameterSet = new HashSet<>(fixedParameterNames);

        assertThat(!parameterSet.contains(null), "Fixed parameter names must not be null.");
        assertThat(!parameterSet.contains(""), "Fixed parameter names must not be empty.");
        assertThat(fixedParameterNames.size() == parameterSet.size(), "Fixed parameter names must be unique.");
        assertThat(allParameterNames.containsAll(parameterSet), "Fixed parameter unknown.");
    }

    public Values create(String activeConfigId, Map<String, String> parameterMap) {
        assertThat(activeConfigId != null, "Config id must not be null.");
        assertThat(configsMap.containsKey(activeConfigId), "Unknown config id: " + activeConfigId);
        assertThat(parameterMap != null, "Parameter map must not be null.");

        return new Values(Optional.of(activeConfigId), parameterMap, configsMap.get(activeConfigId).parameterMap);
    }

    public Values create(Map<String, String> parameterMap) {
        assertThat(parameterMap != null, "Parameter map must not be null.");

        return new Values(parameterMap);
    }

    public Values createEmpty() {
        return new Values(allParameterNames.stream().collect(toMap(name -> name, name -> "")));
    }

    public void put(String id, String... parameterValues) {
        assertThat(id != null, "Config id must not be null.");
        assertThat(!configsMap.containsKey(id), "Config for id '" + id + "' already exists.");
        assertThat(parameterValues != null, "Parameter values must not be null.");
        @SuppressWarnings("null")
        boolean correctSize = parameterValues.length == allParameterNames.size();
        assertThat(correctSize, "Invalid number of parameter values. Expected " + allParameterNames.size() + ", got "
                + parameterValues.length + ".");

        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < parameterValues.length; i++) {
            map.put(allParameterNames.get(i), parameterValues[i]);
        }

        configsMap.put(id, new Values(Optional.of(id), map, map));
    }

    public List<String> getParameterNames() {
        return allParameterNames;
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
            this(Optional.empty(), testParams, Collections.emptyMap());
        }

        private Values(Optional<String> activeConfigId, Map<String, String> testParams, Map<String, String> fixedParams) {
            assertThat(activeConfigId != null, "Config id must not be null."); // should be handled above
            assertThat(testParams != null, "Parameter map must not be null."); // should be handled above
            assertThat(fixedParams != null, "Fixed parameter map must not be null."); // should be handled above

            this.activeConfigId = activeConfigId;

            Map<String, String> processedParams = new HashMap<>();

            allParameterNames.forEach(name -> {
                if (fixedParameterNames.contains(name)) {
                    String fixedValue = Optional.ofNullable(fixedParams.get(name)).orElse("");
                    processedParams.put(name, fixedValue);
                } else {
                    String value = testParams.getOrDefault(name, "");
                    assertThat(value != null, "Parameter '" + name + "' must not be null.");
                    processedParams.put(name, value);
                }
            });
            parameterMap = new HashMap<>(processedParams);
        }

        @Override
        public String get(String name) {
            assertThat(allParameterNames.contains(name), "Unknown parameter name '" + name + "'.");
            return parameterMap.get(name);
        }

        public Optional<String> activeConfigId() {
            return activeConfigId;
        }

        public List<String> getFixedParameterNames() {
            return fixedParameterNames;
        }

    }

    private static void assertThat(boolean assumption, String message) {
        if (!assumption) {
            throw new IllegalArgumentException(message);
        }
    }

}
