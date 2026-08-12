package ru.practicum.explorewithme.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewUserRequest {
    @NotBlank(message = "email should be not blank")
    @Email
    @Size(min = 6, max = 254)
    private String email;
    @NotBlank(message = "name should be not blank")
    @Size(min = 2, max = 250)
    private String name;
}
