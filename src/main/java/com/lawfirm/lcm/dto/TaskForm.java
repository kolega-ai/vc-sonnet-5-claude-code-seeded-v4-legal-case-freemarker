package com.lawfirm.lcm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TaskForm {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Long assigneeId;

    private String priority = "MEDIUM";

    private LocalDate dueDate;
}
