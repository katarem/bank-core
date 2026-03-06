package com.bytecodes.ms_customers.service;

import java.util.UUID;

import com.bytecodes.ms_customers.constant.ErrorConstants;
import com.bytecodes.ms_customers.dto.request.UpdateProfileRequest;
import com.bytecodes.ms_customers.dto.response.GetCustomerResponse;
import com.bytecodes.ms_customers.dto.response.GetProfileResponse;
import com.bytecodes.ms_customers.dto.response.UpdateProfileResponse;
import com.bytecodes.ms_customers.model.*;
import com.bytecodes.ms_customers.util.JwtUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.bytecodes.ms_customers.dto.request.CustomerValidationResponse;
import com.bytecodes.ms_customers.entity.CustomerEntity;
import com.bytecodes.ms_customers.mapper.CustomerMapper;
import com.bytecodes.ms_customers.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    public GetProfileResponse getMyProfile(final AuthPrincipal auth) {

        log.debug("Entering CustomerService > getMyProfile");
        Customer model = mapper.toModel(repository.findByEmail(auth.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(ErrorConstants.userNotFound(auth.getUsername()))));
        log.info("Own profile checked {}", model.getId());
        log.debug("Exiting CustomerService > getMyProfile");

        return mapper.toGetProfileResponse(model);
    }

    public UpdateProfileResponse updateMyProfile(final AuthPrincipal auth, final UpdateProfileRequest updated) {

        log.debug("Entering CustomerService > updateMyProfile");

        CustomerEntity entity = repository.findByEmail(auth.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(ErrorConstants.userNotFound(auth.getUsername())));

        entity.setFirstName(updated.getFirstName());
        entity.setLastName(updated.getLastName());
        entity.setPhone(updated.getPhone());
        entity.setAddress(updated.getAddress());

        Customer model = mapper.toModel(repository.save(entity));

        log.info("Updated profile {}", model.getId());
        log.debug("Exiting CustomerService > updateMyProfile");

        return mapper.toUpdateProfileResponse(model);
    }


    public CustomerValidationResponse validateCustomer(final UUID customerId) {
        log.debug("Entering CustomerService > validateCustomer");
        CustomerEntity customer = repository.findById(customerId)
                .orElseThrow(() -> new UsernameNotFoundException(ErrorConstants.userNotFound(customerId.toString())));

        boolean isActive = customer.getStatus() == CustomerStatus.ACTIVE;

        log.info("Validated user {}", customerId);
        log.debug("Exiting CustomerService > validateCustomer");

        return new CustomerValidationResponse(customerId, true, isActive);
    }

    public GetCustomerResponse getCustomer(final UUID customerId) {

        log.debug("Entering CustomerService > getCustomer");

        CustomerEntity entity = repository.findById(customerId)
                .orElseThrow(() -> new UsernameNotFoundException(ErrorConstants.userNotFound(customerId.toString())));

        Customer customer = mapper.toModel(entity);

        log.info("Customer obtained {}", customerId);
        log.debug("Exiting CustomerService > getCustomer");

        return mapper.toGetCustomerResponse(customer);
    }
}
