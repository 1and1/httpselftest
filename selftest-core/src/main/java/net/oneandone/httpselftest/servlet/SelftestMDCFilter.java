package net.oneandone.httpselftest.servlet;

import static net.oneandone.httpselftest.test.run.TestRunner.X_REQUEST_ID;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.MDC;

public class SelftestMDCFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        boolean runIdWasStored = false;
        try {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                String runId = httpRequest.getHeader(X_REQUEST_ID);
                if (runId != null) {
                    runIdWasStored = true;
                    MDC.put(X_REQUEST_ID, runId);
                }
            }
            chain.doFilter(request, response);
        } finally {
            if (runIdWasStored) {
                MDC.remove(X_REQUEST_ID);
            }
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // noop
    }

    @Override
    public void destroy() {
        // noop
    }

}
