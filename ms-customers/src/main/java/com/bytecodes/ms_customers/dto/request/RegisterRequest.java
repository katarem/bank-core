package com.bytecodes.ms_customers.dto.request;

import com.bytecodes.ms_customers.model.CustomerStatus;
import com.bytecodes.ms_customers.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class RegisterRequest {
    private UUID id;
    @Pattern(
            regexp = "^\\d{8}[A-Z]$",
            message = "El DNI debe tener un formato válido")
    private String dni;
    private String firstName;
    private String lastName;
    @Email(message = "El email debe tener un formato válido")
    private String email;
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z]).{8,}$",
            message = "La contraseña debe tener como mínimo 8 caracteres, 1 letra minúscula y 1 mayúscula")
    private String password;
    private String phone;
    private String address;
    private CustomerStatus status;
    private UserRole role;
    private Instant createdAt;
    private Instant updatedAt;
}
