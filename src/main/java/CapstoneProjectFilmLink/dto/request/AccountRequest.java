package CapstoneProjectFilmLink.dto.request;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor

@Data
public class AccountRequest {
    private String password;
    private String email;
    private String phoneNumber;
    private String name;
    private String gender;
}
