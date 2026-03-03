package com.bytecodes.ms_customers.controller;

import com.bytecodes.ms_customers.dto.request.CustomerValidationResponse;
import com.bytecodes.ms_customers.dto.response.GetCustomerResponse;
import com.bytecodes.ms_customers.dto.response.GetProfileResponse;
import com.bytecodes.ms_customers.dto.response.UpdateProfileResponse;
import com.bytecodes.ms_customers.handler.CustomerExceptionHandler;
import com.bytecodes.ms_customers.model.AuthPrincipal;
import com.bytecodes.ms_customers.dto.request.UpdateProfileRequest;
import com.bytecodes.ms_customers.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = CustomerController.class, properties = {
        "jwt.secret=bocnbRHda/WxWwAhMhCoxBmfK6mLWn/4o2r7STfN0M4=",
        "jwt.expiration=86400000",
})
@AutoConfigureMockMvc(addFilters = false)
@Import(CustomerExceptionHandler.class)
public class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void get_me_ok() throws Exception {

        // given
        var safeUser = new GetProfileResponse();
        safeUser.setFirstName("user");

        //when
        Mockito.when(service.getMyProfile(Mockito.nullable(AuthPrincipal.class)))
                .thenReturn(safeUser);

        //then
        mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/api/customers/me")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("user"));

    }

    @Test
    void get_me_user_not_found() throws Exception {

        // given
        var safeUser = new GetProfileResponse();
        safeUser.setFirstName("user");

        //when
        Mockito.when(service.getMyProfile(Mockito.nullable(AuthPrincipal.class)))
                        .thenThrow(new UsernameNotFoundException(""));

        //then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/customers/me")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));

    }

    @Test
    void get_me_without_authprincipal_still_ok() throws Exception {

        // given
        var safeUser = new GetProfileResponse();
        safeUser.setFirstName("user");

        Mockito.when(service.getMyProfile(Mockito.nullable(AuthPrincipal.class)))
                .thenReturn(safeUser);

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/customers/me")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("user"));

    }

    @Test
    void put_me_ok() throws Exception {
        // given
        var safeUser = new GetProfileResponse();
        safeUser.setFirstName("user");

        //when
        UpdateProfileResponse updated = new UpdateProfileResponse();
        updated.setFirstName("user");
        Mockito.when(service.updateMyProfile(Mockito.nullable(AuthPrincipal.class), Mockito.any(UpdateProfileRequest.class)))
                .thenReturn(updated);

        //then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .put("/api/customers/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(safeUser))
                )
                .andExpect(status().isOk());
    }

    @Test
    void put_me_not_found() throws Exception {
        // given
        var safeUser = new GetProfileResponse();
        safeUser.setFirstName("user");

        //when
        Mockito.when(service.updateMyProfile(Mockito.nullable(AuthPrincipal.class), Mockito.any(UpdateProfileRequest.class)))
                .thenThrow(new UsernameNotFoundException(""));

        //then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .put("/api/customers/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(safeUser))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));

    }

    @Test
    void put_me_without_authprincipal_still_ok() throws Exception {

        // given
        var safeUser = new GetProfileResponse();
        safeUser.setFirstName("user");

        UpdateProfileResponse updated = new UpdateProfileResponse();
        updated.setFirstName("user");
        Mockito.when(service.updateMyProfile(Mockito.nullable(AuthPrincipal.class), Mockito.any(UpdateProfileRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .put("/api/customers/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(safeUser))
                )
                .andExpect(status().isOk());
    }

    @Test
    void get_validate_customer_exists_and_active() throws Exception {

        UUID customerId = UUID.randomUUID();
                                CustomerValidationResponse validation = new CustomerValidationResponse(customerId, true, true);

        Mockito.when(service.validateCustomer(customerId))
                .thenReturn(validation);

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/customers/{customerId}/validate", customerId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void get_validate_customer_exists_but_inactive() throws Exception {

        UUID customerId = UUID.randomUUID();
                CustomerValidationResponse validation = new CustomerValidationResponse(customerId, true, false);

        Mockito.when(service.validateCustomer(customerId))
                .thenReturn(validation);

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/customers/{customerId}/validate", customerId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void get_validate_customer_not_found() throws Exception {

        UUID customerId = UUID.randomUUID();
        Mockito.when(service.validateCustomer(customerId))
                .thenThrow(new UsernameNotFoundException("El usuario no existe"));

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/customers/{customerId}/validate", customerId)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("El usuario no existe"));
    }

    @Test
    void get_validate_customer_invalid_uuid_format() throws Exception {

        String invalidUuid = "invalid-uuid-format";

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/customers/{customerId}/validate", invalidUuid)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UUID_FORMAT"))
                .andExpect(jsonPath("$.message").value("Formato de ID no válido. Introduce una ID válida"));
    }

    @Test
    void get_validate_customer_missing_id() throws Exception {

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/customers/validate")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void get_customer_ok() throws Exception {

        UUID customerId = UUID.randomUUID();
        GetCustomerResponse customer = new GetCustomerResponse();
        customer.setId(customerId);
        customer.setDni("12345678A");
        customer.setFullName("User Lastname");
        customer.setEmail("user@email.com");
        customer.setStatus("ACTIVE");

        Mockito.when(service.getCustomer(customerId))
                .thenReturn(customer);

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/customers/{customerId}", customerId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.dni").value("12345678A"))
                .andExpect(jsonPath("$.fullName").value("User Lastname"))
                .andExpect(jsonPath("$.email").value("user@email.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void get_customer_not_found() throws Exception {

        UUID customerId = UUID.randomUUID();

        Mockito.when(service.getCustomer(customerId))
                .thenThrow(new UsernameNotFoundException("El usuario no existe"));

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/customers/{customerId}", customerId)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("El usuario no existe"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void get_customer_invalid_uuid_format() throws Exception {

        String invalidUuid = "invalid-uuid-format";

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/customers/{customerId}", invalidUuid)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UUID_FORMAT"))
                .andExpect(jsonPath("$.message").value("Formato de ID no válido. Introduce una ID válida"));
    }

    @Test
    void get_customer_missing_id() throws Exception {

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/customers")
                )
                .andExpect(status().isNotFound());
    }


}
