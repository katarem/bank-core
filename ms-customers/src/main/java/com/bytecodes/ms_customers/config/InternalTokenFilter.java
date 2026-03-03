package com.bytecodes.ms_customers.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class InternalTokenFilter extends OncePerRequestFilter {

    @Value("${allowed.services}")
    private String[] availableServices;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String internalServiceToken = request.getHeader("X-API-TOKEN");

        if(internalServiceToken == null || internalServiceToken.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        if(!Arrays.asList(availableServices).contains(internalServiceToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        String role = "ROLE_SERVICE";
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(internalServiceToken, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(token);
        filterChain.doFilter(request, response);
    }
}
