package org.taskservice.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Reads X-Correlation-Id from the incoming request (or generates one),
 * makes it available to the rest of the request via {@link CorrelationIdContext}
 * and the logging MDC, and echoes it back on the response.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(CorrelationIdContext.HEADER_NAME);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        CorrelationIdContext.set(correlationId);
        MDC.put(CorrelationIdContext.MDC_KEY, correlationId);
        response.setHeader(CorrelationIdContext.HEADER_NAME, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationIdContext.MDC_KEY);
            CorrelationIdContext.clear();
        }
    }
}
