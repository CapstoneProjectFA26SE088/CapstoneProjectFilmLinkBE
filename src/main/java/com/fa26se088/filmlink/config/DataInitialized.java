package com.fa26se088.filmlink.config;


import com.fa26se088.filmlink.repository.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import jakarta.annotation.Nullable;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitialized implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public void run(@Nullable String... args) {

    }

}
