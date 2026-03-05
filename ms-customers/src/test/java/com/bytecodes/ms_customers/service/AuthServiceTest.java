package com.bytecodes.ms_customers.service;

import com.bytecodes.ms_customers.config.TestConfig;
import com.bytecodes.ms_customers.dto.request.LoginRequest;
import com.bytecodes.ms_customers.dto.request.RegisterRequest;
import com.bytecodes.ms_customers.dto.response.LoginResponse;
import com.bytecodes.ms_customers.dto.response.RegisterResponse;
import com.bytecodes.ms_customers.entity.CustomerEntity;
import com.bytecodes.ms_customers.mapper.CustomerMapper;
import com.bytecodes.ms_customers.model.Customer;
import com.bytecodes.ms_customers.repository.CustomerRepository;
import com.bytecodes.ms_customers.util.JwtUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;
import java.util.UUID;

@SpringJUnitConfig(
        classes = {
                AuthService.class,
                TestConfig.class
        }
)
class AuthServiceTest {

    @Autowired
    private AuthService service;

    @Autowired
    private CustomerMapper mapper;

    @MockitoBean
    private PasswordEncoder encoder;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomerRepository repository;



    @Test
    void create_customer_ok() {
        RegisterRequest request = new RegisterRequest();
        request.setDni("12345678L");
        request.setEmail("customer@email.com");
        request.setPassword("Password123");

        Customer mappedRequest = new Customer();
        mappedRequest.setDni(request.getDni());
        mappedRequest.setEmail(request.getEmail());
        mappedRequest.setPassword(request.getPassword());

        CustomerEntity savedEntity = new CustomerEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setDni(request.getDni());
        savedEntity.setEmail(request.getEmail());
        savedEntity.setPassword("encoded-password");

        Customer savedModel = new Customer();
        savedModel.setId(savedEntity.getId());
        savedModel.setDni(savedEntity.getDni());
        savedModel.setEmail(savedEntity.getEmail());

        RegisterResponse response = new RegisterResponse();
        response.setId(savedEntity.getId());
        response.setDni(savedEntity.getDni());
        response.setEmail(savedEntity.getEmail());

        Mockito.when(encoder.encode("Password123")).thenReturn("encoded-password");
        Mockito.when(repository.save(Mockito.any(CustomerEntity.class))).thenReturn(savedEntity);

        RegisterResponse registered = service.registerCustomer(request);

        Assertions.assertNotNull(registered);
        Assertions.assertNotNull(registered.getId());
        Assertions.assertEquals(request.getDni(), registered.getDni());
        Assertions.assertEquals(request.getEmail(), registered.getEmail());
    }

    @Test
    void create_customer_conflict() {
        RegisterRequest request = new RegisterRequest();
        request.setDni("12345678L");
        request.setEmail("customer@email.com");
        request.setPassword("Password123");

        Customer mappedRequest = new Customer();
        mappedRequest.setPassword(request.getPassword());

        Mockito.when(encoder.encode("Password123")).thenReturn("encoded-password");
        Mockito.when(repository.save(Mockito.any(CustomerEntity.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "conflict violation",
                        new RuntimeException("duplicate key value violates unique constraint (dni)")
                ));

        var exception = Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> service.registerCustomer(request));

        Assertions.assertEquals("conflict violation", exception.getMessage());
        Assertions.assertNotNull(exception.getMostSpecificCause());
        Assertions.assertEquals("duplicate key value violates unique constraint (dni)",
                exception.getMostSpecificCause().getMessage());
    }

    @Test
    void login_customer_ok() {
        LoginRequest request = new LoginRequest();
        request.setEmail("customer@email.com");
        request.setPassword("StrongPassword123");

        CustomerEntity entity = new CustomerEntity();
        entity.setId(UUID.randomUUID());
        entity.setEmail(request.getEmail());

        Customer databaseCustomer = new Customer();
        databaseCustomer.setId(entity.getId());
        databaseCustomer.setEmail(entity.getEmail());

        Mockito.when(repository.findByEmail(request.getEmail())).thenReturn(Optional.of(entity));
        Mockito.when(authenticationManager.authenticate(Mockito.any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(entity.getEmail(), entity.getPassword()));
        Mockito.when(jwtUtil.getExpiration()).thenReturn(86400000L);
        Mockito.when(jwtUtil.generateToken(databaseCustomer)).thenReturn("token");

        LoginResponse response = service.loginCustomer(request);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(entity.getId().toString(), response.getCustomerId());
        Assertions.assertEquals("token", response.getToken());
        Assertions.assertEquals("Bearer", response.getTokenType());
        Assertions.assertTrue(response.getExpiresIn() > 0L);
    }

    @Test
    void login_customer_bad_credentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("customer@email.com");
        request.setPassword("ImSureThisIsTheOne");

        CustomerEntity customerEntity = new CustomerEntity();
        Mockito.when(repository.findByEmail(request.getEmail())).thenReturn(Optional.of(customerEntity));
        Mockito.when(authenticationManager.authenticate(Mockito.any())).thenThrow(BadCredentialsException.class);

        Assertions.assertThrows(BadCredentialsException.class, () -> service.loginCustomer(request));
    }

    @Test
    void login_customer_not_exists() {
        LoginRequest request = new LoginRequest();
        request.setEmail("juanitoperez@gmail.com");
        request.setPassword("SuperPassword23");

        Mockito.when(repository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        UsernameNotFoundException ex = Assertions.assertThrows(UsernameNotFoundException.class,
                () -> service.loginCustomer(request));

        Assertions.assertEquals("Usuario juanitoperez@gmail.com no encontrado", ex.getMessage());
    }
}

