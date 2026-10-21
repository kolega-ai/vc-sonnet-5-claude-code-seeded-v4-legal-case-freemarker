package com.lawfirm.lcm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileForm {
    @NotBlank(message = "Full name is required")
    private String fullName;
}
