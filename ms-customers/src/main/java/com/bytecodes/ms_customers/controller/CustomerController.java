package com.bytecodes.ms_customers.controller;

import com.bytecodes.ms_customers.dto.CustomerValidation;
import com.bytecodes.ms_customers.dto.response.GetProfileResponse;
import com.bytecodes.ms_customers.dto.response.UpdateProfileResponse;
import com.bytecodes.ms_customers.dto.request.UpdateProfileRequest;
import com.bytecodes.ms_customers.service.CustomerService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/customers")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/me")
    public ResponseEntity<GetProfileResponse> getMyProfile(@RequestHeader(value = "Authorization") String token) {
        return ResponseEntity.ok(customerService.getMyProfile(token.replace("Bearer ", "")));
    }

    @PutMapping("/me")
    public ResponseEntity<UpdateProfileResponse> updateMyProfile(@RequestHeader("Authorization") String token,
                                                                 @RequestBody UpdateProfileRequest updated) {
        return ResponseEntity.ok(customerService.updateMyProfile(token.replace("Bearer ", ""), updated));
    }


    @CrossOrigin(origins = "${app.security.allowed-origin}")
    @GetMapping("/{customerId}/validate")
    public ResponseEntity<CustomerValidation> validateCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(customerService.validateCustomer(customerId));
    }
}
