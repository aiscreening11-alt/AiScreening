package com.company.recruitment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewerAuthResponseDto {

    private UUID id;
    private String fullName;
    private String email;
    private String designation;
    private String department;
    private String role;
    private boolean active;
    private String token;
}
