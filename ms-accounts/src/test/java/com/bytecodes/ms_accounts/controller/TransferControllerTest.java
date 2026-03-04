package com.bytecodes.ms_accounts.controller;

import com.bytecodes.ms_accounts.dto.request.CreateTransferRequest;
import com.bytecodes.ms_accounts.dto.response.CreateTransferResponse;
import com.bytecodes.ms_accounts.handler.AccountExceptionHandler;
import com.bytecodes.ms_accounts.handler.exceptions.AccountNotFoundException;
import com.bytecodes.ms_accounts.handler.exceptions.CustomerIsInactiveException;
import com.bytecodes.ms_accounts.model.TransferStatus;
import com.bytecodes.ms_accounts.service.AccountBalanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AccountExceptionHandler.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountBalanceService accountBalanceService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void transfer_ok() throws Exception {
        // given
        CreateTransferRequest request = buildValidRequest();
        CreateTransferResponse response = buildResponse(TransferStatus.COMPLETED);

        // when
        Mockito.when(accountBalanceService.createTransfer(Mockito.any(CreateTransferRequest.class), Mockito.any()))
                .thenReturn(response);

        // then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferId").value(response.getTransferId().toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.sourceAccount").value(response.getSourceAccount()))
                .andExpect(jsonPath("$.destinationAccount").value(response.getDestinationAccount()))
                .andExpect(jsonPath("$.beneficiaryName").value(response.getBeneficiaryName()))
                .andExpect(jsonPath("$.concept").value(response.getConcept()));
    }

    @Test
    void transfer_error_source_account_does_not_exist() throws Exception {
        // given
        CreateTransferRequest request = buildValidRequest();
        Mockito.when(accountBalanceService.createTransfer(Mockito.any(CreateTransferRequest.class), Mockito.any()))
                .thenThrow(new AccountNotFoundException(request.getSourceAccountId().toString()));

        // then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void transfer_error_source_client_does_not_exist() throws Exception {
        // given
        CreateTransferRequest request = buildValidRequest();
        Mockito.when(accountBalanceService.createTransfer(Mockito.any(CreateTransferRequest.class), Mockito.any()))
                .thenThrow(new UsernameNotFoundException("Not found"));

        // then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void transfer_error_source_client_is_inactive() throws Exception {
        // given
        CreateTransferRequest request = buildValidRequest();
        Mockito.when(accountBalanceService.createTransfer(Mockito.any(CreateTransferRequest.class), Mockito.any()))
                .thenThrow(new CustomerIsInactiveException());

        // then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void transfer_error_destination_account_does_not_exist() throws Exception {
        // given
        CreateTransferRequest request = buildValidRequest();
        Mockito.when(accountBalanceService.createTransfer(Mockito.any(CreateTransferRequest.class), Mockito.any()))
                .thenThrow(new UsernameNotFoundException("Account not found"));

        // then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void transfer_error_destination_client_does_not_exist() throws Exception {
        // given
        CreateTransferRequest request = buildValidRequest();
        Mockito.when(accountBalanceService.createTransfer(Mockito.any(CreateTransferRequest.class), Mockito.any()))
                .thenThrow(new UsernameNotFoundException("Not found"));

        // then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void transfer_error_destination_client_is_inactive() throws Exception {
        // given
        CreateTransferRequest request = buildValidRequest();
        Mockito.when(accountBalanceService.createTransfer(Mockito.any(CreateTransferRequest.class), Mockito.any()))
                .thenThrow(new CustomerIsInactiveException());

        // then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void transfer_error_source_account_has_not_enough_balance() throws Exception {
        // given
        CreateTransferRequest request = buildValidRequest();
        CreateTransferResponse response = buildResponse(TransferStatus.FAILED);
        Mockito.when(accountBalanceService.createTransfer(Mockito.any(CreateTransferRequest.class), Mockito.any()))
                .thenReturn(response);

        // then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void error_while_making_transaction_error_status_persisted() throws Exception {
        // given
        CreateTransferRequest request = buildValidRequest();
        Mockito.when(accountBalanceService.createTransfer(Mockito.any(CreateTransferRequest.class), Mockito.any()))
                .thenThrow(new RuntimeException("database error"));

        // then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isInternalServerError());
    }

    @ParameterizedTest
    @MethodSource("invalidTransferPayloadProvider")
    void transfer_invalid_payload_returns_bad_request(String payload) throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FIELDS"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        Mockito.verifyNoInteractions(accountBalanceService);
    }

    @Test
    void transfer_invalid_json_returns_bad_request() throws Exception {
        String invalidJson = """
                {
                  "sourceAccountId": "not-an-uuid",
                  "destinationAccountNumber": "ES1234",
                  "amount": "invalid-number"
                }
                """;

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        Mockito.verifyNoInteractions(accountBalanceService);
    }

    private static Stream<Arguments> invalidTransferPayloadProvider() {
        return Stream.of(
                Arguments.of("""
                        {
                          "destinationAccountNumber": "ES1234567890123456789012",
                          "amount": 10
                        }
                        """),
                Arguments.of("""
                        {
                          "sourceAccountId": "11111111-1111-1111-1111-111111111111",
                          "destinationAccountNumber": "INVALID_IBAN",
                          "amount": 10
                        }
                        """),
                Arguments.of("""
                        {
                          "sourceAccountId": "11111111-1111-1111-1111-111111111111",
                          "destinationAccountNumber": "ES1234567890123456789012",
                          "amount": 0.5
                        }
                        """),
                Arguments.of("""
                        {
                          "sourceAccountId": "11111111-1111-1111-1111-111111111111",
                          "destinationAccountNumber": "ES1234567890123456789012",
                          "amount": 10000.01
                        }
                        """)
        );
    }

    private CreateTransferRequest buildValidRequest() {
        CreateTransferRequest request = new CreateTransferRequest();
        request.setSourceAccountId(UUID.randomUUID());
        request.setDestinationAccountNumber("ES1234567890123456789012");
        request.setAmount(new BigDecimal("125.75"));
        request.setConcept("Pago de alquiler");
        return request;
    }

    private CreateTransferResponse buildResponse(TransferStatus status) {
        return CreateTransferResponse.builder()
                .transferId(UUID.randomUUID())
                .status(status)
                .sourceAccount("ES1111111111111111111111")
                .destinationAccount("ES2222222222222222222222")
                .beneficiaryName("Jane Doe")
                .amount(new BigDecimal("125.75"))
                .concept("Pago de alquiler")
                .fee(BigDecimal.ZERO)
                .totalDebited(new BigDecimal("125.75"))
                .timestamp(Instant.now())
                .build();
    }
}
