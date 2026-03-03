package com.bytecodes.ms_customers.service;

import java.util.UUID;

import com.bytecodes.ms_customers.dto.request.UpdateProfileRequest;
import com.bytecodes.ms_customers.dto.response.GetCustomerResponse;
import com.bytecodes.ms_customers.dto.response.GetProfileResponse;
import com.bytecodes.ms_customers.dto.response.UpdateProfileResponse;
import com.bytecodes.ms_customers.model.*;
import com.bytecodes.ms_customers.util.JwtUtil;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.bytecodes.ms_customers.dto.request.CustomerValidationResponse;
import com.bytecodes.ms_customers.entity.CustomerEntity;
import com.bytecodes.ms_customers.mapper.CustomerMapper;
import com.bytecodes.ms_customers.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    public GetProfileResponse getMyProfile(final AuthPrincipal auth) {
        Customer model = mapper.toModel(repository.findByEmail(auth.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario " + auth.getUsername() + " no encontrado")));

        return mapper.toGetProfileResponse(model);
    }

    public UpdateProfileResponse updateMyProfile(final AuthPrincipal auth, final UpdateProfileRequest updated) {
        CustomerEntity entity = repository.findByEmail(auth.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario " + auth.getUsername() + " no encontrado"));

        entity.setFirstName(updated.getFirstName());
        entity.setLastName(updated.getLastName());
        entity.setPhone(updated.getPhone());
        entity.setAddress(updated.getAddress());

        Customer model = mapper.toModel(repository.save(entity));

        return mapper.toUpdateProfileResponse(model);
    }


    public CustomerValidationResponse validateCustomer(final UUID customerId) {
        CustomerEntity customer = repository.findById(customerId)
                .orElseThrow(() -> new UsernameNotFoundException("El usuario no existe"));

        boolean isActive = customer.getStatus() == CustomerStatus.ACTIVE;

        return new CustomerValidationResponse(customerId, true, isActive);
    }

    public GetCustomerResponse getCustomer(final UUID customerId) {

        CustomerEntity entity = repository.findById(customerId)
                .orElseThrow(() -> new UsernameNotFoundException("El usuario no existe"));

        Customer customer = mapper.toModel(entity);

        return mapper.toGetCustomerResponse(customer);
    }
}
