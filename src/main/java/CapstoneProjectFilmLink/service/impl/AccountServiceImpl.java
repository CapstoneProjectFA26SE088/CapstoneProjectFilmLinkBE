package CapstoneProjectFilmLink.service.impl;


import CapstoneProjectFilmLink.dto.request.AccountRequest;
import CapstoneProjectFilmLink.dto.request.LoginRequest;
import CapstoneProjectFilmLink.model.Account;
import CapstoneProjectFilmLink.model.Status.AccountStatus;
import CapstoneProjectFilmLink.repository.AccountRepository;
import CapstoneProjectFilmLink.security.service.AuthenticationService;
import CapstoneProjectFilmLink.service.interfaces.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationService authenticationService;

    @Override
    public Account createAccount(AccountRequest account) {
        // Validate email
        if (accountRepository.findByEmail(account.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists: " + account.getEmail());
        }

        Account newAccount = new Account();
        newAccount.setEmail(account.getEmail());
        newAccount.setPassword(authenticationService.encodePassword(account.getPassword()));
        newAccount.setName(account.getName());
        newAccount.setPhoneNumber(account.getPhoneNumber());
        newAccount.setGender(account.getGender());
        newAccount.setStatus(AccountStatus.ACTIVE);
        return accountRepository.save(newAccount);

    }

    @Override
    public Map<String, Object> loginAccount(LoginRequest loginRequestDTO) {
        Account accountOpt = accountRepository.findByEmail(loginRequestDTO.getEmail()).orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), accountOpt.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        String token = authenticationService.generateToken(accountOpt);

        return Map.of(
                "token", token,
                "account", accountOpt);
    }
}

