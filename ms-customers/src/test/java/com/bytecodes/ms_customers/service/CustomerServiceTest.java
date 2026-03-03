package com.bytecodes.ms_customers.service;

import com.bytecodes.ms_customers.config.TestConfig;
import com.bytecodes.ms_customers.dto.request.CustomerValidationResponse;
import com.bytecodes.ms_customers.dto.request.UpdateProfileRequest;
import com.bytecodes.ms_customers.dto.response.GetCustomerResponse;
import com.bytecodes.ms_customers.dto.response.GetProfileResponse;
import com.bytecodes.ms_customers.dto.response.UpdateProfileResponse;
import com.bytecodes.ms_customers.entity.CustomerEntity;
import com.bytecodes.ms_customers.mapper.CustomerMapper;
import com.bytecodes.ms_customers.model.AuthPrincipal;
import com.bytecodes.ms_customers.model.Customer;
import com.bytecodes.ms_customers.model.CustomerStatus;
import com.bytecodes.ms_customers.repository.CustomerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;
import java.util.UUID;

@SpringJUnitConfig(classes = {CustomerService.class, TestConfig.class})
class CustomerServiceTest {

    @MockitoBean
    private CustomerRepository repository;

    @Autowired
    private CustomerMapper mapper;

    @Autowired
    private CustomerService service;

    @Test
    void get_profile_ok() {
        AuthPrincipal auth = auth("user@email.com");
        CustomerEntity entity = new CustomerEntity();
        entity.setFirstName("user");

        Customer model = new Customer();
        model.setFirstName("user");

        GetProfileResponse response = new GetProfileResponse();
        response.setFirstName("user");

        Mockito.when(repository.findByEmail(auth.getUsername())).thenReturn(Optional.of(entity));

        GetProfileResponse safe = service.getMyProfile(auth);

        Assertions.assertNotNull(safe);
        Assertions.assertEquals("user", safe.getFirstName());
    }

    @Test
    void get_profile_not_found() {
        AuthPrincipal auth = auth("unknown@email.com");
        Mockito.when(repository.findByEmail(auth.getUsername())).thenReturn(Optional.empty());

        Assertions.assertThrows(UsernameNotFoundException.class, () -> service.getMyProfile(auth));
    }

    @Test
    void put_profile_ok() {
        AuthPrincipal auth = auth("user@email.com");
        CustomerEntity entity = new CustomerEntity();
        entity.setLastName("user");

        UpdateProfileRequest updated = new UpdateProfileRequest();
        updated.setFirstName("other");
        updated.setLastName("name");
        updated.setPhone("600000000");
        updated.setAddress("Street 1");

        Customer mapped = new Customer();
        mapped.setFirstName("other");

        UpdateProfileResponse response = new UpdateProfileResponse();
        response.setFirstName("other");

        Mockito.when(repository.findByEmail(auth.getUsername())).thenReturn(Optional.of(entity));
        Mockito.when(repository.save(entity)).thenReturn(entity);

        UpdateProfileResponse safe = service.updateMyProfile(auth, updated);

        Assertions.assertNotNull(safe);
        Assertions.assertEquals("other", safe.getFirstName());
    }

    @Test
    void put_profile_not_found() {
        AuthPrincipal auth = auth("unknown@email.com");
        Mockito.when(repository.findByEmail(auth.getUsername())).thenReturn(Optional.empty());

        Assertions.assertThrows(UsernameNotFoundException.class,
                () -> service.updateMyProfile(auth, new UpdateProfileRequest()));
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

        Mockito.when(repository.findById(customerId)).thenReturn(Optional.of(entity));

        GetCustomerResponse safe = service.getCustomer(customerId);

        Assertions.assertNotNull(safe);
        Assertions.assertEquals(customerId, safe.getId());
        Assertions.assertEquals("12345678A", safe.getDni());
        Assertions.assertEquals("John Doe", safe.getFullName());
        Assertions.assertEquals("john.doe@email.com", safe.getEmail());
        Assertions.assertEquals("ACTIVE", safe.getStatus());
    }

    @Test
    void get_customer_not_found() {
        UUID customerId = UUID.randomUUID();
        Mockito.when(repository.findById(customerId)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = Assertions.assertThrows(
                UsernameNotFoundException.class, () -> service.getCustomer(customerId));

        Assertions.assertEquals("El usuario no existe", exception.getMessage());
    }

    @Test
    void get_customer_null_id() {
        Mockito.when(repository.findById(Mockito.isNull())).thenReturn(Optional.empty());

        UsernameNotFoundException exception = Assertions.assertThrows(
                UsernameNotFoundException.class, () -> service.getCustomer(null));

        Assertions.assertEquals("El usuario no existe", exception.getMessage());
    }

    @Test
    void validate_customer_ok_active() {
        UUID customerId = UUID.randomUUID();
        CustomerEntity entity = new CustomerEntity();
        entity.setId(customerId);
        entity.setStatus(CustomerStatus.ACTIVE);

        Mockito.when(repository.findById(customerId)).thenReturn(Optional.of(entity));

        CustomerValidationResponse response = service.validateCustomer(customerId);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(customerId, response.getCustomerId());
        Assertions.assertTrue(response.isExists());
        Assertions.assertTrue(response.isActive());
    }

    @Test
    void validate_customer_ok_inactive() {
        UUID customerId = UUID.randomUUID();
        CustomerEntity entity = new CustomerEntity();
        entity.setId(customerId);
        entity.setStatus(CustomerStatus.BLOCKED);

        Mockito.when(repository.findById(customerId)).thenReturn(Optional.of(entity));

        CustomerValidationResponse response = service.validateCustomer(customerId);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(customerId, response.getCustomerId());
        Assertions.assertTrue(response.isExists());
        Assertions.assertFalse(response.isActive());
    }

    @Test
    void validate_customer_not_found() {
        UUID customerId = UUID.randomUUID();
        Mockito.when(repository.findById(customerId)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = Assertions.assertThrows(
                UsernameNotFoundException.class, () -> service.validateCustomer(customerId));

        Assertions.assertEquals("El usuario no existe", exception.getMessage());
    }

    @Test
    void validate_customer_null_id() {
        Mockito.when(repository.findById(Mockito.isNull())).thenReturn(Optional.empty());

        UsernameNotFoundException exception = Assertions.assertThrows(
                UsernameNotFoundException.class, () -> service.validateCustomer(null));

        Assertions.assertEquals("El usuario no existe", exception.getMessage());
    }

    private AuthPrincipal auth(String username) {
        return new AuthPrincipal(username, UUID.randomUUID().toString());
    }
}
