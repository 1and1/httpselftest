package net.oneandone.httpselftest.test.api;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class TestConfigs {

    private final List<String> allParameterNames;
    private final List<String> fixedParameterNames;
    private final Map<String, Map<String, String>> configsMap;

    public TestConfigs(Builder builder) {

        this.allParameterNames = new ArrayList<>(builder.allParameterNames);
        this.fixedParameterNames = new ArrayList<>(builder.fixedParameterNames);
        Map<String, List<String>> builderMap = new HashMap<>(builder.configsMap);

        assertNoNullEntries(this.allParameterNames, "parameter names");
        assertNoEmptyEntries(this.allParameterNames, "parameter names");
        assertNoDuplicates(this.allParameterNames, "parameter names");

        assertNoDuplicates(this.fixedParameterNames, "fixed parameters");
        assertIsSubset(this.fixedParameterNames, this.allParameterNames, "fixed parameters", "parameter names");

        assertNoNullEntries(builderMap.keySet(), "config ids");
        builderMap.entrySet().stream().forEach(configPair -> {
            assertNoNullEntries(configPair.getValue(), "config values");
            assertThat(configPair.getValue().size() == allParameterNames.size(),
                    "wrong number of parameter values for config id: " + configPair.getKey());
        });

        configsMap = new HashMap<>();
        builderMap.entrySet().forEach(configPair -> {
            String id = configPair.getKey();
            List<String> parameterValues = configPair.getValue();

            Map<String, String> values = new HashMap<>();
            for (int i = 0; i < allParameterNames.size(); i++) {
                values.put(allParameterNames.get(i), parameterValues.get(i));
            }
            configsMap.put(id, values);
        });
    }

    public List<String> getParameterNames() {
        return allParameterNames;
    }

    public Set<String> getIds() {
        return configsMap.keySet();
    }

    public boolean isEmpty() {
        return configsMap.isEmpty();
    }

    public boolean hasFixedParams() {
        return !fixedParameterNames.isEmpty();
    }

    public Values createEmpty() {
        return new Values(Optional.empty(), allParameterNames.stream().collect(toMap(name -> name, name -> "")));
    }

    public Values create(Map<String, String> userValues) {
        return new Values(Optional.empty(), userValues);
    }

    public Values create(String configId) {
        assertThat(configId != null, "Config id must not be null.");
        final Map<String, String> configValues = configsMap.get(configId);
        assertThat(configValues != null, "Unknown config id: " + configId);
        return new Values(Optional.of(configId), configValues);
    }

    public Values create(String configId, Map<String, String> userValues) {
        assertThat(configId != null, "Config id must not be null.");
        return new Values(Optional.of(configId), userValues);
    }

    public static class Builder {

        private final List<String> allParameterNames;
        private List<String> fixedParameterNames = Collections.emptyList();
        private Map<String, List<String>> configsMap = new HashMap<>();

        public Builder(String... allParameterNames) {
            assertThat(allParameterNames != null, "Parameter names array must not be null.");
            this.allParameterNames = Arrays.asList(allParameterNames);
        }

        public void fixed(String... fixedParameterNames) {
            assertThat(fixedParameterNames != null, "Fixed parameter names array must not be null.");
            this.fixedParameterNames = Arrays.asList(fixedParameterNames);
        }

        public void put(String id, String... parameterValues) {
            assertThat(id != null, "Config id must not be null.");
            assertThat(!configsMap.containsKey(id), "Config for id '" + id + "' already exists.");
            assertThat(parameterValues != null, "Parameter values must not be null.");
            configsMap.put(id, Arrays.asList(parameterValues));
        }
    }

    public class Values implements TestValues {

        private final Optional<String> activeConfigId;
        private final Map<String, String> parameterMap;

        private Values(Optional<String> activeConfigId, Map<String, String> userValues) {
            assertThat(userValues != null, "Parameter map must not be null.");
            assertNoNullEntries(userValues.values(), "user values");
            assertThat(!activeConfigId.isPresent() || configsMap.containsKey(activeConfigId.get()),
                    "Unknown config id: " + activeConfigId);

            this.activeConfigId = activeConfigId;
            Optional<Map<String, String>> valuesForActiveConfig = this.activeConfigId.map(configsMap::get);

            Map<String, String> processedParams = new HashMap<>();
            fixedParameterNames.forEach(parameter -> {
                String fixedValue = valuesForActiveConfig.map(values -> values.get(parameter)).orElse("");
                processedParams.put(parameter, fixedValue);
            });
            allParameterNames.forEach(parameter -> {
                processedParams.computeIfAbsent(parameter, name -> {
                    return userValues.getOrDefault(name, "");
                });
            });
            parameterMap = processedParams;
        }

        @Override
        public String get(String name) {
            assertThat(allParameterNames.contains(name), "Unknown parameter name '" + name + "'.");
            return parameterMap.get(name);
        }

        public Optional<String> activeConfigId() {
            return activeConfigId;
        }

        public boolean isFixed(String name) {
            assertThat(allParameterNames.contains(name), "Unknown parameter name '" + name + "'.");
            return fixedParameterNames.contains(name);
        }
    }

    private static void assertNoNullEntries(Collection<?> collection, String name) {
        boolean hasNullElement = collection.stream().anyMatch(Objects::isNull);
        assertThat(!hasNullElement, "May not contain null elements: " + name);
    }

    private void assertNoEmptyEntries(Collection<String> collection, String name) {
        boolean hasEmptyString = collection.stream().anyMatch(String::isEmpty);
        assertThat(!hasEmptyString, "May not contain empty strings: " + name);
    }

    private static void assertNoDuplicates(Collection<?> list, String name) {
        assertThat(list.size() == new HashSet<>(list).size(), "May not contain duplicates: " + name);
    }

    private static void assertIsSubset(Collection<?> subset, Collection<?> superset, String subName, String superName) {
        assertThat(superset.containsAll(subset), "Must be a subset: " + subName + " ⊆ " + superName);
    }

    private static void assertThat(boolean assumption, String message) {
        if (!assumption) {
            throw new IllegalArgumentException(message);
        }
    }

}
