package com.bytecodes.ms_customers.service;

import com.bytecodes.ms_customers.entity.CustomerEntity;
import com.bytecodes.ms_customers.model.Customer;
import com.bytecodes.ms_customers.repository.CustomerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.UUID;

@SpringJUnitConfig
public class CustomerServiceTest {

    @InjectMocks
    private CustomerService service;

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


}
