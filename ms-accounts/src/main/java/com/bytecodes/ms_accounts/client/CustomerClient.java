package com.bytecodes.ms_accounts.client;

import com.bytecodes.ms_accounts.config.FeignConfig;
import com.bytecodes.ms_accounts.dto.response.CustomerResponse;
import com.bytecodes.ms_accounts.dto.response.CustomerValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "ms-customers",
             url = "${customers.service.url}",
             configuration = FeignConfig.class)
public interface CustomerClient {

    @GetMapping("/api/customers/{customerId}/validate")
    CustomerValidationResponse validateCustomer(@PathVariable UUID customerId);

    @GetMapping("/api/customers/{customerId}")
    CustomerResponse getCustomer(@PathVariable UUID customerId);

}
