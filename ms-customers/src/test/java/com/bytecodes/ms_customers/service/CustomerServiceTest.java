package com.bytecodes.ms_customers.service;

import com.bytecodes.ms_customers.dto.response.GetCustomerResponse;
import com.bytecodes.ms_customers.dto.response.GetProfileResponse;
import com.bytecodes.ms_customers.dto.response.UpdateProfileResponse;
import com.bytecodes.ms_customers.entity.CustomerEntity;
import com.bytecodes.ms_customers.mapper.CustomerMapper;
import com.bytecodes.ms_customers.dto.request.UpdateProfileRequest;
import com.bytecodes.ms_customers.model.CustomerStatus;
import com.bytecodes.ms_customers.repository.CustomerRepository;
import com.bytecodes.ms_customers.util.JwtUtil;
import io.jsonwebtoken.JwtException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;
import java.util.UUID;

@SpringJUnitConfig
public class CustomerServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomerRepository repository;

    @Mock
    private CustomerMapper mapper;

    @InjectMocks
    private CustomerService service;

    private final String userToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0YmYzOTUzYy1jYjNmLTQzOGQtOGIyOC0wMTU4NTQ3NzJmODBAZW1haWwuY29tIiwiaWF0IjoxNzcxNDE4NTU0LCJleHAiOjE3NzE1MDQ5NTR9.J7QYCNVfuMinz5CvOj0ZuX_MIJAUyOY1suJgjPw7m6s";

    @Test
    void get_profile_ok(){

        // given
        var databaseEntity = new CustomerEntity();
        databaseEntity.setFirstName("user");

        // when
        Mockito.when(jwtUtil.extractUsername(Mockito.any(String.class)))
                .thenReturn("user");
        Mockito.when(repository.findByEmail(Mockito.any(String.class)))
                .thenReturn(Optional.of(databaseEntity));

        // then
        GetProfileResponse safe = service.getMyProfile(userToken);

        Assertions.assertNotNull(safe);
        Assertions.assertEquals("user", safe.getFirstName());

    }

    @Test
    void get_profile_not_found(){

        // when
        Mockito.when(repository.findByEmail(Mockito.any(String.class)))
                .thenReturn(Optional.empty());

        // then
        Assertions.assertThrows(UsernameNotFoundException.class, () ->
                service.getMyProfile(userToken));

    }

    @Test
    void get_profile_token_not_provided(){

        // when
        Mockito.when(jwtUtil.extractUsername(Mockito.any(String.class)))
                .thenThrow(JwtException.class);

        // then
        Assertions.assertThrows(JwtException.class, () ->
                service.getMyProfile(""));

    }

    @Test
    void put_profile_ok(){

        // given
        var databaseEntity = new CustomerEntity();
        databaseEntity.setLastName("user");

        var newUser = new UpdateProfileRequest();
        newUser.setFirstName("other");

        // when
        Mockito.when(jwtUtil.extractUsername(Mockito.any(String.class)))
                        .thenReturn("user");
        Mockito.when(repository.save(databaseEntity))
                        .thenReturn(databaseEntity);
        Mockito.when(repository.findByEmail(Mockito.any(String.class)))
                .thenReturn(Optional.of(databaseEntity));

        // then
        UpdateProfileResponse safe = service.updateMyProfile(userToken, newUser);

        Assertions.assertNotNull(safe);
        Assertions.assertEquals("other", safe.getFirstName());

    }

    @Test
    void put_profile_not_found(){

        // when
        Mockito.when(repository.findByEmail(Mockito.any(String.class)))
                .thenReturn(Optional.empty());

        // then
        Assertions.assertThrows(UsernameNotFoundException.class, () ->
                service.updateMyProfile(userToken, new UpdateProfileRequest()));

    }

    @Test
    void put_profile_token_not_provided(){

        // when
        Mockito.when(jwtUtil.extractUsername(Mockito.any(String.class)))
               .thenThrow(JwtException.class);

        // then
        Assertions.assertThrows(JwtException.class, () ->
               service.updateMyProfile("", new UpdateProfileRequest()));

    }

    @Test
    void get_customer_ok() {

        UUID customerId = UUID.randomUUID();
        CustomerEntity entity = new CustomerEntity();
        entity.setId(customerId);
        entity.setDni("12345678A");
        entity.setFirstName("John");
        entity.setLastName("Doe");
        entity.setEmail("john.doe@email.com");
        entity.setStatus(CustomerStatus.ACTIVE);

        Mockito.when(repository.findById(customerId))
                .thenReturn(Optional.of(entity));

        GetCustomerResponse response = service.getCustomer(customerId);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(customerId, response.getId());
        Assertions.assertEquals("12345678A", response.getDni());
        Assertions.assertEquals("John Doe", response.getFullName());
        Assertions.assertEquals("john.doe@email.com", response.getEmail());
        Assertions.assertEquals("ACTIVE", response.getStatus());
    }

    @Test
    void get_customer_not_found() {

        UUID customerId = UUID.randomUUID();
        Mockito.when(repository.findById(customerId))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = Assertions.assertThrows(
                UsernameNotFoundException.class,
                () -> service.getCustomer(customerId)
        );

        Assertions.assertEquals("El usuario no existe", exception.getMessage());
    }

    @Test
    void get_customer_null_id() {

        Mockito.when(repository.findById(Mockito.isNull()))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = Assertions.assertThrows(
                UsernameNotFoundException.class,
                () -> service.getCustomer(null)
        );

        Assertions.assertEquals("El usuario no existe", exception.getMessage());
    }



}
