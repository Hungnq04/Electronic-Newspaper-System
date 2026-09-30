package com.group6.electronic_drafting_system.dto.Account;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetAccountDTO {
    private Integer id;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private String status;
}
