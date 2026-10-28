package com.company.recruitment.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInterviewerRequestDto {

    @NotBlank(message = "Interviewer full name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email address is required")
    private String email;

    @NotBlank(message = "Initial password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String designation;

    private String department;

    private String phoneNumber;
}
