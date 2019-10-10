package net.oneandone.httpselftest.writer;

import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.input;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.span;
import static j2html.TagCreator.table;
import static j2html.TagCreator.text;
import static j2html.TagCreator.tr;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static net.oneandone.httpselftest.servlet.SelftestServlet.PARAMETER_PREFIX;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import net.oneandone.httpselftest.common.Pair;
import net.oneandone.httpselftest.http.Headers;
import net.oneandone.httpselftest.http.HttpDetails;
import net.oneandone.httpselftest.http.HttpException;
import net.oneandone.httpselftest.http.WrappedRequest;
import net.oneandone.httpselftest.http.WrappedResponse;
import net.oneandone.httpselftest.http.presenter.FormEntityPresenter;
import net.oneandone.httpselftest.http.presenter.RawHttpPresenter;
import net.oneandone.httpselftest.http.presenter.HttpPresenter;
import net.oneandone.httpselftest.http.presenter.JsonEntityPresenter;
import net.oneandone.httpselftest.http.presenter.PlainHttpPresenter;
import net.oneandone.httpselftest.log.EventRenderer;
import net.oneandone.httpselftest.log.LogDetails;
import net.oneandone.httpselftest.log.SelftestEvent;
import net.oneandone.httpselftest.test.api.TestCase;
import net.oneandone.httpselftest.test.api.TestConfigs;
import net.oneandone.httpselftest.test.run.ResultType;
import net.oneandone.httpselftest.test.run.SimpleContext;
import net.oneandone.httpselftest.test.run.TestRunData;
import net.oneandone.httpselftest.test.run.TestRunResult;

public class SelftestHtmlWriter extends SelfTestWriter {

    private static final Pattern HTTP_PREFIX = Pattern.compile("https?:/.*");

    private static final String SUBMIT = "submit";

    public static final String EXECUTE = "execute";

    public static final String CONFIG_ID = "config-id";

    static final long SECONDS_PER_MINUTE = 60;

    static final long SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE;

    static final long SECONDS_PER_DAY = 24 * SECONDS_PER_HOUR;

    private static final List<HttpPresenter> CONTENT_PRESENTERS;

    static {
        CONTENT_PRESENTERS = new ArrayList<>();
        CONTENT_PRESENTERS.add(new PlainHttpPresenter());
        CONTENT_PRESENTERS.add(new RawHttpPresenter());
        CONTENT_PRESENTERS.add(new FormEntityPresenter());
        CONTENT_PRESENTERS.add(new JsonEntityPresenter());
    }

    public SelftestHtmlWriter(PrintWriter w) {
        super(w);
    }

    @Override
    public void writeText(String paragraph) {
        write(textBlock(paragraph));
    }

    @Override
    public void writePageStart(TestConfigs configs, Set<String> relevantConfigIds, TestConfigs.Values paramsToUse,
            String servletName, String testsBaseUrl, Instant lastTestRun, String callerIp, String lastTestrunIp) {
        writeDirect("<!doctype html>");
        writeDirect("<head><meta charset='utf-8'/>");
        writeCSS();
        writeDirect("</head>");
        writeDirect("<body>");
        writeTestcaseToggleScript();
        write(metainfoBlock(testsBaseUrl, lastTestRun, callerIp, lastTestrunIp));
        write(testParametersForm(configs, paramsToUse, servletName));
        providedConfigsForm(configs, relevantConfigIds, paramsToUse.activeConfigId()).ifPresent(this::write);
        write(div().withClass("clear"));
    }

    @Override
    public void writePageEnd() {
        writeDirect("</body>");
    }

    @Override
    public void writeUnrunTests(List<TestCase> tests) {
        writeText("Available test cases:");
        for (TestCase test : tests) {
            write(unrunTestAsDom(test));
        }
    }

    @Override
    public void writeTestOutcome(TestRunData testRun, List<LogDetails> logs, SimpleContext ctx) {
        // TODO remove foreign logs logic? should be impossible now with runId separation.
        boolean hasForeignLogs = hasLogEvent(logs, (evt, renderer) -> !testRun.runId.equals(evt.runId));
        boolean hasErrorLogs = hasLogEvent(logs, (evt, renderer) -> renderer.getLevel(evt.event).equals("ERROR"));
        boolean hasWarnLogs = hasLogEvent(logs, (evt, renderer) -> renderer.getLevel(evt.event).equals("WARN"));
        boolean logHasOverflown = logs.stream().anyMatch(details -> details.logs.hasOverflown);
        boolean slowResponse = testRun.getDurationMillis() > testRun.getMaxDurationMillis();

        String resultClass = "testcase test-" + testRun.getResult().type.name().toLowerCase();
        if (hasForeignLogs || hasErrorLogs || hasWarnLogs || slowResponse) {
            resultClass = resultClass + " warn";
        }

        ContainerTag div = div().withClasses("group", "nonempty", resultClass).with( //
                h2().with( //
                        text(testRun.testName + " (" + testRun.getDurationMillis() + "ms) - " + testRun.getResult().type), //
                        caretSpan(), //
                        text(ctx.getClues().isEmpty() ? "" : ctx.getClues().toString()), //
                        indicator(hasForeignLogs, "foreignlogs", "There are log messages from another test."), //
                        indicator(hasErrorLogs, "errorlogs", "There are log messages on ERROR."), //
                        indicator(hasWarnLogs, "warnlogs", "There are log messages on WARN."), //
                        indicator(slowResponse, "slowresponse", "The response was slower than expected."), //
                        indicator(logHasOverflown, "logoverflow", "Log buffer has overflown."))
                        .attr("title", testRun.runId + " @ " + EventRenderer.TIMESTAMP_FORMATTER.format(testRun.startInstant)), //
                div().withClass("contents").with( //
                        concat(messageIfExistsAsDom(testRun.getResult()), //
                                requestIfExistsAsDom(testRun.getRequest()), //
                                responseIfExistsAsDom(testRun.getResponse()), //
                                partialResponseIfExistsAsDom(testRun.getResult().uncaught), //
                                exceptionIfExistsAsDom(testRun.getResult().uncaught), //
                                applicationLogIfExistsAsDom(logs, testRun.runId))));
        write(div);
    }

    private boolean hasLogEvent(List<LogDetails> logs, BiPredicate<SelftestEvent, EventRenderer> pred) {
        return logs.stream().anyMatch(details -> details.logs.events.stream().anyMatch(evt -> pred.test(evt, details.renderer)));
    }

    private static ContainerTag indicator(boolean condition, String clazz, String msg) {
        // withCondClass overwrites indicator-inactive
        return span("!").withClass("indicator-inactive").withCondClass(condition, "indicator " + clazz).attr("title", msg);
    }

    @Override
    public void writeUncaughtException(Throwable t) {
        write(h2("UNCAUGHT EXCEPTION"), div(textBlock(stacktraceAsString(t))));
    }

    private void write(DomContent... all) {
        for (DomContent content : all) {
            writeDirect(content.render());
        }
    }

    private void writeDirect(String s) {
        writer.write(s);
        writer.write("\n");
    }

    // FIXME extract to separate file using TagCreator#styleWithInlineFile.
    private void writeCSS() {
        writeDirect("<style>");
        writeDirect("  :root { --darkred: #c00; --darkorange: #d70; }");
        writeDirect("  body { margin: 10px; padding: 0; background-color: #444; color: #ccc; "
                + " font-family: Verdana, Arial, sans-serif; font-size: 12px; }");
        writeDirect("  h2 { margin: 5px; font-size: 19px; }");
        writeDirect("  h3 { font-size: 15px; }");
        writeDirect("  input { padding: 3px; margin: 1px 3px; border: 0; background-color: #202020; color: #bbb; "
                + " border-radius: 3px; }");
        writeDirect("  .fixed { color: #999; }");
        writeDirect("  input[readonly] { color: #666; }");
        writeDirect("  input[type=submit] { padding: 4px 7px; cursor: pointer; }");
        writeDirect("  .block { margin-bottom: 10px; }");
        writeDirect("  div.mono span { white-space: pre-wrap; word-break: break-all; }");
        writeDirect("  span.warn { color: var(--darkred); }");
        writeDirect("  span.url { text-decoration: underline; font-weight: bold; }");
        writeDirect("  #metainfo td:first-child { padding-right: 10px; }");
        writeDirect("  #params, #configs { float: left; }");
        writeDirect("  #configs table { float: left; }");
        writeDirect("  #configs table.otherMarkets input { color: #666; }");
        writeDirect("  #configs .activeConfigId { box-shadow: 0 0 0 2px #3a3; }"); // green
        writeDirect("  .clear { clear: both; }");
        writeDirect("  .group { margin: 12px 0px; padding: 4px 6px; background-color: #222; color: #bbb; "
                + " border-radius: 3px; }");
        writeDirect("  div.mono.log > *:hover { background-image: linear-gradient(to right, #222, #0e4dab); }");
        writeDirect("  .group > * { font-family: monospace; }");
        writeDirect("  .group h2, .group h3 { font-family: Verdana, Arial, sans-serif; }");
        writeDirect("  span.mono { font-family: monospace; }");

        writeDirect("  .testcase { border-left-width: 14px; border-left-style: solid; }");
        writeDirect("  .test-error { border-left-color: var(--darkred); }");
        writeDirect("  .test-failure { border-left-color: var(--darkorange); }");
        writeDirect("  .test-success { border-left-color: #3a3; }");
        writeDirect("  .test-success.warn { border-left-color: #a9b630; }");
        writeDirect("  .test-unrun   { border-left-color: #888; }");

        writeDirect("  .indicator-inactive { display: none; }");
        writeDirect("  .indicator { float: right; border-radius: 3px; margin: 0 2px; "
                + "padding: 2px 8px; line-height: 16px; font-size: 16px; color: black; }");
        writeDirect("  .indicator.foreignlogs { background-color: #034cde; }"); // blue
        writeDirect("  .indicator.errorlogs { background-color: var(--darkred); }");
        writeDirect("  .indicator.warnlogs { background-color: var(--darkorange); }");
        writeDirect("  .indicator.slowresponse { background-color: grey; }");
        writeDirect("  .indicator.logoverflow { background-color: #b610b6; }"); // purple

        writeDirect("  div.mono > div > * { text-indent: -20px; display: inline-block; margin-left: 20px; }");

        writeDirect("  .js .group div.contents { display: none; }");
        writeDirect("  .js .group.open div.contents { display: block; }");
        writeDirect("  .js .group.nonempty h2 { cursor: pointer; }");
        writeDirect("  .js h2 .caret { display: inline-block; width: 0px; height: 0; margin: 0 4px; vertical-align: middle;");
        writeDirect("   border-top: 6px solid; border-right: 6px solid transparent; border-left: 6px solid transparent; }");
        writeDirect("  .js h2:hover .caret { margin-top: 3px; }");

        writeDirect("  .level-trace { color: #777; }");
        writeDirect("  .level-debug { color: #777; }");
        writeDirect("  .level-info  { }"); // default color
        writeDirect("  .level-warn  { color: var(--darkorange); }");
        writeDirect("  .level-error { color: var(--darkred); }");
        writeDirect("  .level-fatal { color: var(--darkred); }");

        writeDirect("  body:not(.js) .presentationToggle { display: none; }");
        writeDirect("  body:not(.js) .presenterContent ~ .presenterContent { display: none; }");
        writeDirect("  .presentationToggle { padding: 2px 5px; margin-left: 5px; background-color: #fff2; border-radius: 3px; "
                + "font-family: monospace; cursor: pointer; font-size: 12px; }");
        writeDirect("  .presentationToggle.active { background-color: #fff4; }");
        writeDirect("  .js .presenterContent:not(.active) { display: none; }");

        writeDirect("  ");
        writeDirect("</style>");
    }

    // FIXME extract to separate file using TagCreator#scriptWithInlineFile.
    private void writeTestcaseToggleScript() {
        writeDirect("<script>");
        writeDirect("document.querySelector('body').classList.add('js')");
        writeDirect("document.querySelector('body').onclick = function(evt) {");

        // test case collapsing
        writeDirect("   var closestH2 = evt.target;");
        writeDirect("   while(closestH2 != null && closestH2.nodeName != 'H2' ) {");
        writeDirect("     closestH2 = closestH2.parentNode");
        writeDirect("   }");
        writeDirect(
                "   if (closestH2 != null && closestH2.nodeName==='H2' && closestH2.parentNode.classList.contains('group')) {");
        writeDirect("      var group = closestH2.parentNode");
        writeDirect("      if (group.classList.contains('open')) {");
        writeDirect("         group.classList.remove('open')");
        writeDirect("      } else {");
        writeDirect("         group.classList.add('open')");
        writeDirect("      }");
        writeDirect("   }");

        // http presentation switching
        writeDirect("  if (evt.target.classList.contains('presentationToggle')) {");
        writeDirect("    var toggleNode = evt.target;");
        writeDirect("    var presenterId = toggleNode.textContent;");
        writeDirect("    var headerNode = toggleNode.parentNode;"); // ^ h3
        writeDirect("    var httpNode = headerNode.nextElementSibling;"); // ~ div
        writeDirect("    ");
        writeDirect("    headerNode.querySelectorAll('.active').forEach(function(child) {");
        writeDirect("      child.classList.remove('active');");
        writeDirect("    });");
        writeDirect("    httpNode.querySelectorAll('.active').forEach(function(child) {");
        writeDirect("      child.classList.remove('active');");
        writeDirect("    });");
        writeDirect("    toggleNode.classList.add('active');");
        writeDirect("    httpNode.querySelector('.presenterContent.' + presenterId).classList.add('active');");
        writeDirect("  }");

        writeDirect("}");
        writeDirect("</script>");
    }

    DomContent metainfoBlock(String testsBaseUrl, Instant lastTestRun, String callerIp, String lastTestrunIp) {
        String lastRunText =
                lastTestRun == null ? "never" : formattedDurationWithIpHint(lastTestRun, Instant.now(), callerIp, lastTestrunIp);

        return div().withId("metainfo").withClass("block").with( //
                table( //
                        row(text("Test Endpoint"), text(testsBaseUrl + "...")), //
                        row(text("Last test run"), text(lastRunText)) //
                ));
    }

    DomContent testParametersForm(TestConfigs configs, TestConfigs.Values params, String servletName) {
        return div().withId("params").withClass("block").with( //
                form().withMethod("POST").withAction(servletName).with( //
                        text("Test parameters:"), //
                        table(configs.getParameterNames().stream().map(name -> row( //
                                span(name).withCondClass(params.isFixed(name), "fixed"), //
                                input().withType("text").withName(PARAMETER_PREFIX + name)
                                        .condAttr(params.isFixed(name), "readonly", "true").withValue(params.get(name))))
                                .toArray(ContainerTag[]::new)), //
                        input().withCondHidden(true).withName(CONFIG_ID).withValue(params.activeConfigId().orElse("")), //
                        input().withType(SUBMIT).withName(EXECUTE).withValue("Run tests") //
                ));
    }

    Optional<DomContent> providedConfigsForm(TestConfigs configs, Set<String> configIdsForCurrentMarket,
            Optional<String> activeConfigId) {
        if (configs.isEmpty()) {
            return Optional.empty();
        }
        Set<String> allIds = configs.getIds();
        boolean hasFixedParams = configs.hasFixedParams();

        final Set<String> firstClassIds;
        final Set<String> secondClassIds;
        if (configIdsForCurrentMarket.isEmpty()) {
            firstClassIds = allIds;
            secondClassIds = Collections.emptySet();
        } else {
            firstClassIds = configIdsForCurrentMarket;
            secondClassIds = allIds.stream().filter(id -> !configIdsForCurrentMarket.contains(id)).collect(toSet());
        }

        ContainerTag form = form().withMethod("GET").with( //
                div("Available pre-defined parameter sets for this application. Click to transfer values into form:"), //
                configsTableAsDom(firstClassIds, "relevantMarkets", activeConfigId, hasFixedParams));
        if (!secondClassIds.isEmpty()) {
            form.with(configsTableAsDom(secondClassIds, "otherMarkets", activeConfigId, hasFixedParams));
        }

        return Optional.of(div().withId("configs").withClass("block").with(form));
    }

    @SafeVarargs
    private static DomContent[] concat(List<DomContent>... parts) {
        List<DomContent> combined = new LinkedList<>();

        for (List<DomContent> elements : parts) {
            for (DomContent element : elements) {
                combined.add(element);
            }
        }
        return combined.toArray(new DomContent[0]);
    }

    @SafeVarargs
    private static <T> List<T> listOf(T... elements) {
        return Arrays.asList(elements);
    }

    private static ContainerTag row(DomContent... elements) {
        return tr(listOf(elements).stream().map( //
                TagCreator::td).toArray(ContainerTag[]::new));
    }

    private static ContainerTag monospacedParagraph(String text) {
        return div().withClass("mono").with(textBlock(text));
    }

    private static DomContent textBlock(String paragraph) {
        List<DomContent> pieces = new ArrayList<>();
        for (String line : paragraph.split("\\n")) {
            pieces.add(span(text(line)));
            pieces.add(br());
        }
        return div(pieces.toArray(new DomContent[0]));
    }

    // TODO the abstraction of this method broke over time. try to separate url parsing from runId handling.
    private static DomContent urlHighlightedTextBlock(String testRunId, String eventRunId, String paragraph, String logLevel) {
        List<DomContent> lines = new ArrayList<>();
        for (String line : paragraph.split("\\n")) {
            LinkedList<DomContent> parts = new LinkedList<>();
            for (String part : line.split(" ")) {
                parts.add(text(" "));
                boolean isUrl = HTTP_PREFIX.matcher(part).matches();
                parts.add(isUrl ? span(part).withClass("url") : text(part));
            }
            if (!parts.isEmpty()) {
                parts.removeFirst(); // remove leading " "
            }
            if (!testRunId.equals(eventRunId)) {
                parts.addFirst(text(" "));
                parts.addFirst(span("[" + eventRunId + "]").withClass("warn").attr("title",
                        "This log line was generated by another test!"));
            }
            lines.add(span(parts.toArray(new DomContent[0])).withClass("level-" + logLevel.toLowerCase()));
            lines.add(br());
        }
        return div(lines.toArray(new DomContent[0]));
    }

    private static DomContent logEventAsDom(SelftestEvent e, String testRunId, EventRenderer renderer) {
        return urlHighlightedTextBlock(testRunId, e.runId, renderer.doLayout(e.event), renderer.getLevel(e.event));
    }

    private static List<DomContent> messageIfExistsAsDom(TestRunResult result) {
        if (result != null && result.type == ResultType.FAILURE) {
            return listOf(h3("FAILED ASSERTION"), monospacedParagraph(result.assertionMessage));
        } else {
            return Collections.emptyList();
        }
    }

    private static List<DomContent> requestIfExistsAsDom(WrappedRequest wrapper) {
        if (wrapper == null) {
            return Collections.emptyList();
        }
        return httpBlockAsDom("REQUEST", wrapper.request.getHeaders(), wrapper.getDetails());
    }

    private static List<DomContent> responseIfExistsAsDom(WrappedResponse wrapper) {
        if (wrapper == null) {
            return Collections.emptyList();
        }
        return httpBlockAsDom("RESPONSE", wrapper.response.getHeaders(), wrapper.responseDetails);
    }

    private static List<DomContent> httpBlockAsDom(String blockName, Headers headers, HttpDetails details) {
        List<Pair<String, String>> parsedContents = CONTENT_PRESENTERS.stream() //
                .map(presenter -> parseCatchingExceptions(presenter, headers, details)) //
                .filter(pair -> pair.right.isPresent()) //
                .map(pair -> new Pair<>(pair.left, pair.right.get())) //
                .collect(Collectors.toList());

        boolean defaultWorked = parsedContents.stream().anyMatch(pair -> pair.left.equals(CONTENT_PRESENTERS.get(0).id()));

        AtomicBoolean first = new AtomicBoolean(true);
        List<DomContent> parserTags = parsedContents.stream() //
                .map(pair -> span(pair.left) //
                        .withClass("presentationToggle") //
                        .withCondClass(first.getAndSet(false), "active presentationToggle")) //
                .collect(Collectors.toList());

        first.set(true);
        List<DomContent> responseParagraphs = parsedContents.stream() //
                .map(pair -> div() //
                        .withClass("presenterContent " + pair.left) //
                        .withCondClass(first.getAndSet(false), "active presenterContent " + pair.left) //
                        .with(monospacedParagraph(pair.right))) //
                .collect(Collectors.toList());

        List<DomContent> headerElements = new LinkedList<>();
        headerElements.add(span(blockName));
        if (parserTags.size() > 1 || !defaultWorked) {
            headerElements.addAll(parserTags);
        }

        ContainerTag header = h3(concat(headerElements));

        List<DomContent> httpBlock = new LinkedList<>();
        httpBlock.add(header);
        httpBlock.add(div(concat(responseParagraphs)));

        return httpBlock;
    }

    private static Pair<String, Optional<String>> parseCatchingExceptions(HttpPresenter presenter, Headers headers,
            HttpDetails details) {
        try {
            Optional<String> presentation = presenter.parse(headers, details);
            return new Pair<>(presenter.id(), presentation);
        } catch (RuntimeException e) {
            return new Pair<>(presenter.id(), Optional.empty());
        }
    }

    private static List<DomContent> partialResponseIfExistsAsDom(Exception e) {
        if (e instanceof HttpException && ((HttpException) e).getBytes() != null) {
            try {
                byte[] bytes = ((HttpException) e).getBytes();
                String partialResponse = new String(bytes, StandardCharsets.UTF_8);
                return listOf(h3("PARTIAL RESPONSE UNTIL EXCEPTION"), monospacedParagraph(partialResponse));
            } catch (RuntimeException e2) {
                return listOf(h3("EXCEPTION DURING PARTIAL RESPONSE HANDLING: " + e2.getMessage()));
            }
        } else {
            return Collections.emptyList();
        }
    }

    private static List<DomContent> exceptionIfExistsAsDom(Exception e) {
        if (e != null) {
            return listOf(h3("EXCEPTION DURING EXECUTION"), monospacedParagraph(stacktraceAsString(e)));
        } else {
            return Collections.emptyList();
        }
    }

    private static List<DomContent> applicationLogIfExistsAsDom(List<LogDetails> list, String runId) {
        List<LogDetails> logsWithEntries =
                list.stream().filter(info -> !info.logs.events.isEmpty()).sorted(ROOT_COMP).collect(toList());

        List<DomContent> dom = new LinkedList<>();
        logsWithEntries.forEach(info -> {
            if (!info.logs.events.isEmpty()) {
                List<DomContent> header = new LinkedList<>();
                header.add(span("LOG ["));
                header.addAll(joinWithArrows(info.logNames));
                header.add(span("]"));
                dom.add(h3(concat(header)));

                if (info.logs.hasOverflown) {
                    dom.add(span("Log buffer has overflown! Oldest lines have been dropped.").withClass("warn"));
                    dom.add(br());
                }

                ContainerTag logDiv = div().withClass("mono log");
                info.logs.events.forEach(event -> logDiv.with(logEventAsDom(event, runId, info.renderer)));
                dom.add(logDiv);
            }
        });
        return dom;
    }

    private static List<DomContent> joinWithArrows(List<String> logNames) {
        LinkedList<DomContent> joined = logNames.stream().collect(LinkedList::new, (list, part) -> {
            list.add(span(part).withClass("mono"));
            list.add(rawHtml(" &rarr; "));
        }, LinkedList::addAll);
        if (!joined.isEmpty()) {
            joined.removeLast(); // trailing arrow
        }
        return joined;
    }

    private static ContainerTag configsTableAsDom(Set<String> idsToWrite, String className, Optional<String> activeConfigId,
            boolean fixedParamsExist) {
        if (idsToWrite.isEmpty()) {
            throw new IllegalStateException("called configsTable with 0 ids");
        }
        final Map<Character, List<String>> idsByFirstChar = idsToWrite.stream().collect(groupingBy(key -> key.charAt(0)));

        return table().withClass(className).with( //
                idsByFirstChar.keySet().stream().sorted().map(firstChar -> //
                row(idsByFirstChar.get(firstChar).stream().sorted().map(id -> //
                input().withType(SUBMIT).withName(CONFIG_ID).withValue(id)
                        .withCondClass(fixedParamsExist && activeConfigId.map(id::equals).orElse(false), "activeConfigId") //
                ).toArray(DomContent[]::new)) //
                ).toArray(ContainerTag[]::new) //
        );
    }

    private static ContainerTag unrunTestAsDom(TestCase test) {
        return div().withClasses("group", "test-unrun").with( //
                h2(test.getName()));
    }

    private static ContainerTag caretSpan() {
        return span().withClass("caret");
    }

    private static String formattedDurationWithIpHint(Instant lastTestRun, Instant now, String callerIp, String lastTestrunIp) {
        final String formattedDuration = formattedDurationBetween(lastTestRun, now);
        final String hint = callerIp.equals(lastTestrunIp) ? "from your IP" : "from another IP";
        return formattedDuration + " " + hint;
    }

    private static String formattedDurationBetween(Instant earlier, Instant later) {
        final long secondsTotal = Duration.between(earlier, later).getSeconds();

        final long days = secondsTotal / SECONDS_PER_DAY;
        final long hours = (secondsTotal % SECONDS_PER_DAY) / SECONDS_PER_HOUR;
        final long minutes = (secondsTotal % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
        final long seconds = secondsTotal % SECONDS_PER_MINUTE;

        final StringBuilder result = new StringBuilder();
        if (days > 0) {
            result.append(days).append("d ");
        }
        if (hours > 0) {
            result.append(hours).append("h ");
        }
        if (minutes > 0) {
            result.append(minutes).append("m ");
        }
        if (seconds > 0) {
            result.append(seconds).append("s ");
        }
        return result.length() > 0 ? result.toString() + "ago" : "just now";
    }

    private static final Comparator<? super LogDetails> ROOT_COMP = (info1, info2) -> {
        if (info1.logNames.isEmpty() || info1.logNames.get(0).equalsIgnoreCase("root")) {
            return -1;
        } else if (info2.logNames.isEmpty() || info2.logNames.get(0).equalsIgnoreCase("root")) {
            return 1;
        } else {
            return info1.logNames.get(0).compareToIgnoreCase(info2.logNames.get(0));
        }
    };

    private static String stacktraceAsString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
