package net.oneandone.httpselftest.test.api;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;

import net.oneandone.httpselftest.test.api.TestConfigs;
import net.oneandone.httpselftest.test.api.TestConfigs.Values;

@SuppressWarnings("unused")
public class TestConfigsTest {

    @Test
    public void new_testconfigs() throws Exception {
        new TestConfigs();
        new TestConfigs("a");
        new TestConfigs("a", "b");
    }

    @Test(expected = IllegalArgumentException.class)
    public void new_testconfigs_null_array() throws Exception {
        new TestConfigs((String[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void new_testconfigs_null_null() throws Exception {
        new TestConfigs(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void new_testconfigs_duplicate() throws Exception {
        new TestConfigs("a", "a");
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_null_id() throws Exception {
        TestConfigs c = new TestConfigs("a");
        c.put(null, "va");
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_null_array() throws Exception {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", (String[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_null_values() throws Exception {
        TestConfigs c = new TestConfigs("p1", "p2");
        c.put("c1", "v1", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_duplicate_id() throws Exception {
        TestConfigs c = new TestConfigs("a");
        c.put("a", "va");
        c.put("a", "va");
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_tooShort() throws Exception {
        TestConfigs c = new TestConfigs("p1", "p2");
        c.put("c1", "v1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_tooLong() throws Exception {
        TestConfigs c = new TestConfigs("p1", "p2");
        c.put("c1", "v1", "v2", "v3");
    }

    @Test
    public void testconfigs_isEmpty() throws Exception {
        TestConfigs c = new TestConfigs("a");
        assertTrue(c.isEmpty());
        c.put("a", "va");
        assertFalse(c.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testconfigs_get_null() throws Exception {
        TestConfigs c = new TestConfigs("a");
        c.get(null);
    }

    @Test
    public void testconfigs_get_exists() throws Exception {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");
        c.get("c1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testconfigs_get_notexists() throws Exception {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");
        c.get("c2");
    }

    @Test
    public void getIds() throws Exception {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");
        c.put("c2", "v2");
        c.put("c3", "v3");
        assertThat(c.getIds(), is(new HashSet<String>(Arrays.asList("c1", "c2", "c3"))));
    }

    @Test
    public void getParameterNames() throws Exception {
        TestConfigs c = new TestConfigs("p1", "p2");
        assertThat(c.getParameterNames(), is(Arrays.asList("p1", "p2")));
    }

    @Test
    public void values_get_ok() throws Exception {
        TestConfigs c = new TestConfigs("p1", "p2");
        c.put("c1", "v1", "v2");
        c.put("c2", "v3", "v4");
        Values values = c.get("c1");

        assertThat(c.get("c1").get("p1"), is("v1"));
        assertThat(c.get("c1").get("p2"), is("v2"));
        assertThat(c.get("c2").get("p1"), is("v3"));
        assertThat(c.get("c2").get("p2"), is("v4"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void values_get_unknownParameter() throws Exception {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");
        Values values = c.get("c1");
        values.get("p2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void values_get_null() throws Exception {
        TestConfigs c = new TestConfigs("p1");
        c.put("c1", "v1");
        Values values = c.get("c1");
        values.get(null);
    }

    @Test
    public void createEmpty() throws Exception {
        TestConfigs c = new TestConfigs("p1");
        Values v = c.createEmpty();
        assertThat(v.get("p1"), is(""));
    }

    @Test
    public void create_ok() throws Exception {
        TestConfigs c = new TestConfigs("p1", "p2");
        Map<String, String> map = new HashMap<>();
        map.put("p1", "v1");
        map.put("p2", "v2");
        c.create(map);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_null_id() throws Exception {
        TestConfigs c = new TestConfigs("p1");
        Map<String, String> map = new HashMap<>();
        map.put(null, "v1");
        c.create(map);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_null_value() throws Exception {
        TestConfigs c = new TestConfigs("p1");
        Map<String, String> map = new HashMap<>();
        map.put("p1", null);
        c.create(map);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_unknownParameter() throws Exception {
        TestConfigs c = new TestConfigs("p1");
        Map<String, String> map = new HashMap<>();
        map.put("p2", "v2");
        c.create(map);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_notAllParameters() throws Exception {
        TestConfigs c = new TestConfigs("p1", "p2");
        Map<String, String> map = new HashMap<>();
        map.put("p2", "v2");
        c.create(map);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_tooManyParameters() throws Exception {
        TestConfigs c = new TestConfigs("p1", "p2");
        Map<String, String> map = new HashMap<>();
        map.put("p1", "v1");
        map.put("p2", "v2");
        map.put("p3", "v3");
        c.create(map);
    }

}
