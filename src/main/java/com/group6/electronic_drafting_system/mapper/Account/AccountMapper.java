package com.group6.electronic_drafting_system.mapper.Account;

import com.group6.electronic_drafting_system.dto.Account.GetAccountDTO;
import com.group6.electronic_drafting_system.entity.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {
    public GetAccountDTO toGetAccountDTO(Account account) {
        return GetAccountDTO.builder()
                .id(account.getId())
                .username(account.getUsername())
                .fullName(account.getFullName())
                .email(account.getEmail())
                .role(account.getRole().getRoleName())
                .status(account.getStatus())
                .build();
    }
}
