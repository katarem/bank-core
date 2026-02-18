package com.bytecodes.ms_customers.service;

import java.time.Instant;

import com.bytecodes.ms_customers.model.*;
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
import com.bytecodes.ms_customers.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper = CustomerMapper.INSTANCE;
    private final JwtUtil jwtUtil;

    public SafeCustomer getMyProfile(final String token) {
        String username = jwtUtil.extractUsername(token);

        CustomerEntity entity = repository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario " + username + " no encontrado"));

        return mapper.toSafeModel(entity);
    }

    public SafeCustomer updateMyProfile(final String token, final SafeUpdateCustomer updated) {
        String username = jwtUtil.extractUsername(token);

        CustomerEntity entity = repository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario " + username + " no encontrado"));

        entity.setFirstName(updated.getFirstName());
        entity.setLastName(updated.getLastName());
        entity.setPhone(updated.getPhone());
        entity.setAddress(updated.getAddress());

        CustomerEntity updatedEntity = repository.save(entity);

        return mapper.toSafeModel(updatedEntity);
    }
}
