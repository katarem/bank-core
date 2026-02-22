package com.bytecodes.ms_accounts.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

public class TokenUtil {

    private TokenUtil(){

    }

    public static String generateToken(String secret, String username, UUID customerId) {

        Date now = new Date();
        Date expiration = new Date(now.toInstant().toEpochMilli() + 31_536_000_000L);

        return Jwts
                .builder()
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .expiration(expiration)
                .issuedAt(now)
                .subject(username)
                .claim("customerId", customerId.toString())
                .compact();
    }

}
