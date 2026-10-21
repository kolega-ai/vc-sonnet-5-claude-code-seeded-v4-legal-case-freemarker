package com.lawfirm.lcm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatterForm {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Client is required")
    private Long clientId;

    private Long practiceAreaId;

    private Long assignedAttorneyId;

    @NotNull(message = "Sensitivity level is required")
    private String sensitivityLevel = "LOW";
}
