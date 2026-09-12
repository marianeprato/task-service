package org.taskservice.correlation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdClientInterceptorTest {

    private final CorrelationIdClientInterceptor interceptor = new CorrelationIdClientInterceptor();

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
    }

    @Test
    void addsCorrelationIdHeaderWhenPresentInContext() throws Exception {
        CorrelationIdContext.set("abc-123");
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost/reminders/1"));
        ClientHttpRequestExecution execution = (req, body) -> new MockClientHttpResponse(new byte[0], 200);

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(CorrelationIdContext.HEADER_NAME)).isEqualTo("abc-123");
    }

    @Test
    void doesNotAddHeaderWhenContextEmpty() throws Exception {
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost/reminders/1"));
        ClientHttpRequestExecution execution = (req, body) -> new MockClientHttpResponse(new byte[0], 200);

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().containsKey(CorrelationIdContext.HEADER_NAME)).isFalse();
    }
}
