package com.bytecodes.ms_customers.controller;

import com.bytecodes.ms_customers.handler.CustomerExceptionHandler;
import com.bytecodes.ms_customers.response.SuccessfulAuthResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.bytecodes.ms_customers.model.Customer;
import com.bytecodes.ms_customers.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthController.class, properties = {
        "jwt.secret=bocnbRHda/WxWwAhMhCoxBmfK6mLWn/4o2r7STfN0M4=",
        "jwt.expiration=86400000",
})
@AutoConfigureMockMvc(addFilters = false)
@Import(CustomerExceptionHandler.class)
public class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Test
    void register_customer_created() throws Exception{

        // given (solo los fields requeridos para minimizar el test)
        Customer customer = new Customer();
        customer.setEmail("test@ing.com");
        customer.setDni("12345678L");
        customer.setPassword("Secure123!");

        // when (cuando le doy el payload como quiero que se comporte)
        Mockito.when(customerService.registerCustomer(Mockito.any(Customer.class)))
            .thenReturn(customer);

        // then (comprobamos el comportamiento ejecutando lo que vamos a probar)
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customer))
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(customer.getEmail()))
                .andExpect(jsonPath("$.dni").value(customer.getDni()))
                .andExpect(jsonPath("$.password").value(customer.getPassword()));

    }

    @Test
    void register_customer_bad_request() throws Exception{
        // given (solo los fields requeridos para minimizar el test)
        Customer customer = new Customer();
        customer.setEmail("test@ing.com");
        customer.setDni("12345678L");
        customer.setPassword("Secu!");
        // when (En este caso no debemos configurar comportamiento, ya que no llegará a nuestros mocks)

        // then (comprobamos el comportamiento ejecutando lo que vamos a probar)
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(customer))
                )
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("badCustomersProvider")
    void register_customer_bad_request_all(Customer customer) throws Exception {

        // when (En este caso no debemos configurar comportamiento, ya que no llegará a nuestros mocks)

        // then (comprobamos el comportamiento ejecutando lo que vamos a probar)
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(customer))
                )
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("conflictsProvider")
    void register_customer_conflict(DataIntegrityViolationException exception) throws Exception {
        // given
        Customer customer = new Customer();
        customer.setDni("12345678L");
        customer.setEmail("email@mail.com");
        customer.setPassword("Password123");

        // when
        Mockito.when(customerService.registerCustomer(customer))
                        .thenThrow(exception);

        // then (comprobamos el comportamiento ejecutando lo que vamos a probar)
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(customer))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void login_customer_ok() throws Exception {

        // given
        Customer auth = new Customer();
        auth.setEmail("auth@auth.com");
        auth.setPassword("MyPassword123");

        // when
        Mockito.when(customerService.loginCustomer(auth))
                .thenReturn(SuccessfulAuthResponse.builder().build());

        // then
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(auth))
        )
                .andExpect(status().isOk());

    }

    @Test
    void login_customer_invalid_credentials() throws Exception {

        // given
        Customer auth = new Customer();
        auth.setEmail("auth@auth.com");
        auth.setPassword("MyPassword123");

        // when
        Mockito.when(customerService.loginCustomer(Mockito.any(Customer.class)))
                .thenThrow(BadCredentialsException.class);

        // then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(auth))
                )
                .andExpect(status().isUnauthorized());

    }

    @ParameterizedTest
    @MethodSource("badCustomersProvider")
    void login_customer_bad_customer(Customer customer) throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customer))
        ).andExpect(status().isBadRequest());
    }

    private static Stream<Arguments> badCustomersProvider() {
        Customer wrongEmail = new Customer();
        wrongEmail.setEmail("a.com");

        Customer wrongPassword = new Customer();
        wrongPassword.setPassword("hola");

        Customer wrongDni = new Customer();
        wrongDni.setDni("L32432421");

        return Stream.of(
                Arguments.of(wrongEmail),
                Arguments.of(wrongPassword),
                Arguments.of(wrongDni)
        );
    }

    private static Stream<Arguments> conflictsProvider() {
        return Stream.of(
                Arguments.of(new DataIntegrityViolationException(
                        "constraint violation",
                        new RuntimeException("duplicate key value violates unique constraint (dni)")
                )),
                Arguments.of(
                        new DataIntegrityViolationException(
                                "constraint violation",
                                new RuntimeException("duplicate key value violates unique constraint (email)")
                        )
                )
        );
    }

}
