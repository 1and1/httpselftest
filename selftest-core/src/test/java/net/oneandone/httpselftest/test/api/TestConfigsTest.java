package net.oneandone.httpselftest.test.api;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import net.oneandone.httpselftest.test.api.TestConfigs;
import net.oneandone.httpselftest.test.api.TestConfigs.Values;

@SuppressWarnings("unused")
public class TestConfigsTest {

    private static final String NOT_NULL = "must not be null";

    private TestConfigs p1Config;

    @Before
    public void before() {
        p1Config = new TestConfigs("p1");
    }

    @Test
    public void new_testconfigs() {
        new TestConfigs();
        new TestConfigs("a");
        new TestConfigs("a", "b");
    }

    @Test
    public void new_testconfigs_nullArray() {
        assertThatThrownBy(() -> new TestConfigs((String[]) null)).hasMessageContaining(NOT_NULL);
    }

    @Test
    public void new_testconfigs_nullParameter() {
        assertThatThrownBy(() -> new TestConfigs(null, null)).hasMessageContaining(NOT_NULL);
    }

    @Test
    public void new_testconfigs_duplicate() {
        assertThatThrownBy(() -> new TestConfigs("a", "a")).hasMessageContaining("must be unique");
    }

    @Test
    public void new_testconfigs_emptyString() {
        assertThatThrownBy(() -> new TestConfigs("")).hasMessageContaining("must not be empty");
    }

    @Test
    public void fixed_nullArray() throws Exception {
        assertThatThrownBy(() -> p1Config.fixed((String[]) null)).hasMessageContaining(NOT_NULL);
    }

    @Test
    public void fixed_nullParameter() throws Exception {
        assertThatThrownBy(() -> p1Config.fixed(null, null)).hasMessageContaining(NOT_NULL);
    }

    @Test
    public void fixed_empty() throws Exception {
        assertThatThrownBy(() -> p1Config.fixed()).hasMessageContaining("must not be empty");
    }

    @Test
    public void fixed_emptyString() throws Exception {
        assertThatThrownBy(() -> p1Config.fixed("")).hasMessageContaining("must not be empty");
    }

    @Test
    public void fixed_duplicate() throws Exception {
        assertThatThrownBy(() -> p1Config.fixed("p1", "p1")).hasMessageContaining("must be unique");
    }

    @Test
    public void fixed_unknownParameter() throws Exception {
        assertThatThrownBy(() -> p1Config.fixed("p2")).hasMessageContaining("parameter unknown");
    }

    @Test
    public void fixed_twice() throws Exception {
        p1Config.fixed("p1");
        assertThatThrownBy(() -> p1Config.fixed("p1")).hasMessageContaining("already set");
    }

    @Test
    public void fixed_afterPut() throws Exception {
        p1Config.put("c1", "v1");
        assertThatThrownBy(() -> p1Config.fixed("p1")).hasMessageContaining("before adding values");
    }

    @Test
    public void isFixed_unfixed() throws Exception {
        TestConfigs c = new TestConfigs("p1");
        Values v = c.createEmpty();

        assertThatThrownBy(() -> v.isFixed(null));
        assertThatThrownBy(() -> v.isFixed("p2"));
        assertThat(v.isFixed("p1")).isFalse();
    }

    @Test
    public void isFixed_fixed() throws Exception {
        TestConfigs c = new TestConfigs("p1");
        c.fixed("p1");
        Values vfixed = c.createEmpty();
        assertThat(vfixed.isFixed("p1")).isTrue();
    }

    @Test
    public void put_nullId() {
        assertThatThrownBy(() -> p1Config.put(null, "v1")).hasMessageContaining(NOT_NULL);
    }

    @Test
    public void put_nullArray() {
        assertThatThrownBy(() -> p1Config.put("c1", (String[]) null)).hasMessageContaining(NOT_NULL);
    }

    @Test
    public void put_nullValue() {
        TestConfigs c = new TestConfigs("p1", "p2");
        assertThatThrownBy(() -> c.put("c1", "v1", null)).hasMessageContaining(NOT_NULL).hasMessageContaining("'p2'");
    }

    @Test
    public void put_duplicateId() {
        p1Config.put("p1", "v1");
        assertThatThrownBy(() -> p1Config.put("p1", "v1")).hasMessageContaining("already exists");
    }

    @Test
    public void put_notEnoughValues() {
        TestConfigs c = new TestConfigs("p1", "p2");
        assertThatThrownBy(() -> c.put("c1", "v1")).hasMessageContaining("number of parameter values");
    }

    @Test
    public void put_tooManyValues() {
        TestConfigs c = new TestConfigs("p1", "p2");
        assertThatThrownBy(() -> c.put("c1", "v1", "v2", "v3")).hasMessageContaining("number of parameter values");
    }

    @Test
    public void isEmpty() {
        assertThat(p1Config.isEmpty()).as("should be empty").isTrue();
        p1Config.put("p1", "v1");
        assertThat(p1Config.isEmpty()).as("should not be empty").isFalse();
    }

    @Test
    public void getValues_null() {
        assertThatThrownBy(() -> p1Config.getValues(null)).hasMessageContaining(NOT_NULL);
    }

    @Test
    public void getValues_exists() {
        p1Config.put("c1", "v1");
        p1Config.getValues("c1");
    }

    @Test
    public void getValues_notExists() {
        p1Config.put("c1", "v1");
        assertThatThrownBy(() -> p1Config.getValues("c2")).hasMessageContaining("not found");
    }

    @Test
    public void getIds() {
        p1Config.put("c1", "v1");
        p1Config.put("c2", "v2");
        p1Config.put("c3", "v3");
        assertThat(p1Config.getIds()).isEqualTo((new HashSet<String>(asList("c1", "c2", "c3"))));
    }

    @Test
    public void activeConfigId() throws Exception {
        p1Config.put("c1", "v1");
        p1Config.put("c2", "v2");
        assertThat(p1Config.getValues("c1").activeConfigId()).containsSame("c1");
        assertThat(p1Config.getValues("c2").activeConfigId()).containsSame("c2");
    }

    @Test
    public void create_nullMap() throws Exception {
        assertThatThrownBy(() -> p1Config.create(null)).hasMessageContaining(NOT_NULL);
    }

    @Test
    public void create_null_value() {
        Map<String, String> params = new HashMap<>();
        params.put("p1", null);
        assertThatThrownBy(() -> p1Config.create(params)).hasMessageContaining(NOT_NULL).hasMessageContaining("'p1'");
    }

    @Test
    public void create_unknownParameter_isIgnored() {
        Map<String, String> params = new HashMap<>();
        params.put("p2", "v2");
        p1Config.create(params);
    }

    @Test
    public void create_missingParameter_usesDefaultValue() {
        TestConfigs c = new TestConfigs("p1", "p2");
        Map<String, String> params = new HashMap<>();
        params.put("p2", "v2");
        assertThat(c.create(params).get("p1")).isEqualTo("");
    }

    @Test
    public void create_fixed() throws Exception {
        String FIXED = "f2";
        TestConfigs c = new TestConfigs("p1", FIXED);
        c.fixed(FIXED);
        c.put("c1", "v1", "fixed_value");

        HashMap<String, String> params = new HashMap<>();
        params.put("p1", "v1");
        params.put(FIXED, "v2");

        Values v;
        v = c.create(params);
        assertThat(v.get("p1")).isEqualTo("v1");
        assertThat(v.get(FIXED)).isEqualTo("");

        v = c.create("c1", params);
        assertThat(v.get("p1")).isEqualTo("v1");
        assertThat(v.get(FIXED)).isEqualTo("fixed_value");
    }

    @Test
    public void create_ok() {
        TestConfigs c = new TestConfigs("p1", "p2");
        Map<String, String> params = new HashMap<>();
        params.put("p1", "v1");
        params.put("p2", "v2");
        c.create(params);
    }

    @Test
    public void createWithId_nullId() {
        assertThatThrownBy(() -> p1Config.create(null, emptyMap())).hasMessageContaining(NOT_NULL);
    }

    @Test
    public void createWithId_nullParams() {
        p1Config.put("c1", "v1");
        assertThatThrownBy(() -> p1Config.create("c1", null)).hasMessageContaining(NOT_NULL);
    }

    @Test
    public void createWithId_unknownId() throws Exception {
        p1Config.put("c1", "v1");
        assertThatThrownBy(() -> p1Config.create("c2", emptyMap())).hasMessageContaining("Unknown config id");
    }

    @Test
    public void createWithId_ok() throws Exception {
        p1Config.put("c1", "v1");
        p1Config.create("c1", emptyMap());
    }

    @Test
    public void createEmpty() {
        Values v = p1Config.createEmpty();
        assertThat(v.get("p1")).isEqualTo("");
    }

    @Test
    public void getParameterNames() {
        TestConfigs c = new TestConfigs("p1", "p2");
        assertThat(c.getParameterNames()).isEqualTo(asList("p1", "p2"));
    }

    @Test
    public void values_get_ok() {
        TestConfigs c = new TestConfigs("p1", "p2");
        c.put("c1", "v1", "v2");
        c.put("c2", "v3", "v4");
        Values values = c.getValues("c1");

        assertThat(c.getValues("c1").get("p1")).isEqualTo("v1");
        assertThat(c.getValues("c1").get("p2")).isEqualTo("v2");
        assertThat(c.getValues("c2").get("p1")).isEqualTo("v3");
        assertThat(c.getValues("c2").get("p2")).isEqualTo("v4");
    }

    @Test
    public void values_get_unknownParameter() {
        p1Config.put("c1", "v1");
        Values values = p1Config.getValues("c1");
        assertThatThrownBy(() -> values.get("p2")).hasMessageContaining("Unknown parameter");

    }

    @Test
    public void values_get_null() {
        p1Config.put("c1", "v1");
        Values values = p1Config.getValues("c1");
        assertThatThrownBy(() -> values.get(null)).hasMessageContaining("Unknown parameter name");
    }

}
