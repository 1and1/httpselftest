package net.oneandone.httpselftest.test.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

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

    @Test(expected = IllegalArgumentException.class)
    public void new_testconfigs_null_array() {
        new TestConfigs((String[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void new_testconfigs_null_null() {
        new TestConfigs(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void new_testconfigs_duplicate() {
        new TestConfigs("a", "a");
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_null_id() {
        TestConfigs c = new TestConfigs("a");
        c.put(null, "va");
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_null_array() {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", (String[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_null_values() {
        TestConfigs c = new TestConfigs("p1", "p2");
        c.put("c1", "v1", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_duplicate_id() {
        TestConfigs c = new TestConfigs("a");
        c.put("a", "va");
        c.put("a", "va");
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_tooShort() {
        TestConfigs c = new TestConfigs("p1", "p2");
        c.put("c1", "v1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_tooLong() {
        TestConfigs c = new TestConfigs("p1", "p2");
        c.put("c1", "v1", "v2", "v3");
    }

    @Test
    public void testconfigs_isEmpty() {
        TestConfigs c = new TestConfigs("a");
        assertTrue(c.isEmpty());
        c.put("a", "va");
        assertFalse(c.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testconfigs_get_null() {
        TestConfigs c = new TestConfigs("a");
        c.getValues(null);
    }

    @Test
    public void testconfigs_get_exists() {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");
        c.getValues("c1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testconfigs_get_notexists() {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");
        c.getValues("c2");
    }

    @Test
    public void getIds() {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");
        c.put("c2", "v2");
        c.put("c3", "v3");
        assertThat(c.getIds(), is(new HashSet<String>(Arrays.asList("c1", "c2", "c3"))));
    }

    @Test
    public void getConfigId() throws Exception {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");
        c.put("c2", "v2");
        assertThat(c.getValues("c1").getConfigId(), is("c1"));
        assertThat(c.getValues("c2").getConfigId(), is("c2"));
    }

    @Test
    public void createConfig_unknownId() throws Exception {
        assertThatThrownBy(() -> {
            TestConfigs c = new TestConfigs("p1");
            c.put("c1", "v1");
            c.create("c2", Collections.emptyMap());
        }).hasMessageContaining("Unknown config id:");
    }

    @Test
    public void getParameterNames() {
        TestConfigs c = new TestConfigs("p1", "p2");
        assertThat(c.getParameterNames(), is(Arrays.asList("p1", "p2")));
    }

    @Test
    public void values_get_ok() {
        TestConfigs c = new TestConfigs("p1", "p2");
        c.put("c1", "v1", "v2");
        c.put("c2", "v3", "v4");
        Values values = c.getValues("c1");

        assertThat(c.getValues("c1").get("p1"), is("v1"));
        assertThat(c.getValues("c1").get("p2"), is("v2"));
        assertThat(c.getValues("c2").get("p1"), is("v3"));
        assertThat(c.getValues("c2").get("p2"), is("v4"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void values_get_unknownParameter() {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");
        Values values = c.getValues("c1");
        values.get("p2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void values_get_null() {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");
        Values values = c.getValues("c1");
        values.get(null);
    }

    @Test
    public void createEmpty() {
        TestConfigs c = new TestConfigs("p1");
        Values v = c.createEmpty();
        assertThat(v.get("p1"), is(""));
    }

    @Test
    public void create_ok() {
        TestConfigs c = new TestConfigs("p1", "p2");
        Map<String, String> map = new HashMap<>();
        map.put("p1", "v1");
        map.put("p2", "v2");
        c.create(map);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_null_id() {
        TestConfigs c = new TestConfigs("p1");
        Map<String, String> map = new HashMap<>();
        map.put(null, "v1");
        c.create(map);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_null_value() {
        TestConfigs c = new TestConfigs("p1");
        Map<String, String> map = new HashMap<>();
        map.put("p1", null);
        c.create(map);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_unknownParameter() {
        TestConfigs c = new TestConfigs("p1");
        Map<String, String> map = new HashMap<>();
        map.put("p2", "v2");
        c.create(map);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_notAllParameters() {
        TestConfigs c = new TestConfigs("p1", "p2");
        Map<String, String> map = new HashMap<>();
        map.put("p2", "v2");
        c.create(map);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_tooManyParameters() {
        TestConfigs c = new TestConfigs("p1", "p2");
        Map<String, String> map = new HashMap<>();
        map.put("p1", "v1");
        map.put("p2", "v2");
        map.put("p3", "v3");
        c.create(map);
    }

}
