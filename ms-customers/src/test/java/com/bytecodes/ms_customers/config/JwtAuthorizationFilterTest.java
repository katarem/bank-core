package com.bytecodes.ms_customers.config;

import com.bytecodes.ms_customers.service.AuthService;
import com.bytecodes.ms_customers.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig
class JwtAuthorizationFilterTest {

    @Mock
    AuthService authService;

    @Mock
    JwtUtil jwtUtil;

    @Mock
    FilterChain filterChain;

    JwtAuthorizationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthorizationFilter(authService, jwtUtil);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @MethodSource("excludedEndpointsProvider")
    void should_not_filter_returns_true(MockHttpServletRequest req) {
        // when & then
        assertTrue(filter.shouldNotFilter(req));
    }

    private static Stream<Arguments> excludedEndpointsProvider() {
        return Stream.of(
                Arguments.of(new MockHttpServletRequest("GET", "/actuator/prometheus")),
                Arguments.of(new MockHttpServletRequest("GET", "/actuator/health")),
                Arguments.of(new MockHttpServletRequest("GET", "/api/auth/login"))
        );
    }


    @Test
    void should_not_filter_returns_false() {
        // given
        var req = new MockHttpServletRequest("GET", "/api/customers");

        // when & then
        assertFalse(filter.shouldNotFilter(req));
    }

    @Test
    void auth_header_missing() throws ServletException, IOException {

        // given
        var req = new MockHttpServletRequest("GET", "/api/customers");
        var res = new MockHttpServletResponse();

        // when & then
        filter.doFilterInternal(req, res, filterChain);

        assertEquals(401, res.getStatus());
        verifyNoInteractions(jwtUtil, authService, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void auth_header_empty() throws ServletException, IOException {

        // given
        var req = new MockHttpServletRequest("GET", "/api/customers");
        req.addHeader("Authorization", "");
        var res = new MockHttpServletResponse();

        // when & then
        filter.doFilterInternal(req, res, filterChain);

        assertEquals(401, res.getStatus());
        verifyNoInteractions(jwtUtil, authService, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void invalid_token() throws ServletException, IOException {

        // given
        var req = new MockHttpServletRequest("GET", "/api/customers");
        req.addHeader("Authorization", "Bearer invalid-token");
        var res = new MockHttpServletResponse();

        // when
        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        // then
        filter.doFilterInternal(req, res, filterChain);

        assertEquals(401, res.getStatus());
        verify(jwtUtil).validateToken("invalid-token");
        verify(jwtUtil, never()).extractUsername(anyString());
        verifyNoInteractions(authService);
        verifyNoInteractions(filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void user_not_found() throws ServletException, IOException {

        // given
        var req = new MockHttpServletRequest("GET", "/api/customers");
        req.addHeader("Authorization", "Bearer valid-token");
        var res = new MockHttpServletResponse();

        // when
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("customer");
        when(authService.loadUserByUsername("customer")).thenReturn(null);

        // then
        filter.doFilterInternal(req, res, filterChain);

        assertEquals(401, res.getStatus());
        verify(jwtUtil).validateToken("valid-token");
        verify(jwtUtil).extractUsername("valid-token");
        verify(authService).loadUserByUsername("customer");
        verifyNoInteractions(filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void filters_ok()
            throws ServletException, IOException {

        // given
        var req = new MockHttpServletRequest("GET", "/api/customers");
        req.addHeader("Authorization", "Bearer valid-token");
        var res = new MockHttpServletResponse();

        UserDetails user = new User("customer", "pw", List.of());

        // when
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("customer");
        when(authService.loadUserByUsername("customer")).thenReturn(user);

        // then
        filter.doFilterInternal(req, res, filterChain);

        assertNotEquals(401, res.getStatus());

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("customer", auth.getName());
        assertSame(user, auth.getPrincipal());
        assertNotNull(auth.getDetails());

        verify(filterChain).doFilter(req, res);
    }
}
