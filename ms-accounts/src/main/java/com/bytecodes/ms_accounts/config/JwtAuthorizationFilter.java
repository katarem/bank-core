package com.bytecodes.ms_accounts.config;

import com.bytecodes.ms_accounts.model.AuthPrincipal;
import com.bytecodes.ms_accounts.model.JwtClaim;
import com.bytecodes.ms_accounts.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty()) {
            uri = uri.substring(ctx.length());
        }

        return uri.startsWith("/api/auth/")
                || uri.equals("/actuator/health")
                || uri.startsWith("/swagger-ui/")
                || uri.startsWith("/v3/api-docs")
                || uri.equals("/actuator/prometheus");
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if(authHeader == null || authHeader.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String token = request.getHeader("Authorization").replace("Bearer ", "");

        boolean isValidToken = jwtUtil.validateToken(token);

        if(!isValidToken){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String role = (String) jwtUtil.extractClaim(token, JwtClaim.ROLE);
        String username = jwtUtil.extractUsername(token);
        String customerId = (String) jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID);

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase()));

        AuthPrincipal authPrincipal = new AuthPrincipal();
        authPrincipal.setUsername(username);
        authPrincipal.setCustomerId(customerId);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(authPrincipal, null, authorities);

        // Holds the auth for the rest of the call
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

}
