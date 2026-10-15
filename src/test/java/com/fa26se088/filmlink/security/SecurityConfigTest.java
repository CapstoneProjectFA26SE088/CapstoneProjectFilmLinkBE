package com.fa26se088.filmlink.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private RoleHierarchy roleHierarchy;

  @Test
  void apiDocsArePublic() throws Exception {
    mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
  }

  @Test
  void protectedEndpointWithoutTokenReturnsProblemDetail401() throws Exception {
    mockMvc
        .perform(get("/api/v1/accounts/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
  }

  @Test
  void invalidTokenReturns401() throws Exception {
    mockMvc
        .perform(get("/api/v1/accounts/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
  }

  @Test
  void unknownApiPathRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/anything")).andExpect(status().isUnauthorized());
  }

  /** Register là public: body sai trả 400 từ validation, không phải 401. */
  @Test
  void registerIsPublic() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
  }

  @Test
  void logoutRequiresAuthentication() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"x\"}"))
        .andExpect(status().isUnauthorized());
  }

  /** VNPay spike bắt buộc đăng nhập. */
  @Test
  void paymentSandboxUrlRequiresAuthentication() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/payments/vnpay/sandbox-url")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amountVnd\":100000}"))
        .andExpect(status().isUnauthorized());
  }

  /** BR-05: ADMIN ⊃ MODERATOR, PHOTOGRAPHY_EXPERT ⊃ PHOTOGRAPHER; chiều ngược lại không đúng. */
  @Test
  void roleHierarchyFollowsBr05() {
    assertThat(reachable("ROLE_ADMIN")).contains("ROLE_MODERATOR");
    assertThat(reachable("ROLE_MODERATOR")).doesNotContain("ROLE_ADMIN");
    assertThat(reachable("ROLE_PHOTOGRAPHY_EXPERT")).contains("ROLE_PHOTOGRAPHER");
    assertThat(reachable("ROLE_PHOTOGRAPHER")).doesNotContain("ROLE_PHOTOGRAPHY_EXPERT");
    assertThat(reachable("ROLE_FILM_LAB_OWNER")).containsExactly("ROLE_FILM_LAB_OWNER");
  }

  private List<String> reachable(String role) {
    Collection<? extends GrantedAuthority> authorities =
        roleHierarchy.getReachableGrantedAuthorities(List.of(new SimpleGrantedAuthority(role)));
    return authorities.stream().map(GrantedAuthority::getAuthority).toList();
  }
}
