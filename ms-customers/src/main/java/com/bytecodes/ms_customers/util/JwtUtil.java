package com.bytecodes.ms_customers.util;

import com.bytecodes.ms_customers.model.Customer;
import com.bytecodes.ms_customers.model.JwtClaim;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Getter
    @Value("${jwt.expiration}")
    private long expiration;

    public String generateToken(Customer customer) {

        log.debug("Entering JwtUtil > generateToken");

        Date now = new Date();
        Date exp = new Date(now.getTime() + expiration);

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));

        Map<String, String> extraInfo = Map.of(
                "customerId", customer.getId().toString(),
                "role", customer.getRole().name()
        );

        var token = Jwts.builder()
                .subject(customer.getEmail())
                .issuedAt(now)
                .expiration(exp)
                .claims(extraInfo)
                .signWith(key)
                .compact();

        log.debug("Exiting JwtUtil > generateToken");

        return token;
    }

    public boolean validateToken(String token) {
        try {

            log.debug("Entering JwtUtil > validateToken");

            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
            JwtParser parser = Jwts.parser()
                    .setSigningKey(key)
                    .build();
            parser.parse(token);
            return true;
        } catch (JwtException e) {
            return false;
        } finally {
            log.debug("Exiting JwtUtil > validateToken");
        }
    }

    public String extractUsername(String token) {

        log.debug("Entering JwtUtil > extractUsername");

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));

        var jwt = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);

        log.debug("Exiting JwtUtil > extractUsername");

        return jwt.getPayload().getSubject();
    }

    private Claims extractAllClaims(String token) {

        log.debug("Entering JwtUtil > extractAllClaims");

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));

        var claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        log.debug("Exiting JwtUtil > extractAllClaims");

        return claims;
    }

    public Object extractClaim(String token, JwtClaim claim) {
        log.debug("Entering JwtUtil > extractClaim");
        var obtainedClaim = extractAllClaims(token)
                .get(claim.getClaimName(), claim.getType());
        log.debug("Exiting JwtUtil > extractClaim");
        return obtainedClaim;
    }

}
