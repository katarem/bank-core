package com.bytecodes.ms_customers.service;

import java.time.Instant;

import com.bytecodes.ms_customers.model.SafeCustomer;
import com.bytecodes.ms_customers.response.SuccessfulAuthResponse;
import com.bytecodes.ms_customers.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public Customer registerCustomer (final Customer customer){
        
        CustomerEntity entity = mapper.toEntity(customer);
        
        entity.setStatus(CustomerStatus.ACTIVE);
        entity.setRole(UserRole.CUSTOMER);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        entity.setPassword(encoder.encode(entity.getPassword()));

        CustomerEntity registered = repository.save(entity);

        return mapper.toModel(registered);
    }

    public SuccessfulAuthResponse loginCustomer(final Customer customer) {

        CustomerEntity databaseCustomer = repository.findByEmail(customer.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario " + customer.getEmail() + " no encontrado"));

        var authenticated = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(customer.getEmail(), customer.getPassword()));

        String token = jwtUtil.generateToken(authenticated);

        return SuccessfulAuthResponse.builder()
                .token(token)
                .expiresIn(jwtUtil.getExpiration())
                .tokenType("Bearer")
                .customerId(databaseCustomer.getId().toString())
                .build();
    }

    public SafeCustomer getMyProfile(final String token) {
        String username = jwtUtil.extractUsername(token);

        CustomerEntity entity = repository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario " + username + " no encontrado"));

        return mapper.toSafeModel(entity);
    }
}
