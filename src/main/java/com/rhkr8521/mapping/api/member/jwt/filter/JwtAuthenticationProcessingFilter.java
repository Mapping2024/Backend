package com.rhkr8521.mapping.api.member.jwt.filter;

import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.api.member.repository.MemberRepository;
import com.rhkr8521.mapping.api.member.jwt.service.JwtService;
import com.rhkr8521.mapping.api.member.jwt.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationProcessingFilter extends OncePerRequestFilter {

    @Value("${jwt.access.header}")
    private String accessTokenHeader;

    @Value("${jwt.refresh.header}")
    private String refreshTokenHeader;

    private static final String NO_CHECK_URL = "/oauth2/authorization/kakao"; // 카카오 OAuth 요청 제외
    private static final String TOKEN_REISSUE_URL = "/api/v1/member/token-reissue"; // 토큰 재발급 엔드포인트

    private final JwtService jwtService;
    private final MemberRepository memberRepository;

    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        // 카카오 OAuth 요청 제외
        if (requestURI.equals(NO_CHECK_URL)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 리프레시 토큰 재발급 로직은 /token-reissue 엔드포인트에서만 수행
        if (requestURI.equals(TOKEN_REISSUE_URL)) {
            // Refresh Token이 존재하는지 확인
            Optional<String> refreshToken = extractToken(request, refreshTokenHeader)
                    .filter(jwtService::isTokenValid);

            // Refresh Token이 유효하면 Access Token을 재발급하고 인증 정보를 설정
            if (refreshToken.isPresent()) {
                handleRefreshToken(response, refreshToken.get());
            }
            filterChain.doFilter(request, response);
            return;
        }

        // Access Token이 존재하고 유효한지 확인
        Optional<String> accessToken = extractToken(request, accessTokenHeader)
                .filter(jwtService::isTokenValid);

        accessToken.ifPresent(token -> jwtService.extractEmail(token)
                .ifPresent(email -> memberRepository.findByEmail(email)
                        .ifPresent(this::setAuthentication)));

        filterChain.doFilter(request, response);
    }

    // Refresh Token을 처리하여 Access Token 재발급 및 인증 처리
    private void handleRefreshToken(HttpServletResponse response, String refreshToken) {
        memberRepository.findByRefreshToken(refreshToken)
                .ifPresent(user -> {
                    String newAccessToken = jwtService.createAccessToken(user.getEmail());
                    String newRefreshToken = jwtService.createRefreshToken(user.getEmail());

                    // Refresh Token 업데이트
                    jwtService.updateRefreshToken(user.getEmail(), newRefreshToken);

                    // 새로운 Access Token과 Refresh Token을 헤더로 설정
                    response.setHeader(accessTokenHeader, "Bearer " + newAccessToken);
                    response.setHeader(refreshTokenHeader, "Bearer " + newRefreshToken);

                    log.info("Access Token, Refresh Token 재발급 완료");
                    log.info("Access Token : {}", newAccessToken);
                    log.info("Refresh Token : {}", newRefreshToken);
                });
    }

    // 요청 헤더에서 토큰을 추출하는 메서드
    private Optional<String> extractToken(HttpServletRequest request, String headerName) {
        String bearerToken = request.getHeader(headerName);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return Optional.of(bearerToken.substring(7)); // "Bearer " 이후의 토큰 부분만 반환
        }
        return Optional.empty();
    }

    // 인증 정보를 SecurityContext에 설정하는 메서드
    private void setAuthentication(Member member) {
        String password = member.getPassword();
        if (password == null) {
            password = PasswordUtil.generateRandomPassword();
        }

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(member.getEmail())
                .password(password)
                .roles(member.getRole().name())
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, authoritiesMapper.mapAuthorities(userDetails.getAuthorities()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}