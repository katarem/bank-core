package com.bytecodes.ms_customers.config;

import com.bytecodes.ms_customers.model.AuthPrincipal;
import com.bytecodes.ms_customers.model.JwtClaim;
import com.bytecodes.ms_customers.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig
class JwtAuthorizationFilterTest {

    @Mock
    JwtUtil jwtUtil;

    @Mock
    FilterChain filterChain;

    JwtAuthorizationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthorizationFilter(jwtUtil);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void auth_header_missing() throws ServletException, IOException {

        // given
        var req = new MockHttpServletRequest("GET", "/api/customers");
        var res = new MockHttpServletResponse();

        // when & then
        filter.doFilterInternal(req, res, filterChain);

        verifyNoInteractions(jwtUtil);
        verify(filterChain).doFilter(req, res);
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

        verifyNoInteractions(jwtUtil);
        verify(filterChain).doFilter(req, res);
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

        verify(jwtUtil).validateToken("invalid-token");
        verify(jwtUtil, never()).extractUsername(anyString());
        verify(jwtUtil, never()).extractClaim(anyString(), any(JwtClaim.class));
        verify(filterChain).doFilter(req, res);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void valid_token_sets_security_context() throws ServletException, IOException {

        // given
        var req = new MockHttpServletRequest("GET", "/api/customers");
        req.addHeader("Authorization", "Bearer valid-token");
        var res = new MockHttpServletResponse();

        // when
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("customer@email.com");
        when(jwtUtil.extractClaim("valid-token", JwtClaim.CUSTOMER_ID)).thenReturn("customer-123");
        when(jwtUtil.extractClaim("valid-token", JwtClaim.ROLE)).thenReturn("ADMIN");

        // then
        filter.doFilterInternal(req, res, filterChain);

        verify(jwtUtil).validateToken("valid-token");
        verify(jwtUtil).extractUsername("valid-token");
        verify(jwtUtil).extractClaim("valid-token", JwtClaim.CUSTOMER_ID);
        verify(jwtUtil).extractClaim("valid-token", JwtClaim.ROLE);
        verify(filterChain).doFilter(req, res);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, auth);
        assertEquals("ROLE_ADMIN", auth.getAuthorities().iterator().next().getAuthority());
        assertNotNull(auth.getDetails());

        assertInstanceOf(AuthPrincipal.class, auth.getPrincipal());
        AuthPrincipal principal = (AuthPrincipal) auth.getPrincipal();
        assertEquals("customer@email.com", principal.getUsername());
        assertEquals("customer-123", principal.getCustomerId());
    }

    @Test
    void auth_header_without_bearer_prefix_uses_raw_token() throws ServletException, IOException {

        // given
        var req = new MockHttpServletRequest("GET", "/api/customers");
        req.addHeader("Authorization", "plain-token");
        var res = new MockHttpServletResponse();

        // when
        when(jwtUtil.validateToken("plain-token")).thenReturn(false);

        // then
        filter.doFilterInternal(req, res, filterChain);

        verify(jwtUtil).validateToken("plain-token");
        verify(jwtUtil, never()).extractUsername(anyString());
        verify(filterChain).doFilter(req, res);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
