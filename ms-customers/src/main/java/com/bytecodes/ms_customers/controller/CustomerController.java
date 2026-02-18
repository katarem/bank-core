package com.bytecodes.ms_customers.controller;

import com.bytecodes.ms_customers.model.Customer;
import com.bytecodes.ms_customers.model.SafeCustomer;
import com.bytecodes.ms_customers.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/customers")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/me")
    public ResponseEntity<SafeCustomer> getMyProfile(@RequestHeader(value = "Authorization") String token) {
        return ResponseEntity.ok(customerService.getMyProfile(token.replace("Bearer ", "")));
    }
}
