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

import org.junit.Test;

import net.oneandone.httpselftest.test.api.TestConfigs;
import net.oneandone.httpselftest.test.api.TestConfigs.Values;

@SuppressWarnings("unused")
public class TestConfigsTest {

    @Test
    public void new_testconfigs() {
        new TestConfigs();
        new TestConfigs("a");
        new TestConfigs("a", "b");
    }

    @Test
    public void new_testconfigs_null_array() {
        assertThatThrownBy(() -> new TestConfigs((String[]) null)).hasMessageContaining("must not be null");
    }

    @Test
    public void new_testconfigs_null_null() {
        assertThatThrownBy(() -> new TestConfigs(null, null)).hasMessageContaining("must not be null");
    }

    @Test
    public void new_testconfigs_duplicate() {
        assertThatThrownBy(() -> new TestConfigs("a", "a")).hasMessageContaining("must be unique");
    }

    @Test
    public void put_nullId() {
        TestConfigs c = new TestConfigs("a");
        assertThatThrownBy(() -> c.put(null, "va")).hasMessageContaining("must not be null");
    }

    @Test
    public void put_nullArray() {
        TestConfigs c = new TestConfigs("p1");
        assertThatThrownBy(() -> c.put("c1", (String[]) null)).hasMessageContaining("must not be null");
    }

    @Test
    public void put_nullValue() {
        TestConfigs c = new TestConfigs("p1", "p2");
        assertThatThrownBy(() -> c.put("c1", "v1", null)).hasMessageContaining("must not be null").hasMessageContaining("'p2'");
    }

    @Test
    public void put_duplicateId() {
        TestConfigs c = new TestConfigs("a");
        c.put("a", "va");
        assertThatThrownBy(() -> c.put("a", "va")).hasMessageContaining("already exists");
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
    public void testconfigs_isEmpty() {
        TestConfigs c = new TestConfigs("a");
        assertThat(c.isEmpty()).as("should be empty").isTrue();
        c.put("a", "va");
        assertThat(c.isEmpty()).as("should not be empty").isFalse();
    }

    @Test
    public void testconfigs_getNull() {
        TestConfigs c = new TestConfigs("a");
        assertThatThrownBy(() -> c.getValues(null)).hasMessageContaining("must not be null");
    }

    @Test
    public void testconfigs_get_exists() {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");
        c.getValues("c1");
    }

    @Test
    public void testconfigs_get_notexists() {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");

        assertThatThrownBy(() -> c.getValues("c2")).hasMessageContaining("not found");
    }

    @Test
    public void getIds() {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");
        c.put("c2", "v2");
        c.put("c3", "v3");
        assertThat(c.getIds()).isEqualTo((new HashSet<String>(asList("c1", "c2", "c3"))));
    }

    @Test
    public void getConfigId() throws Exception {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");
        c.put("c2", "v2");
        assertThat(c.getValues("c1").activeConfigId()).containsSame("c1");
        assertThat(c.getValues("c2").activeConfigId()).containsSame("c2");
    }

    @Test
    public void createWithId_nullId() {
        TestConfigs c = new TestConfigs("p1");
        assertThatThrownBy(() -> c.create(null, emptyMap())).hasMessageContaining("must not be null");
    }

    @Test
    public void createWithId_unknownId() throws Exception {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");
        assertThatThrownBy(() -> c.create("c2", emptyMap())).hasMessageContaining("Unknown config id");
    }

    @Test
    public void createWithId() throws Exception {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");
        c.create("c1", emptyMap());
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
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");
        Values values = c.getValues("c1");
        assertThatThrownBy(() -> values.get("p2")).hasMessageContaining("Unknown parameter");

    }

    @Test
    public void values_get_null() {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");
        Values values = c.getValues("c1");
        assertThatThrownBy(() -> values.get(null)).hasMessageContaining("Unknown parameter name");
    }

    @Test
    public void createEmpty() {
        TestConfigs c = new TestConfigs("p1");
        Values v = c.createEmpty();
        assertThat(v.get("p1")).isEqualTo("");
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
    public void create_nullMap() throws Exception {
        TestConfigs c = new TestConfigs("p1");
        assertThatThrownBy(() -> c.create(null)).hasMessageContaining("must not be null");
    }

    @Test
    public void create_null_value() {
        TestConfigs c = new TestConfigs("p1");
        Map<String, String> params = new HashMap<>();
        params.put("p1", null);
        assertThatThrownBy(() -> c.create(params)).hasMessageContaining("must not be null").hasMessageContaining("'p1'");
    }

    @Test
    public void create_unknownParameter_isIgnored() {
        TestConfigs c = new TestConfigs("p1");
        Map<String, String> params = new HashMap<>();
        params.put("p2", "v2");
        c.create(params);
    }

    @Test
    public void create_missingParameter_usesDefaultValue() {
        TestConfigs c = new TestConfigs("p1", "p2");
        Map<String, String> params = new HashMap<>();
        params.put("p2", "v2");
        assertThat(c.create(params).get("p1")).isEqualTo("");
    }

}
