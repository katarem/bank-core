package com.bytecodes.ms_customers.controller;

import com.bytecodes.ms_customers.model.SafeCustomer;
import com.bytecodes.ms_customers.model.SafeUpdateCustomer;
import com.bytecodes.ms_customers.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/customers")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/me")
    public ResponseEntity<SafeCustomer> getMyProfile(@RequestHeader(value = "Authorization") String token) {
        return ResponseEntity.ok(customerService.getMyProfile(token.replace("Bearer ", "")));
    }

    @PutMapping("/me")
    public ResponseEntity<SafeCustomer> updateMyProfile(@RequestHeader("Authorization") String token,
                                                        @RequestBody SafeUpdateCustomer updated) {
        return ResponseEntity.ok(customerService.updateMyProfile(token.replace("Bearer ", ""), updated));
    }
}
