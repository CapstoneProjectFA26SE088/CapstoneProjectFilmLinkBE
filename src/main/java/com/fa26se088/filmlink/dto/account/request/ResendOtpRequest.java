package com.fa26se088.filmlink.dto.account.request;

import com.fa26se088.filmlink.entity.account.enums.OtpPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * UC06 / UH-36: yêu cầu gửi lại mã OTP qua email.
 *
 * <p>Mặc định {@link OtpPurpose#EMAIL_VERIFICATION} nếu {@code purpose} để trống hoặc null.
 */
public record ResendOtpRequest(
    @NotBlank
    @Email
    @Size(max = 255)
    @Schema(description = "Email tài khoản cần gửi lại mã OTP", example = "photographer@example.com")
    String email,

    @Schema(
        description = "Mục đích của mã OTP, mặc định là EMAIL_VERIFICATION",
        example = "EMAIL_VERIFICATION")
    OtpPurpose purpose) {

  public ResendOtpRequest {
    if (purpose == null) {
      purpose = OtpPurpose.EMAIL_VERIFICATION;
    }
  }

  public ResendOtpRequest(String email) {
    this(email, OtpPurpose.EMAIL_VERIFICATION);
  }
}
