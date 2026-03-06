package com.bytecodes.ms_customers.service;

import com.bytecodes.ms_customers.entity.CustomerEntity;
import com.bytecodes.ms_customers.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final CustomerRepository customerRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        log.debug("Entering UserDetailsService > loadByUsername");

        CustomerEntity entity = customerRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        log.debug("Exiting UserDetailsService > loadByUsername");

        return User.builder()
                .username(entity.getEmail())
                .password(entity.getPassword())
                .roles(entity.getRole().name())
                .build();
    }
}
