package com.lawfirm.lcm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DeadlineForm {
    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private String rule;

    private Long responsibleId;
}
