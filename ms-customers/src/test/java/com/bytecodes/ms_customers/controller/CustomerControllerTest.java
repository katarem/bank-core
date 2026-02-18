package com.bytecodes.ms_customers.controller;

import com.bytecodes.ms_customers.handler.CustomerExceptionHandler;
import com.bytecodes.ms_customers.model.Customer;
import com.bytecodes.ms_customers.model.SafeCustomer;
import com.bytecodes.ms_customers.model.SafeUpdateCustomer;
import com.bytecodes.ms_customers.service.CustomerService;
import com.bytecodes.ms_customers.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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

    private String userToken;

    @MockitoBean
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        Customer customer = new Customer();
        customer.setEmail("user@email.com");
        customer.setPassword("PassWord123");
        userToken = jwtUtil.generateToken(customer);
    }

    @Test
    void get_me_ok() throws Exception {

        // given
        var safeUser = new SafeCustomer();
        safeUser.setFirstName("user");

        //when
        Mockito.when(service.getMyProfile(Mockito.any(String.class)))
                .thenReturn(safeUser);

        //then
        mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/api/customers/me")
                        .header("Authorization", "Bearer " + userToken)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("user"));

    }

    @Test
    void get_me_user_not_found() throws Exception {

        // given
        var safeUser = new SafeCustomer();
        safeUser.setFirstName("user");

        //when
        Mockito.when(service.getMyProfile(Mockito.any(String.class)))
                        .thenThrow(new UsernameNotFoundException(""));

        //then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/customers/me")
                                .header("Authorization", "Bearer " + userToken)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CUSTOMER_NOT_FOUND"));

    }

    @Test
    void get_me_user_token_not_provided() throws Exception {

        // given
        var safeUser = new SafeCustomer();
        safeUser.setFirstName("user");

        //when && then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/customers/me")
                )
                .andExpect(status().isBadRequest());

    }

    @Test
    void put_me_ok() throws Exception {
        // given
        var safeUser = new SafeCustomer();
        safeUser.setFirstName("user");

        //when
        Mockito.when(service.getMyProfile(Mockito.any(String.class)))
                .thenReturn(safeUser);

        //then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .put("/api/customers/me")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(safeUser))
                )
                .andExpect(status().isOk());
    }

    @Test
    void put_me_not_found() throws Exception {
        // given
        var safeUser = new SafeCustomer();
        safeUser.setFirstName("user");

        //when
        Mockito.when(service.updateMyProfile(Mockito.any(String.class), Mockito.any(SafeUpdateCustomer.class)))
                .thenThrow(new UsernameNotFoundException(""));

        //then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .put("/api/customers/me")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(safeUser))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CUSTOMER_NOT_FOUND"));

    }

    @Test
    void put_me_token_not_provided() throws Exception {

        // given
        var safeUser = new SafeCustomer();
        safeUser.setFirstName("user");

        //when && then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .put("/api/customers/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(safeUser))
                )
                .andExpect(status().isBadRequest());
    }


}
