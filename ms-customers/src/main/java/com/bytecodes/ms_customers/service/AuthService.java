package com.bytecodes.ms_customers.service;

import com.bytecodes.ms_customers.dto.request.LoginRequest;
import com.bytecodes.ms_customers.dto.request.RegisterRequest;
import com.bytecodes.ms_customers.dto.response.RegisterResponse;
import com.bytecodes.ms_customers.entity.CustomerEntity;
import com.bytecodes.ms_customers.mapper.CustomerMapper;
import com.bytecodes.ms_customers.model.Customer;
import com.bytecodes.ms_customers.model.CustomerStatus;
import com.bytecodes.ms_customers.model.UserRole;
import com.bytecodes.ms_customers.repository.CustomerRepository;
import com.bytecodes.ms_customers.dto.response.LoginResponse;
import com.bytecodes.ms_customers.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public RegisterResponse registerCustomer (final RegisterRequest customer){

        log.debug("Entering AuthService > registerCustomer");

        Customer model = mapper.toModel(customer);

        model.setStatus(CustomerStatus.ACTIVE);
        model.setRole(UserRole.CUSTOMER);
        model.setCreatedAt(Instant.now());
        model.setUpdatedAt(Instant.now());

        model.setPassword(encoder.encode(model.getPassword()));

        Customer registered = mapper.toModel(repository.save(mapper.toEntity(model)));

        log.debug("Exiting AuthService > registerCustomer");
        log.info("Registered customer {}", registered.getId());

        return mapper.toRegisterResponse(registered);
    }

    public LoginResponse loginCustomer(final LoginRequest request) {

        log.debug("Entering AuthService > loginCustomer");

        CustomerEntity databaseEntity = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario " + request.getEmail() + " no encontrado"));

        authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        Customer databaseCustomer = mapper.toModel(databaseEntity);

        String token = jwtUtil.generateToken(databaseCustomer);

        log.debug("Exiting AuthService > loginCustomer");
        log.info("Logged in customer {}", databaseCustomer.getId());

        return LoginResponse.builder()
                .token(token)
                .expiresIn(jwtUtil.getExpiration())
                .tokenType("Bearer")
                .customerId(databaseCustomer.getId().toString())
                .build();
    }
}
