package com.bytecodes.ms_customers.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return ResponseEntity.ok(registered);
    }

}
