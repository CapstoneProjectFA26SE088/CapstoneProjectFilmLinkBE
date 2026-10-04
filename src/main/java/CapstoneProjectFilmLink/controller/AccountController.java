package CapstoneProjectFilmLink.controller;

import CapstoneProjectFilmLink.dto.request.AccountRequest;
import CapstoneProjectFilmLink.dto.request.LoginRequest;
import CapstoneProjectFilmLink.dto.response.ResponseBase;
import CapstoneProjectFilmLink.model.Account;
import CapstoneProjectFilmLink.service.impl.AccountServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/accounts")
@RequiredArgsConstructor
@Tag(name = "Account Management", description = "APIs for managing user accounts")
public class AccountController {

    private final AccountServiceImpl accountService;

    @PostMapping("/register")
    public ResponseEntity<ResponseBase> registerAccount(@RequestBody AccountRequest accountRequest) {
        try {
            Account newAccount = accountService.createAccount(accountRequest);
            ResponseBase result = new ResponseBase();
            result.setCode(201);
            result.setMessage("Account created successfully");
            result.setData(newAccount);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            ResponseBase result = new ResponseBase();
            result.setCode(401);
            result.setMessage("Failed to create account");
            result.setData(null);
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseBase> loginAccount(@RequestBody LoginRequest loginRequest) {
        try {
            Map<String, Object> loginResult = accountService.loginAccount(loginRequest);
            ResponseBase result = new ResponseBase();
            result.setCode(200);
            result.setMessage("Login successful");
            result.setData(loginResult);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            ResponseBase result = new ResponseBase();
            result.setCode(401);
            result.setMessage("Invalid email or password");
            result.setData(null);
            return ResponseEntity.status(401).body(result);
        }
    }
}
