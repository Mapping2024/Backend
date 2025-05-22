package com.rhkr8521.mapping.api.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhkr8521.mapping.api.member.dto.GoogleTokenResponseDTO;
import com.rhkr8521.mapping.api.member.dto.GoogleUserInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    private static final String TOKEN_URL    = "https://oauth2.googleapis.com/token";
    private static final String REVOKE_URL   = "https://oauth2.googleapis.com/revoke";
    private static final String USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper  objectMapper  = new ObjectMapper();

    /**
     * 1) code → access/refresh 토큰 교환
     * 2) access_token → userinfo(id, email) 조회
     */
    public GoogleUserInfoDTO getGoogleUserInfo(String code) throws Exception {
        // --- 1) Authorization Code → Token ---
        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> tokenParams = new LinkedMultiValueMap<>();
        tokenParams.add("grant_type",    "authorization_code");
        tokenParams.add("client_id",     googleClientId);
        tokenParams.add("client_secret", googleClientSecret);
        tokenParams.add("redirect_uri",  googleRedirectUri);
        tokenParams.add("code",          code);

        HttpEntity<MultiValueMap<String, String>> tokenRequest =
                new HttpEntity<>(tokenParams, tokenHeaders);

        ResponseEntity<GoogleTokenResponseDTO> tokenResponse =
                restTemplate.postForEntity(TOKEN_URL, tokenRequest, GoogleTokenResponseDTO.class);

        if (!tokenResponse.getStatusCode().is2xxSuccessful() || tokenResponse.getBody() == null) {
            log.error("Google OAuth 토큰 발급 실패: status={}", tokenResponse.getStatusCode());
            throw new Exception("Google OAuth 토큰 발급 오류");
        }
        GoogleTokenResponseDTO tokenDto = tokenResponse.getBody();

        // --- 2) access_token → UserInfo(id, email) ---
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(tokenDto.getAccess_token());
        HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

        ResponseEntity<String> userInfoResponse =
                restTemplate.exchange(USERINFO_URL, HttpMethod.GET, userRequest, String.class);

        if (!userInfoResponse.getStatusCode().is2xxSuccessful() || userInfoResponse.getBody() == null) {
            log.error("Google 사용자 정보 조회 실패: status={}", userInfoResponse.getStatusCode());
            throw new Exception("Google 사용자 정보 조회 오류");
        }

        JsonNode userJson = objectMapper.readTree(userInfoResponse.getBody());
        return GoogleUserInfoDTO.builder()
                .id(userJson.get("sub").asText())
                .email(userJson.get("email").asText())
                .refreshToken(tokenDto.getRefresh_token())
                .build();
    }

    // Google RefreshToken -> AccessToken 재발급 로직
    public String refreshGoogleAccessToken(String refreshToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type",    "refresh_token");
        params.add("client_id",     googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(params, headers);

        ResponseEntity<GoogleTokenResponseDTO> response =
                restTemplate.postForEntity(TOKEN_URL, request, GoogleTokenResponseDTO.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.error("Google AccessToken 재발급 실패: status={}", response.getStatusCode());
            throw new Exception("Google AccessToken 재발급 오류");
        }
        return response.getBody().getAccess_token();
    }

    // Google OAuth2 맵핑 해제
    public void unlinkGoogleUser(String accessToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String url = REVOKE_URL + "?token=" + accessToken;

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Google 계정 연결 해제 실패: status={}", response.getStatusCode());
            throw new Exception("Google 계정 연결 해제 오류");
        }
    }
}
