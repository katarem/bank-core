package com.bytecodes.ms_customers.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

class InternalTokenFilterTest {

    private InternalTokenFilter filter;

    @BeforeEach
    void setUp() {
        filter = new InternalTokenFilter();
        ReflectionTestUtils.setField(filter, "availableServices", new String[]{"service-a", "service-b"});
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void do_filter_without_header_does_not_authenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
        Assertions.assertNotNull(chain.getRequest());
    }

    @Test
    void do_filter_with_empty_header_does_not_authenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-TOKEN", "");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
        Assertions.assertNotNull(chain.getRequest());
    }

    @Test
    void do_filter_with_unknown_token_does_not_authenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-TOKEN", "unknown-service");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
        Assertions.assertNotNull(chain.getRequest());
    }

    @Test
    void do_filter_with_valid_token_authenticates_as_service_role() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-TOKEN", "service-a");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Assertions.assertNotNull(authentication);
        Assertions.assertEquals("service-a", authentication.getPrincipal());
        Assertions.assertEquals(List.of("ROLE_SERVICE"),
                authentication.getAuthorities().stream().map(Object::toString).toList());
        Assertions.assertNotNull(chain.getRequest());
    }

    @Test
    void do_filter_with_second_valid_token_also_authenticates() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-TOKEN", "service-b");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Assertions.assertNotNull(authentication);
        Assertions.assertEquals("service-b", authentication.getPrincipal());
        Assertions.assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SERVICE".equals(a.getAuthority())));
    }
}

