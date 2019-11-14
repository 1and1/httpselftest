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
import net.oneandone.httpselftest.test.api.TestConfigs.Builder;
import net.oneandone.httpselftest.test.api.TestConfigs.Values;

@SuppressWarnings("unused")
public class TestConfigsTest {

    private static final String NOT_NULL = "must not be null";
    private static final String NO_NULL_ELEMENTS = "not contain null elements";
    private static final String NO_DUPLICATES = "not contain duplicates";

    private TestConfigs.Builder p1Builder;

    private TestConfigs p1Config;

    @Before
    public void before() {
        p1Builder = new TestConfigs.Builder("p1");
        p1Config = new TestConfigs(p1Builder);
    }

    @Test
    public void new_testconfigs() {
        new TestConfigs(new TestConfigs.Builder());
        new TestConfigs(new TestConfigs.Builder("a"));
        new TestConfigs(new TestConfigs.Builder("a", "b"));
    }

    @Test
    public void new_testconfigs_nullArray() {
        assertThatThrownBy(() -> new TestConfigs.Builder((String[]) null)).hasMessageContaining(NOT_NULL);
    }

    @Test
    public void new_testconfigs_nullParameter() {
        assertThatThrownBy(() -> new TestConfigs(new TestConfigs.Builder(null, null))).hasMessageContaining(NO_NULL_ELEMENTS);
    }

    @Test
    public void new_testconfigs_duplicate() {
        assertThatThrownBy(() -> new TestConfigs(new TestConfigs.Builder("a", "a"))).hasMessageContaining(NO_DUPLICATES);
    }

    @Test
    public void new_testconfigs_emptyString() {
        Builder cb = new TestConfigs.Builder("");
        assertThatThrownBy(() -> new TestConfigs(cb)).hasMessageContaining("not contain empty strings");
    }

    @Test
    public void fixed_nullArray() throws Exception {
        assertThatThrownBy(() -> p1Builder.fixed((String[]) null)).hasMessageContaining(NOT_NULL);
    }

    @Test
    public void fixed_empty_works() throws Exception {
        p1Builder.fixed();
        new TestConfigs(p1Builder);
    }

    @Test
    public void fixed_duplicate() throws Exception {
        p1Builder.fixed("p1", "p1");
        assertThatThrownBy(() -> new TestConfigs(p1Builder)).hasMessageContaining(NO_DUPLICATES);
    }

    @Test
    public void fixed_emptyString() throws Exception {
        p1Builder.fixed("");
        assertThatThrownBy(() -> new TestConfigs(p1Builder)).hasMessageContaining("subset");
    }

    @Test
    public void fixed_nullParameter() throws Exception {
        p1Builder.fixed("p1", null);
        assertThatThrownBy(() -> new TestConfigs(p1Builder)).hasMessageContaining("subset");
    }

    @Test
    public void fixed_unknownParameter() throws Exception {
        p1Builder.fixed("p2");
        assertThatThrownBy(() -> new TestConfigs(p1Builder)).hasMessageContaining("subset");
    }

    @Test
    public void hasFixedParams() throws Exception {
        p1Builder.fixed("p1");
        assertThat(new TestConfigs(p1Builder).hasFixedParams()).isTrue();
    }

    @Test
    public void isFixed_unfixed() throws Exception {
        Values v = p1Config.createEmpty();

        assertThatThrownBy(() -> v.isFixed(null));
        assertThatThrownBy(() -> v.isFixed("p2"));
        assertThat(v.isFixed("p1")).isFalse();
    }

    @Test
    public void isFixed_fixed() throws Exception {
        p1Builder.fixed("p1");
        TestConfigs c = new TestConfigs(p1Builder);
        Values vfixed = c.createEmpty();
        assertThat(vfixed.isFixed("p1")).isTrue();
    }

    @Test
    public void put_nullId() {
        assertThatThrownBy(() -> p1Builder.put(null, "v1")).hasMessageContaining(NOT_NULL);
    }

    @Test
    public void put_nullArray() {
        assertThatThrownBy(() -> p1Builder.put("c1", (String[]) null)).hasMessageContaining(NOT_NULL);
    }

    @Test
    public void put_nullValue() {
        TestConfigs.Builder cb = new TestConfigs.Builder("p1", "p2");
        cb.put("c1", "v1", null);
        assertThatThrownBy(() -> new TestConfigs(cb)).hasMessageContaining(NO_NULL_ELEMENTS);
    }

    @Test
    public void put_duplicateId() {
        p1Builder.put("p1", "v1");
        assertThatThrownBy(() -> p1Builder.put("p1", "v1")).hasMessageContaining("already exists");
    }

    @Test
    public void put_notEnoughValues() {
        Builder cb = new TestConfigs.Builder("p1", "p2");
        cb.put("c1", "v1");
        assertThatThrownBy(() -> new TestConfigs(cb)).hasMessageContaining("number of parameter values");
    }

    @Test
    public void put_tooManyValues() {
        Builder cb = new TestConfigs.Builder("p1", "p2");
        cb.put("c1", "v1", "v2", "v3");
        assertThatThrownBy(() -> new TestConfigs(cb)).hasMessageContaining("number of parameter values");
    }

    @Test
    public void isEmpty() {
        assertThat(new TestConfigs(p1Builder).isEmpty()).as("should be empty").isTrue();
        p1Builder.put("p1", "v1");
        assertThat(new TestConfigs(p1Builder).isEmpty()).as("should not be empty").isFalse();
    }

    @Test
    public void getValues_null() {
        String nullConfigId = null;
        assertThatThrownBy(() -> p1Config.create(nullConfigId)).hasMessageContaining(NOT_NULL);
    }

    @Test
    public void getValues_exists() {
        p1Builder.put("c1", "v1");
        new TestConfigs(p1Builder).create("c1");
    }

    @Test
    public void getValues_notExists() {
        p1Builder.put("c1", "v1");
        TestConfigs c = new TestConfigs(p1Builder);
        assertThatThrownBy(() -> c.create("c2")).hasMessageContaining("Unknown config id");
    }

    @Test
    public void getIds() {
        p1Builder.put("c1", "v1");
        p1Builder.put("c2", "v2");
        p1Builder.put("c3", "v3");
        TestConfigs c = new TestConfigs(p1Builder);
        assertThat(c.getIds()).isEqualTo((new HashSet<String>(asList("c1", "c2", "c3"))));
    }

    @Test
    public void activeConfigId() throws Exception {
        p1Builder.put("c1", "v1");
        p1Builder.put("c2", "v2");
        TestConfigs c = new TestConfigs(p1Builder);
        assertThat(c.create("c1").activeConfigId()).containsSame("c1");
        assertThat(c.create("c2").activeConfigId()).containsSame("c2");
    }

    @Test
    public void create_nullMap() throws Exception {
        Map<String, String> nullUserValues = null;
        assertThatThrownBy(() -> p1Config.create(nullUserValues)).hasMessageContaining(NOT_NULL);
    }

    @Test
    public void create_null_value() {
        Map<String, String> params = new HashMap<>();
        params.put("p1", null);
        assertThatThrownBy(() -> p1Config.create(params)).hasMessageContaining(NO_NULL_ELEMENTS);
    }

    @Test
    public void create_unknownParameter_isIgnored() {
        Map<String, String> params = new HashMap<>();
        params.put("p2", "v2");
        p1Config.create(params);
    }

    @Test
    public void create_missingParameter_usesDefaultValue() {
        TestConfigs c = new TestConfigs(new TestConfigs.Builder("p1", "p2"));
        Map<String, String> params = new HashMap<>();
        params.put("p2", "v2");
        assertThat(c.create(params).get("p1")).isEqualTo("");
    }

    @Test
    public void create_fixed() throws Exception {
        String FIXED = "p2";
        Builder cb = new TestConfigs.Builder("p1", FIXED);
        cb.fixed(FIXED);
        cb.put("c1", "v1", "fixed_value");
        TestConfigs c = new TestConfigs(cb);

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
        TestConfigs c = new TestConfigs(new TestConfigs.Builder("p1", "p2"));
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
        p1Builder.put("c1", "v1");
        assertThatThrownBy(() -> new TestConfigs(p1Builder).create("c1", null)).hasMessageContaining(NOT_NULL);
    }

    @Test
    public void createWithId_unknownId() throws Exception {
        p1Builder.put("c1", "v1");
        assertThatThrownBy(() -> new TestConfigs(p1Builder).create("c2", emptyMap())).hasMessageContaining("Unknown config id");
    }

    @Test
    public void createWithId_ok() throws Exception {
        p1Builder.put("c1", "v1");
        new TestConfigs(p1Builder).create("c1", emptyMap());
    }

    @Test
    public void createEmpty() {
        Values v = p1Config.createEmpty();
        assertThat(v.get("p1")).isEqualTo("");
    }

    @Test
    public void getParameterNames() {
        TestConfigs c = new TestConfigs(new TestConfigs.Builder("p1", "p2"));
        assertThat(c.getParameterNames()).isEqualTo(asList("p1", "p2"));
    }

    @Test
    public void values_get_ok() {
        Builder cb = new TestConfigs.Builder("p1", "p2");
        cb.put("c1", "v1", "v2");
        cb.put("c2", "v3", "v4");
        TestConfigs c = new TestConfigs(cb);
        Values values = c.create("c1");

        assertThat(c.create("c1").get("p1")).isEqualTo("v1");
        assertThat(c.create("c1").get("p2")).isEqualTo("v2");
        assertThat(c.create("c2").get("p1")).isEqualTo("v3");
        assertThat(c.create("c2").get("p2")).isEqualTo("v4");
    }

    @Test
    public void values_get_unknownParameter() {
        p1Builder.put("c1", "v1");
        TestConfigs c = new TestConfigs(p1Builder);
        Values values = c.create("c1");
        assertThatThrownBy(() -> values.get("p2")).hasMessageContaining("Unknown parameter");
    }

    @Test
    public void values_get_null() {
        p1Builder.put("c1", "v1");
        TestConfigs c = new TestConfigs(p1Builder);
        Values values = c.create("c1");
        assertThatThrownBy(() -> values.get(null)).hasMessageContaining("Unknown parameter name");
    }

}
