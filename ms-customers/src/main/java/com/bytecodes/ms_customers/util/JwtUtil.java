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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Getter
    @Value("${jwt.expiration}")
    private long expiration;

    public String generateToken(Customer customer) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expiration);

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));

        Map<String, String> extraInfo = Map.of(
                "customerId", customer.getId().toString(),
                "role", customer.getRole().name()
        );

        return Jwts.builder()
                .subject(customer.getEmail())
                .issuedAt(now)
                .expiration(exp)
                .claims(extraInfo)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
            JwtParser parser = Jwts.parser()
                    .setSigningKey(key)
                    .build();
            parser.parse(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public String extractUsername(String token) {

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));

        var jwt = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
        return jwt.getPayload().getSubject();
    }

    private Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Object extractClaim(String token, JwtClaim claim) {
        return extractAllClaims(token)
                .get(claim.getClaimName(), claim.getType());
    }

}
