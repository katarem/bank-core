package com.bytecodes.ms_accounts.util;

import com.bytecodes.ms_accounts.model.JwtClaim;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "xPHozB2HdjwZdfUGYXkZKnx1JgF+qwYdhQDrCDpO9U4=";
    private JwtUtil jwtUtil;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86_400_000L);
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }

    @Test
    void validate_token_successful() {
        String token = buildToken("user@email.com", UUID.randomUUID(), "CUSTOMER", 60_000);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validate_token_expired_returns_false() {
        String token = buildToken("user@email.com", UUID.randomUUID(), "CUSTOMER", -60_000);
        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void validate_token_malformed_returns_false() {
        assertFalse(jwtUtil.validateToken("invalid.token.value"));
    }

    @Test
    void extract_username_ok() {
        String token = buildToken("john.doe@email.com", UUID.randomUUID(), "CUSTOMER", 60_000);
        assertEquals("john.doe@email.com", jwtUtil.extractUsername(token));
    }

    @Test
    void extract_username_invalid_token_throws() {
        assertThrows(Exception.class, () -> jwtUtil.extractUsername("malformed"));
    }

    @Test
    void extract_customer_id_claim_ok() {
        UUID customerId = UUID.randomUUID();
        String token = buildToken("user@email.com", customerId, "CUSTOMER", 60_000);

        Object claim = jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID);
        assertEquals(customerId.toString(), claim);
    }

    @Test
    void extract_role_claim_ok() {
        String token = buildToken("user@email.com", UUID.randomUUID(), "ADMIN", 60_000);

        Object claim = jwtUtil.extractClaim(token, JwtClaim.ROLE);
        assertEquals("ADMIN", claim);
    }

    private String buildToken(String subject, UUID customerId, String role, long expirationOffsetMs) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationOffsetMs);

        return Jwts.builder()
                .subject(subject)
                .claim("customerId", customerId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }
}

