package com.bytecodes.ms_customers.service;

import com.bytecodes.ms_customers.entity.CustomerEntity;
import com.bytecodes.ms_customers.model.Customer;
import com.bytecodes.ms_customers.model.UserRole;
import com.bytecodes.ms_customers.repository.CustomerRepository;
import com.bytecodes.ms_customers.response.SuccessfulAuthResponse;
import com.bytecodes.ms_customers.util.JwtUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;
import java.util.UUID;

@SpringJUnitConfig
public class AuthServiceTest {

    @InjectMocks
    private AuthService service;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private AuthenticationManager manager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomerRepository repository;

    @Test
    void create_customer_ok(){

        // given
        Customer customer = new Customer();
        customer.setDni("12345678L");
        customer.setEmail("customer@email.com");
        customer.setPassword("Password123");

        CustomerEntity databaseCustomer = new CustomerEntity();
        databaseCustomer.setId(UUID.randomUUID());
        databaseCustomer.setDni(customer.getDni());
        databaseCustomer.setEmail(customer.getEmail());
        databaseCustomer.setPassword(customer.getPassword());

        // when
        Mockito.when(repository.save(Mockito.any(CustomerEntity.class)))
                .thenReturn(databaseCustomer);

        // then
        Customer registered = service.registerCustomer(customer);

        Assertions.assertNotNull(registered);
        Assertions.assertNotNull(registered.getId());
        Assertions.assertEquals(customer.getDni(), registered.getDni());
        Assertions.assertEquals(customer.getEmail(), registered.getEmail());
        Assertions.assertEquals(customer.getPassword(), registered.getPassword());

    }

    @Test
    void create_customer_conflict(){
        // given
        Customer customer = new Customer();
        customer.setDni("12345678L");
        customer.setEmail("customer@email.com");
        customer.setPassword("Password123");

        // when
        Mockito.when(repository.save(Mockito.any(CustomerEntity.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "conflict violation",
                        new RuntimeException("duplicate key value violates unique constraint (dni)")
                ));

        // then
        var exception = Assertions.assertThrows(DataIntegrityViolationException.class, () ->
                service.registerCustomer(customer));

        Assertions.assertEquals("conflict violation", exception.getMessage());
        Assertions.assertNotNull(exception.getMostSpecificCause());
        Assertions.assertEquals("duplicate key value violates unique constraint (dni)",exception.getMostSpecificCause().getMessage());

    }

    @Test
    void login_customer_ok() {

        //given
        Customer customer = new Customer();
        customer.setEmail("customer@email.com");
        customer.setPassword("StrongPassword123");

        CustomerEntity entity = new CustomerEntity();
        entity.setId(UUID.randomUUID());

        //when
        Mockito.when(repository.findByEmail(customer.getEmail()))
                .thenReturn(Optional.of(entity));

        Mockito.when(manager.authenticate(Mockito.any(Authentication.class)))
                        .thenReturn(new UsernamePasswordAuthenticationToken(entity.getEmail(), entity.getPassword()));

        Mockito.when(jwtUtil.getExpiration())
                        .thenReturn(86400000L);

        Mockito.when(jwtUtil.generateToken(Mockito.any(Authentication.class)))
                .thenReturn("");

        //then
        SuccessfulAuthResponse response = service.loginCustomer(customer);
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getCustomerId());
        Assertions.assertEquals(entity.getId().toString(), response.getCustomerId());
        Assertions.assertNotNull(response.getToken());
        Assertions.assertTrue(response.getExpiresIn() > 0L);

    }

    @Test
    void login_customer_bad_credentials() {

        //given
        Customer customer = new Customer();
        customer.setEmail("customer@email.com");
        customer.setPassword("ImSureThisIsTheOne");

        CustomerEntity customerEntity = new CustomerEntity();

        //when
        Mockito.when(repository.findByEmail(customer.getEmail()))
                .thenReturn(Optional.of(customerEntity));

        Mockito.when(manager.authenticate(Mockito.any(Authentication.class)))
                .thenThrow(BadCredentialsException.class);

        //then
        Assertions.assertThrows(BadCredentialsException.class, () ->
                service.loginCustomer(customer));

    }

    @Test
    void login_customer_not_exists() {
        //given
        Customer customer = new Customer();
        customer.setEmail("juanitoperez@gmail.com");
        customer.setPassword("SuperPassword23");

        //when
        Mockito.when(repository.findByEmail(customer.getEmail()))
                .thenReturn(Optional.empty());

        //then
        UsernameNotFoundException ex = Assertions.assertThrows(UsernameNotFoundException.class, () ->
                service.loginCustomer(customer));

        Assertions.assertNotNull(ex.getMessage());
        Assertions.assertEquals("Usuario " + "juanitoperez@gmail.com" + " no encontrado", ex.getMessage());

    }



}
