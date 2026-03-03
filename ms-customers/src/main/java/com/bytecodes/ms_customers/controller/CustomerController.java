package com.bytecodes.ms_customers.controller;

import com.bytecodes.ms_customers.dto.request.CustomerValidationResponse;
import com.bytecodes.ms_customers.dto.response.GetCustomerResponse;
import com.bytecodes.ms_customers.dto.response.GetProfileResponse;
import com.bytecodes.ms_customers.dto.response.UpdateProfileResponse;
import com.bytecodes.ms_customers.dto.request.UpdateProfileRequest;
import com.bytecodes.ms_customers.model.AuthPrincipal;
import com.bytecodes.ms_customers.service.CustomerService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/customers")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/me")
    public ResponseEntity<GetProfileResponse> getMyProfile(@AuthenticationPrincipal AuthPrincipal auth) {
        return ResponseEntity.ok(customerService.getMyProfile(auth));
    }

    @PutMapping("/me")
    public ResponseEntity<UpdateProfileResponse> updateMyProfile(@AuthenticationPrincipal AuthPrincipal auth,
                                                                 @RequestBody UpdateProfileRequest updated) {
        return ResponseEntity.ok(customerService.updateMyProfile(auth, updated));
    }


    @CrossOrigin(origins = "${app.security.allowed-origin}")
    @GetMapping("/{customerId}/validate")
    public ResponseEntity<CustomerValidationResponse> validateCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(customerService.validateCustomer(customerId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SERVICE')")
    @GetMapping("/{customerId}")
    public ResponseEntity<GetCustomerResponse> getCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(customerService.getCustomer(customerId));
    }
}
