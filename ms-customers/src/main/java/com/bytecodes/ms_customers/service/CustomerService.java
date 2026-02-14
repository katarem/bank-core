package com.bytecodes.ms_customers.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.bytecodes.ms_customers.entity.CustomerEntity;
import com.bytecodes.ms_customers.mapper.CustomerMapper;
import com.bytecodes.ms_customers.model.Customer;
import com.bytecodes.ms_customers.model.CustomerStatus;
import com.bytecodes.ms_customers.model.UserRole;
import com.bytecodes.ms_customers.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper = CustomerMapper.INSTANCE;

    public Customer registerCustomer (final Customer customer){
        
        CustomerEntity entity = mapper.toEntity(customer);
        
        entity.setStatus(CustomerStatus.ACTIVE);
        entity.setRole(UserRole.CUSTOMER);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        CustomerEntity registered = repository.save(entity);

        return mapper.toModel(registered);
    }

}
