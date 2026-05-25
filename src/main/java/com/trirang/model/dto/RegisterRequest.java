package com.trirang.model.dto;

import com.trirang.model.enums.shared.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RegisterRequest(
    @NotBlank(message = "Name is required") String name,
    @Email(message = "Invalid email format") @NotBlank(message = "Email is required") String email,
    String phone,
    @NotBlank(message = "Password is required") String password,
    @NotNull(message = "Role is required") Role role,
    String address,
    BigDecimal latitude,
    BigDecimal longitude
) {}
