package com.bytecodes.ms_customers.config;

import com.bytecodes.ms_customers.model.AuthPrincipal;
import com.bytecodes.ms_customers.model.JwtClaim;
import com.bytecodes.ms_customers.service.AuthService;
import com.bytecodes.ms_customers.service.UserDetailsServiceImpl;
import com.bytecodes.ms_customers.util.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        log.debug("Entering JwtAuthorizationFilter > doFilterInternal");

        String authHeader = request.getHeader("Authorization");

        if(authHeader == null || authHeader.isEmpty()) {
            log.debug("Token not provided");
            log.debug("Exiting JwtAuthorizationFilter > doFilterInternal");
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("Authorization").replace("Bearer ", "");

        boolean isValidToken = jwtUtil.validateToken(token);

        if(!isValidToken){
            log.debug("Invalid token");
            log.debug("Exiting JwtAuthorizationFilter > doFilterInternal");
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtUtil.extractUsername(token);

        String customerId = (String) jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID);
        String role = (String) jwtUtil.extractClaim(token, JwtClaim.ROLE);

        AuthPrincipal authPrincipal = new AuthPrincipal(username, customerId);

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(authPrincipal, null, authorities);

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Authenticated with role={}", authorities.getFirst().getAuthority());

        log.debug("Exiting JwtAuthorizationFilter > doFilterInternal");
        filterChain.doFilter(request, response);
    }

}
