package CapstoneProjectFilmLink.service.interfaces;

import CapstoneProjectFilmLink.dto.request.AccountRequest;
import CapstoneProjectFilmLink.dto.request.LoginRequest;
import CapstoneProjectFilmLink.model.Account;

import java.util.Map;

public interface AccountService {
    Account createAccount(AccountRequest account);
    Map<String, Object> loginAccount(LoginRequest loginRequestDTO);
}
