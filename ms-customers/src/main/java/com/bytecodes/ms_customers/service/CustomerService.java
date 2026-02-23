package com.bytecodes.ms_customers.service;

import java.util.UUID;

import com.bytecodes.ms_customers.dto.request.UpdateProfileRequest;
import com.bytecodes.ms_customers.dto.response.GetProfileResponse;
import com.bytecodes.ms_customers.dto.response.UpdateProfileResponse;
import com.bytecodes.ms_customers.model.*;
import com.bytecodes.ms_customers.util.JwtUtil;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.bytecodes.ms_customers.dto.CustomerValidation;
import com.bytecodes.ms_customers.entity.CustomerEntity;
import com.bytecodes.ms_customers.mapper.CustomerMapper;
import com.bytecodes.ms_customers.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper = CustomerMapper.INSTANCE;
    private final JwtUtil jwtUtil;

    public GetProfileResponse getMyProfile(final String token) {
        String username = jwtUtil.extractUsername(token);

        Customer model = mapper.toModel(repository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario " + username + " no encontrado")));

        return mapper.toGetProfileResponse(model);
    }

    public UpdateProfileResponse updateMyProfile(final String token, final UpdateProfileRequest updated) {
        String username = jwtUtil.extractUsername(token);

        CustomerEntity entity = repository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario " + username + " no encontrado"));

        entity.setFirstName(updated.getFirstName());
        entity.setLastName(updated.getLastName());
        entity.setPhone(updated.getPhone());
        entity.setAddress(updated.getAddress());

        Customer model = mapper.toModel(repository.save(entity));

        return mapper.toUpdateProfileResponse(model);
    }


    public CustomerValidation validateCustomer(UUID customerId) {
        CustomerEntity customer = repository.findById(customerId)
                .orElseThrow(() -> new UsernameNotFoundException("El usuario no existe"));

        boolean isActive = customer.getStatus() == CustomerStatus.ACTIVE;

        return new CustomerValidation(customerId, true, isActive);
    }
}
