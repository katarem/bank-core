package com.bytecodes.ms_customers.config;

import com.bytecodes.ms_customers.service.AuthService;
import com.bytecodes.ms_customers.service.UserDetailsServiceImpl;
import com.bytecodes.ms_customers.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig
class JwtAuthorizationFilterTest {

    @Mock
    AuthService authService;

    @Mock
    UserDetailsServiceImpl userDetailsService;

    @Mock
    JwtUtil jwtUtil;

    @Mock
    FilterChain filterChain;

    JwtAuthorizationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthorizationFilter(userDetailsService, jwtUtil);
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
        when(userDetailsService.loadUserByUsername("customer")).thenReturn(null);

        // then
        filter.doFilterInternal(req, res, filterChain);

        assertEquals(401, res.getStatus());
        verify(jwtUtil).validateToken("valid-token");
        verify(jwtUtil).extractUsername("valid-token");
        verify(userDetailsService).loadUserByUsername("customer");
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
        when(userDetailsService.loadUserByUsername("customer")).thenReturn(user);

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
