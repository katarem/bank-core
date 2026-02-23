package com.bytecodes.ms_customers.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequest {
    @Email(message = "El email debe tener un formato válido")
    private String email;
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z]).{8,}$", 
        message = "La contraseña debe tener como mínimo 8 caracteres, 1 letra minúscula y 1 mayúscula")
    private String password;
}
