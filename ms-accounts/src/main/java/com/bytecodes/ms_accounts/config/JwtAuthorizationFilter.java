package com.bytecodes.ms_accounts.config;

import com.bytecodes.ms_accounts.model.AuthPrincipal;
import com.bytecodes.ms_accounts.model.JwtClaim;
import com.bytecodes.ms_accounts.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if(authHeader == null || authHeader.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.replace("Bearer ", "");

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
        authPrincipal.setCustomerId(UUID.fromString(customerId));

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(authPrincipal, null, authorities);

        // Holds the auth for the rest of the call
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

}