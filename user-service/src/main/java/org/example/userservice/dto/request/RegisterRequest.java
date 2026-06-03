package org.example.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.userservice.validation.ValidPassword;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 30)
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]+$",
            message = "Username contains invalid characters"
    )
    private String username;

    @NotBlank(message = "Email is required")
    @Email
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Password is required")
    @ValidPassword
    private String password;
}
