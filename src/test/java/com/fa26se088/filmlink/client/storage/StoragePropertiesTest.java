package com.fa26se088.filmlink.client.storage;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class StoragePropertiesTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  /** NFR-PRI-02: link ký tối đa 15 phút. */
  @Test
  void signedUrlTtlOver15MinutesIsInvalid() {
    assertThat(validator.validate(properties(Duration.ofMinutes(16)))).isNotEmpty();
    assertThat(validator.validate(properties(Duration.ZERO))).isNotEmpty();
    assertThat(validator.validate(properties(Duration.ofMinutes(15)))).isEmpty();
  }

  private static StorageProperties properties(Duration ttl) {
    return new StorageProperties("local", ttl, null);
  }
}
