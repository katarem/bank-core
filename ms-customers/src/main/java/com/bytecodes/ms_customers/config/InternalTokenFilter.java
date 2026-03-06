package com.bytecodes.ms_customers.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class InternalTokenFilter extends OncePerRequestFilter {

    @Value("${allowed.services}")
    private String[] availableServices;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        log.debug("Entering InternalTokenFilter > doFilterInternal");

        String internalServiceToken = request.getHeader("X-API-TOKEN");

        if(internalServiceToken == null || internalServiceToken.isBlank()) {
            log.debug("Token not included");
            log.debug("Exiting InternalTokenFilter > doFilterInternal");
            filterChain.doFilter(request, response);
            return;
        }

        if(!Arrays.asList(availableServices).contains(internalServiceToken)) {
            log.debug("Invalid token");
            log.debug("Exiting InternalTokenFilter > doFilterInternal");
            filterChain.doFilter(request, response);
            return;
        }

        String role = "ROLE_SERVICE";
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(internalServiceToken, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(token);
        log.debug("Valid internal token, authenticated as role={}", role);
        log.debug("Exiting InternalTokenFilter > doFilterInternal");
        filterChain.doFilter(request, response);
    }
}
