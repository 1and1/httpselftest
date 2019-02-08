package net.oneandone.httpselftest.test.run;

public final class TestRunResult {

    public final ResultType type;

    public final String assertionMessage;

    public final Exception uncaught;

    private TestRunResult(ResultType type, String assertionMessage, Exception uncaught) {
        this.type = type;
        this.assertionMessage = assertionMessage;
        this.uncaught = uncaught;
    }

    public static TestRunResult success() {
        return new TestRunResult(ResultType.SUCCESS, null, null);
    }

    public static TestRunResult failure(String errorMessage) {
        return new TestRunResult(ResultType.FAILURE, errorMessage, null);
    }

    public static TestRunResult error(Exception e) {
        return new TestRunResult(ResultType.ERROR, null, e);
    }

}
