package com.gout.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void uses_safe_incoming_request_id_and_exposes_it_to_mdc_and_response() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER_NAME, "web-req_1234");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, capturingChain((req, resp) -> {
            assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isEqualTo("web-req_1234");
            assertThat(req.getAttribute(RequestIdFilter.REQUEST_ATTRIBUTE)).isEqualTo("web-req_1234");
            assertThat(resp.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo("web-req_1234");
        }));

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo("web-req_1234");
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void generates_request_id_when_incoming_value_is_unsafe() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER_NAME, "bad\nheader");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, capturingChain((req, resp) -> {
            String generated = resp.getHeader(RequestIdFilter.HEADER_NAME);
            assertThat(generated).isNotEqualTo("bad\nheader");
            assertThat(generated).matches("[0-9a-f-]{36}");
            assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isEqualTo(generated);
        }));

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void clears_mdc_even_when_downstream_filter_fails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            filter.doFilter(request, response, (req, resp) -> {
                throw new RuntimeException("boom");
            });
        } catch (Exception ignored) {
            // expected
        }

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isNotBlank();
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    private FilterChain capturingChain(CheckedChain checkedChain) {
        return (request, response) -> {
            try {
                checkedChain.doFilter(
                        (MockHttpServletRequest) request,
                        (MockHttpServletResponse) response);
            } catch (ServletException | IOException e) {
                throw e;
            } catch (Exception e) {
                throw new ServletException(e);
            }
        };
    }

    @FunctionalInterface
    private interface CheckedChain {
        void doFilter(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception;
    }
}
