package net.oneandone.httpselftest.servlet;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SelftestMDCFilterTest {

    private static final String MDC_ADAPTER_FIELDNAME = "mdcAdapter";

    @Mock
    private HttpServletRequest request;

    @Mock
    private ServletResponse response;

    @Mock
    private FilterChain chain;

    @Mock
    private MDCAdapter mdcMock;

    private MDCAdapter oldAdapter;

    private SelftestMDCFilter filter;

    @Before
    public void prepareMDC() {
        oldAdapter = (MDCAdapter) ReflectionTestUtils.getField(MDC.class, MDC_ADAPTER_FIELDNAME);
        ReflectionTestUtils.setField(MDC.class, MDC_ADAPTER_FIELDNAME, mdcMock);
        filter = new SelftestMDCFilter();
    }

    @After
    public void cleanup() {
        ReflectionTestUtils.setField(MDC.class, MDC_ADAPTER_FIELDNAME, oldAdapter);
    }

    @Test
    public void noActionWithoutHeader() throws Exception {
        filter.doFilter(request, response, chain);
        verifyNoInteractions(mdcMock);
    }

    @Test
    public void mdcIsSetAndClearedWithHeader() throws Exception {
        when(request.getHeader("X-REQUEST-ID")).thenReturn("runId1");

        filter.doFilter(request, response, chain);

        verify(mdcMock).put("X-REQUEST-ID", "runId1");
        verify(mdcMock).remove("X-REQUEST-ID");
        verifyNoMoreInteractions(mdcMock);
    }

}
