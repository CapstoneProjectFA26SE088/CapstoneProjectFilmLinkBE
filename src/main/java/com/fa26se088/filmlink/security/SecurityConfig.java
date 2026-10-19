package com.fa26se088.filmlink.security;

import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import com.fa26se088.filmlink.service.account.AuthenticationService;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Cấu hình Spring Security: stateless, xác thực bằng access token JWT.
 *
 * <p>Mặc định mọi request phải đăng nhập. Chỉ các endpoint trong {@link #PUBLIC_POST_PATHS}, {@link
 * #PUBLIC_GET_PATHS}, Swagger và {@code /error} được mở. Phân quyền nghiệp vụ theo ma trận SRS 3.1.3 kiểm ở tầng service hoặc
 * bằng {@code @PreAuthorize} (CR-14).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  /** UC06, UC01: các bước trước khi có access token. */
  private static final String[] PUBLIC_POST_PATHS = {
    "/api/v1/auth/register", "/api/v1/auth/verify-email", "/api/v1/auth/login", "/api/v1/auth/refresh"
  };

  /**
   * Link tải file ký HMAC của storage local (NFR-PRI-02) và danh mục master data công khai (MD-01,
   * BR-16).
   */
  private static final String[] PUBLIC_GET_PATHS = {
    "/api/v1/files",
    "/api/v1/film-processes",
    "/api/v1/film-formats",
    "/api/v1/service-types",
    "/api/v1/film-stocks"
  };

  private static final String[] SWAGGER_PATHS = {
    "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
  };

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtTokenProvider jwtTokenProvider,
      AuthenticationService authenticationService,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
      CorsConfigurationSource corsConfigurationSource)
      throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.POST, PUBLIC_POST_PATHS)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS)
                    .permitAll()
                    .requestMatchers(SWAGGER_PATHS)
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                        new ProblemDetailAuthenticationEntryPoint(exceptionResolver))
                    .accessDeniedHandler(new ProblemDetailAccessDeniedHandler(exceptionResolver)))
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtTokenProvider, authenticationService),
            UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  /**
   * BR-05: ADMIN có mọi quyền của MODERATOR; PHOTOGRAPHY_EXPERT có mọi quyền của PHOTOGRAPHER.
   * Chiều ngược lại không đúng. Khai báo {@code static} để method security dùng được ngay khi khởi
   * tạo.
   */
  @Bean
  static RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.withDefaultRolePrefix()
        .role(UserAccountRole.ADMIN.name())
        .implies(UserAccountRole.MODERATOR.name())
        .role(UserAccountRole.PHOTOGRAPHY_EXPERT.name())
        .implies(UserAccountRole.PHOTOGRAPHER.name())
        .build();
  }

  /** Origin FE được phép gọi API, đọc từ {@code CORS_ALLOWED_ORIGINS} (phân tách dấu phẩy). */
  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      @Value("${filmlink.cors.allowed-origins}") List<String> allowedOrigins) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedOrigins.stream().map(String::trim).toList());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
