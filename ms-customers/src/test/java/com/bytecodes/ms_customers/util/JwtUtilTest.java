package com.bytecodes.ms_customers.util;

import com.bytecodes.ms_customers.model.Customer;
import com.bytecodes.ms_customers.model.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

@SpringJUnitConfig
public class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    String token = "";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "secret", "xPHozB2HdjwZdfUGYXkZKnx1JgF+qwYdhQDrCDpO9U4=");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);

        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setRole(UserRole.CUSTOMER);
        customer.setEmail("email@email.com");
        customer.setPassword("contraseña");

        token = jwtUtil.generateToken(customer);


    }

    @Test
    void create_token_successful() {

        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setRole(UserRole.CUSTOMER);
        customer.setEmail("email@email.com");
        customer.setPassword("contraseña");

        String generatedToken = jwtUtil.generateToken(customer);
        Assertions.assertNotNull(generatedToken);
        Assertions.assertFalse(generatedToken.isEmpty());

    }

    @Test
    void validate_token_successful() {

        boolean isValid = jwtUtil.validateToken(token);

        Assertions.assertTrue(isValid);

    }

    @Test
    void validate_token_invalid() {
        token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIwNmM4ODYzNS01NWYxLTQwODEtYTJlNi0zNTE2NTg5ZDQ4YTFAZW1haWwuY29tIiwiaWF0IjoxNzcxMjA4NDEwLCJleHAiOjE3NzEyOTQ4MTB9._nZPE8FFYftlGrrnavxxZCjfC8eDtKW_Nu44tTotNKs";
        boolean isValid = jwtUtil.validateToken(token);

        Assertions.assertFalse(isValid);


    }

    @Test
    void validate_token_empty() {
        token = "";
        Assertions.assertThrows(Exception.class, () -> jwtUtil.validateToken(""));

    }

    @Test
    void token_should_not_be_expired() {

        boolean isValid = jwtUtil.validateToken(token);

        Assertions.assertTrue(isValid);

    }

    @Test
    void token_should_be_expired() {

        ReflectionTestUtils.setField(jwtUtil, "expiration", 0L);
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setRole(UserRole.CUSTOMER);
        customer.setEmail("email@email.com");
        customer.setPassword("contraseña");

        token = jwtUtil.generateToken(customer);


        // when & then
        boolean isValid = jwtUtil.validateToken(token);
        Assertions.assertFalse(isValid);

    }

    @Test
    void extract_username_should_get_it() {

        String username = jwtUtil.extractUsername(token);

        Assertions.assertEquals("email@email.com", username);

    }

    @Test
    void extract_username_should_not_work() {
        String token = "fewfewewoiuffidsfjdvnojq";

        Assertions.assertThrows(Exception.class, () ->
                jwtUtil.extractUsername(token));

    }

    @Test
    void token_includes_role_and_customer_id() {
        // given
        Customer customer = new Customer();
        customer.setRole(UserRole.ADMIN);
        customer.setEmail("user@email.com");
        customer.setId(UUID.randomUUID());

        // when && then
        String token = jwtUtil.generateToken(customer);

        var jwt = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode("xPHozB2HdjwZdfUGYXkZKnx1JgF+qwYdhQDrCDpO9U4=")))
                .build()
                .parseSignedClaims(token);

        Assertions.assertNotNull(jwt);
        Assertions.assertNotNull(jwt.getPayload());
        Assertions.assertNotNull(jwt.getPayload().getSubject());
        Assertions.assertNotNull(jwt.getPayload().get("customerId", String.class));
        Assertions.assertNotNull(jwt.getPayload().get("role", String.class));

        Assertions.assertEquals(customer.getId().toString(), jwt.getPayload().get("customerId", String.class));
        Assertions.assertEquals(customer.getRole().name(), jwt.getPayload().get("role", String.class));

    }


}
