package com.bytecodes.ms_customers.util;

import com.bytecodes.ms_customers.model.Customer;
import com.bytecodes.ms_customers.model.JwtClaim;
import com.bytecodes.ms_customers.model.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

class JwtUtilTest {

    private static final String SECRET = "xPHozB2HdjwZdfUGYXkZKnx1JgF+qwYdhQDrCDpO9U4=";

    private JwtUtil jwtUtil;
    private Customer customer;
    private String token;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);

        customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setRole(UserRole.CUSTOMER);
        customer.setEmail("email@email.com");
        customer.setPassword("Password123");

        token = jwtUtil.generateToken(customer);
    }

    @Test
    void create_token_successful() {
        String generatedToken = jwtUtil.generateToken(customer);
        Assertions.assertNotNull(generatedToken);
        Assertions.assertFalse(generatedToken.isEmpty());
    }

    @Test
    void validate_token_successful() {
        Assertions.assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validate_token_with_invalid_signature_returns_false() {
        String tokenSignedWithOtherSecret = Jwts.builder()
                .subject(customer.getEmail())
                .claim("customerId", customer.getId().toString())
                .claim("role", customer.getRole().name())
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode("mW71xIyO2u5Iok7h+4LCxj+8QeQkN6i6FhLo/Jn3vO0=")))
                .compact();

        Assertions.assertFalse(jwtUtil.validateToken(tokenSignedWithOtherSecret));
    }

    @Test
    void validate_token_empty_throws() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> jwtUtil.validateToken(""));
    }

    @Test
    void token_should_be_expired() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", 0L);
        String expiredToken = jwtUtil.generateToken(customer);
        Assertions.assertFalse(jwtUtil.validateToken(expiredToken));
    }

    @Test
    void extract_username_should_get_it() {
        String username = jwtUtil.extractUsername(token);
        Assertions.assertEquals(customer.getEmail(), username);
    }

    @Test
    void extract_username_should_not_work_with_invalid_token() {
        Assertions.assertThrows(Exception.class, () -> jwtUtil.extractUsername("fewfewewoiuffidsfjdvnojq"));
    }

    @Test
    void token_includes_role_and_customer_id() {
        String generatedToken = jwtUtil.generateToken(customer);

        var jwt = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .build()
                .parseSignedClaims(generatedToken);

        Assertions.assertNotNull(jwt.getPayload().getSubject());
        Assertions.assertEquals(customer.getId().toString(), jwt.getPayload().get("customerId", String.class));
        Assertions.assertEquals(customer.getRole().name(), jwt.getPayload().get("role", String.class));
    }

    @Test
    void extract_customer_id_claim_ok() {
        Object customerId = jwtUtil.extractClaim(token, JwtClaim.CUSTOMER_ID);
        Assertions.assertEquals(customer.getId().toString(), customerId);
    }

    @Test
    void extract_role_claim_ok() {
        Object role = jwtUtil.extractClaim(token, JwtClaim.ROLE);
        Assertions.assertEquals(customer.getRole().name(), role);
    }
}

