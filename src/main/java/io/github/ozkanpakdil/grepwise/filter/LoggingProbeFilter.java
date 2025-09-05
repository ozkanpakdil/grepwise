package io.github.ozkanpakdil.grepwise.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logging-only probe to identify problematic non-API requests/dispatchers during hard reloads.
 * No behavioral changes; strictly logs diagnostics and passes the request through.
 */
public class LoggingProbeFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(LoggingProbeFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String accept = request.getHeader("Accept");
        var dispatcher = request.getDispatcherType();

        // Skip API paths; this probe is only for non-API requests
        if (path != null && path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("[Probe] Non-API request path={}, method={}, dispatcher={}, accept={}", path, method, dispatcher, accept);
        }

        // If we are in ERROR dispatch for a static asset request, short-circuit with 404 to avoid recursive forwards
        if (dispatcher == jakarta.servlet.DispatcherType.ERROR) {
            Object originalPath = request.getAttribute("jakarta.servlet.error.request_uri");
            String orig = originalPath instanceof String ? (String) originalPath : null;
            Object statusAttr = request.getAttribute("jakarta.servlet.error.status_code");
            int status = (statusAttr instanceof Integer) ? (Integer) statusAttr : HttpServletResponse.SC_NOT_FOUND;

            // Short-circuit all non-API ERROR dispatches to avoid recursive forwards/includes
            if (logger.isDebugEnabled()) {
                logger.debug("[Probe] Short-circuiting ERROR dispatch for origPath={} with status={}", orig, status);
            }
            if (!response.isCommitted()) {
                response.setStatus(status);
                response.setContentType("text/plain");
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        // We want to see ERROR dispatches to catch recursive forwards
        return false;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }
}
