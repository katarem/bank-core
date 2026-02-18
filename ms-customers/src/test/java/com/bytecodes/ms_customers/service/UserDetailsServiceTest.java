package com.bytecodes.ms_customers.service;

import com.bytecodes.ms_customers.entity.CustomerEntity;
import com.bytecodes.ms_customers.model.Customer;
import com.bytecodes.ms_customers.model.UserRole;
import com.bytecodes.ms_customers.repository.CustomerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;

@SpringJUnitConfig
public class UserDetailsServiceTest {

    @Mock
    private CustomerRepository repository;

    @InjectMocks
    private UserDetailsServiceImpl service;

    @Test
    void load_user_by_username_returns_user() {

        //given

        Customer customer = new Customer();
        customer.setEmail("customer@email.com");
        customer.setPassword("MyPassword123!");

        CustomerEntity foundUser = new CustomerEntity();
        foundUser.setEmail(customer.getEmail());
        foundUser.setPassword(customer.getPassword());
        foundUser.setRole(UserRole.CUSTOMER);

        //when
        Mockito.when(repository.findByEmail(customer.getEmail()))
                .thenReturn(Optional.of(foundUser));

        //then
        UserDetails loadedUser = service.loadUserByUsername("customer@email.com");

        Assertions.assertNotNull(loadedUser);

    }

    @Test
    void load_user_by_username_user_not_found() {

        //given

        Customer customer = new Customer();
        customer.setEmail("customer@email.com");
        customer.setPassword("MyPassword123!");

        //when
        Mockito.when(repository.findByEmail(customer.getEmail()))
                .thenReturn(Optional.empty());

        //then
        Assertions.assertThrows(UsernameNotFoundException.class, () ->
                service.loadUserByUsername("customer@email.com"));

    }


}
