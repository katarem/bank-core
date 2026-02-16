package com.bytecodes.ms_customers.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Getter
    @Value("${jwt.expiration}")
    private long expiration;


    public String generateToken(Authentication auth) {
        String username = auth.getName();
        Date now = new Date();
        Date exp = new Date(now.getTime() + expiration);

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(exp)
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

    public boolean isExpired(String token) {

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));

        var jwt = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);

        Date expiredBy = jwt.getPayload().getExpiration();
        Date now = new Date();

        return now.after(expiredBy);
    }


}
