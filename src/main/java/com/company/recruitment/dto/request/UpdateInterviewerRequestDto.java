package com.company.recruitment.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInterviewerRequestDto {

    private String fullName;

    private String designation;

    private String department;

    private String phoneNumber;

    private Boolean active;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;
}
