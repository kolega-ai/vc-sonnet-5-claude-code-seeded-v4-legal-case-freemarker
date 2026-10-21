package com.lawfirm.lcm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageForm {
    @NotBlank(message = "Message body is required")
    private String body;
}
