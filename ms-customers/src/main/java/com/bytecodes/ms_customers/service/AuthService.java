package com.bytecodes.ms_customers.service;

import com.bytecodes.ms_customers.entity.CustomerEntity;
import com.bytecodes.ms_customers.mapper.CustomerMapper;
import com.bytecodes.ms_customers.model.Customer;
import com.bytecodes.ms_customers.model.CustomerStatus;
import com.bytecodes.ms_customers.model.UserRole;
import com.bytecodes.ms_customers.repository.CustomerRepository;
import com.bytecodes.ms_customers.response.SuccessfulAuthResponse;
import com.bytecodes.ms_customers.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper = CustomerMapper.INSTANCE;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        CustomerEntity entity = repository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        return User.builder()
                .username(entity.getEmail())
                .password(entity.getPassword())
                .roles(entity.getRole().name())
                .build();
    }

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
}
