package com.bytecodes.ms_customers.controller;

import com.bytecodes.ms_customers.response.SuccessfulAuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bytecodes.ms_customers.model.Customer;
import com.bytecodes.ms_customers.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @PostMapping("/register")
    public ResponseEntity<Customer> registerUser(@RequestBody @Valid Customer customer) {
        Customer registered = service.registerCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(registered);
    }

    @PostMapping("/login")
    public ResponseEntity<SuccessfulAuthResponse> loginUser(@RequestBody @Valid Customer customer) {
        SuccessfulAuthResponse response = service.loginCustomer(customer);
        return ResponseEntity.ok(response);
    }
}
