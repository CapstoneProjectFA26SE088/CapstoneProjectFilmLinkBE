package com.fa26se088.filmlink.service.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditLogServiceImplTest {

  /** Chuỗi toàn chữ hex là tên máy, không phải IP: phải trả null, không tra DNS. */
  @Test
  void parseIpLiteralRejectsHostLikeHexString() {
    assertThat(AuditLogServiceImpl.parseIpLiteral("abc")).isNull();
  }

  @Test
  void parseIpLiteralAcceptsIpv4AndIpv6() {
    assertThat(AuditLogServiceImpl.parseIpLiteral("203.0.113.7").getHostAddress())
        .isEqualTo("203.0.113.7");
    assertThat(AuditLogServiceImpl.parseIpLiteral("::1")).isNotNull();
  }
}
