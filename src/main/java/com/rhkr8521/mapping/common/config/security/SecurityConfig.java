package com.rhkr8521.mapping.common.config.security;

import com.rhkr8521.mapping.common.config.jwt.JwtConfig;
import com.rhkr8521.mapping.common.config.oauth2.OAuth2Config;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtConfig jwtConfig;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .formLogin(form -> form.disable()) // FormLogin 사용 X
                .httpBasic(basic -> basic.disable()) // httpBasic 사용 X
                .csrf(csrf -> csrf.disable()) // csrf 보안 사용 X
                .cors(corsCustomizer -> corsCustomizer.configurationSource(new CorsConfigurationSource() {
                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                        CorsConfiguration config = new CorsConfiguration();
                        config.setAllowedOrigins(Arrays.asList(
                                "https://api.mapping.kro.kr",
                                "http://localhost:8100"
                        ));
                        config.setAllowedMethods(Collections.singletonList("*"));
                        config.setAllowCredentials(true);
                        config.setAllowedHeaders(Collections.singletonList("*"));
                        config.setMaxAge(3600L); //1시간
                        config.addExposedHeader("Authorization");
                        config.addExposedHeader("Authorization-Refresh");
                        return config;
                    }
                }))
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers( "/api-doc", "/health","/v3/api-docs/**", "/swagger-resources/**","/swagger-ui/**", "/h2-console/**").permitAll() // 스웨거, H2콘솔
                        .requestMatchers( "/api/v2/memo/total", "/api/v2/memo/detail").permitAll() // 메모 조회 관련 API
                        .requestMatchers(HttpMethod.GET, "/api/v2/comment/**").permitAll() // 댓글 조회 관련 API
                        .requestMatchers("/oauth2/authorization/kakao", "/api/v2/member/accesstoken", "/api/v2/member/login", "/api/v2/member/token-reissue").permitAll() //로그인 관련 API 미인증 접근 가능
                        .anyRequest().authenticated() // 위의 경로 이외에는 모두 인증된 사용자만 접근 가능
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) // 401 Unauthorized 반환
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/oauth2/authorization/kakao")
                        .defaultSuccessUrl("/api/v2/member/accesstoken", true)
                );

        // JwtAuthenticationProcessingFilter를 추가하여 JWT 인증을 처리
        http.addFilterBefore(jwtConfig.jwtAuthenticationProcessingFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * AuthenticationManager 설정 후 등록
     * FormLogin(기존 스프링 시큐리티 로그인)과 동일하게 DaoAuthenticationProvider 사용
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        return new ProviderManager(provider);
    }
}
