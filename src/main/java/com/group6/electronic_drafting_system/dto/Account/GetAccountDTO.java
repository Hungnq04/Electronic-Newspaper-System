package com.group6.electronic_drafting_system.dto.Account;

import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
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
    private AccountStatus status;
}
