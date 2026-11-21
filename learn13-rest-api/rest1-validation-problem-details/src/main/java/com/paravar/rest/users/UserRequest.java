package com.paravar.rest.users;

import com.paravar.rest.validation.PasswordsMatch;
import com.paravar.rest.validation.StrongPassword;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

// class-level rule checks two fields together; groups pick rules per operation
@PasswordsMatch(groups = UserRequest.OnCreate.class)
public record UserRequest(
        @Null(groups = OnCreate.class, message = "{user.id.notAllowed}") @NotNull(groups = OnUpdate.class) Long id,
        @NotBlank @Size(max = 40) String name,
        @NotBlank @Email String email,
        @Min(18) @Max(120) int age,
        @NotBlank(groups = OnCreate.class) @StrongPassword(groups = OnCreate.class) String password,
        String confirmPassword,
        @NotNull @Valid Address address) {

    public interface OnCreate extends Default {
    }

    public interface OnUpdate extends Default {
    }

    public record Address(@NotBlank String city, @Pattern(regexp = "\\d{6}", message = "{address.pin.pattern}") String pin) {
    }
}
