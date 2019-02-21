package net.oneandone.httpselftest.test.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Before;
import org.junit.Test;

public class SimpleContextTest {

    private SimpleContext ctx;

    @Before
    public void before() {
        ctx = new SimpleContext();
    }

    @Test
    public void retrieveOnEmptyContext() throws Exception {
        assertThat(ctx.retrieve("someKey")).isNull();
    }

    @Test
    public void storeNullKey() throws Exception {
        assertThatThrownBy(() -> ctx.store(null, "notnull"));
    }

    @Test
    public void storeNullValue() throws Exception {
        ctx.store("key", null);
    }

    @Test
    public void retrieve() throws Exception {
        ctx.store("key1", "value1");
        assertThat(ctx.retrieve("key1")).isEqualTo("value1");

        ctx.store("key2", "value2");
        assertThat(ctx.retrieve("key1")).isEqualTo("value1");
        assertThat(ctx.retrieve("key2")).isEqualTo("value2");
        assertThat(ctx.retrieve("otherKey")).isNull();
    }

    @Test
    public void storeOverwrite() throws Exception {
        ctx.store("key", "value1");
        ctx.store("key", "value2");
        assertThat(ctx.retrieve("key")).isEqualTo("value2");
    }

    @Test
    public void getCluesOnEmptyContext() throws Exception {
        assertThat(ctx.getClues()).isEmpty();
    }

    @Test
    public void addNullClue() throws Exception {
        assertThatThrownBy(() -> ctx.addClue(null));
    }

    @Test
    public void addClue() throws Exception {
        ctx.addClue("clue");
        assertThat(ctx.getClues()).hasSize(1).containsExactly("clue");
    }

    @Test
    public void addClueMultiple() throws Exception {
        ctx.addClue("clue1");
        ctx.addClue("clue2");
        ctx.addClue("clue3");
        assertThat(ctx.getClues()).hasSize(3).containsExactly("clue1", "clue2", "clue3");
    }

    @Test
    public void resetClues() throws Exception {
        ctx.addClue("clue");
        assertThat(ctx.getClues()).isNotEmpty();

        ctx.resetClues();
        assertThat(ctx.getClues()).isEmpty();
    }

    @Test
    public void resetDoesNotClearStore() throws Exception {
        ctx.resetClues();
        assertThat(ctx.retrieve("key")).isNull();

        ctx.store("key", "value");
        assertThat(ctx.retrieve("key")).isEqualTo("value");

        ctx.resetClues();
        assertThat(ctx.retrieve("key")).isEqualTo("value");
    }

}
