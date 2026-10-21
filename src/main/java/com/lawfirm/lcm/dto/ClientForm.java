package com.lawfirm.lcm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientForm {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Type is required")
    private String type = "PERSON";

    private String nationalId;
    private String email;
    private String phone;
    private String address;
    private String conflictCheckNotes;
}
