package com.fa26se088.filmlink.service.account.interfaces;

import com.fa26se088.filmlink.dto.account.request.AccountRequest;
import com.fa26se088.filmlink.dto.account.request.LoginRequest;

import com.fa26se088.filmlink.entity.account.Account;

import java.util.Map;

public interface AccountService {
    Account createAccount(AccountRequest account);
    Map<String, Object> loginAccount(LoginRequest loginRequestDTO);
}
