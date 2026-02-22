package com.bytecodes.ms_customers.config;

import com.bytecodes.ms_customers.service.AuthService;
import com.bytecodes.ms_customers.service.UserDetailsServiceImpl;
import com.bytecodes.ms_customers.util.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final UserDetailsServiceImpl authService;
    private final JwtUtil jwtUtil;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty()) {
            uri = uri.substring(ctx.length());
        }

        return uri.startsWith("/api/auth/")
            || uri.matches("/api/customers/[0-9a-fA-F-]+/validate")
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

        String username = jwtUtil.extractUsername(token);
        UserDetails user = authService.loadUserByUsername(username);
        if(user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // Guardamos contexto de la autenticación en el resto de la llamada
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

}
