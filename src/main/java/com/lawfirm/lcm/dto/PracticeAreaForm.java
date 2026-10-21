package com.lawfirm.lcm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PracticeAreaForm {
    @NotBlank(message = "Name is required")
    private String name;
    private String description;
}
