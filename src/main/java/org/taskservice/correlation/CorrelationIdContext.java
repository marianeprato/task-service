package org.taskservice.correlation;

/**
 * Carries the current request's correlation id across the thread that
 * handles it, so outbound REST calls and published Kafka events can tag
 * themselves with the id the inbound request arrived with (or was
 * generated for).
 */
public final class CorrelationIdContext {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CorrelationIdContext() {}

    public static void set(String correlationId) {
        CURRENT.set(correlationId);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
