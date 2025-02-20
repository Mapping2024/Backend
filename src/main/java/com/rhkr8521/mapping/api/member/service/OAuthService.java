package com.rhkr8521.mapping.api.member.service;

import com.rhkr8521.mapping.api.member.dto.KakaoUserInfoDTO;
import com.rhkr8521.mapping.common.exception.InternalServerException;
import com.rhkr8521.mapping.common.exception.UnauthorizedException;
import com.rhkr8521.mapping.common.response.ErrorStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${kakao.admin-key}")
    private String kakaoAdminKey;

    // 카카오에서 인가 코드를 이용해 액세스 토큰을 받아오는 메서드
    public String getKakaoAccessToken(String code) {
        String url = "https://kauth.kakao.com/oauth/token";

        // HttpHeaders 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 파라미터 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code);
        params.add("client_secret", kakaoClientSecret);

        // HttpEntity에 헤더와 파라미터 설정
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        // 카카오 액세스 토큰 요청
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
                return result.get("access_token").toString();
            } catch (IOException e) {
                throw new RuntimeException("카카오 액세스 토큰 파싱 중 오류 발생", e);
            }
        } else {
            throw new RuntimeException("카카오 액세스 토큰 요청 실패");
        }
    }

    // 카카오 사용자 정보를 가져오는 메서드
    public KakaoUserInfoDTO getKakaoUserInfo(String accessToken) {
        String url = "https://kapi.kakao.com/v2/user/me";

        // HttpHeaders 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken); // Bearer 토큰으로 인증

        // HttpEntity에 헤더 추가
        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            // 카카오 API 호출
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                // JSON 파싱
                String body = response.getBody();
                return parseKakaoUserInfo(body); // JSON 데이터를 KakaoUserInfoDTO로 변환
            } else {
                throw new InternalServerException(ErrorStatus.FAIL_PARSE_KAKAO_USER_INFO.getMessage());
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            // 401 Unauthorized 처리
            throw new UnauthorizedException(ErrorStatus.INVALID_KAKAO_ACCESSTOKEN_EXCEPTION.getMessage());
        } catch (HttpClientErrorException e) {
            // 기타 에러 처리
            throw new InternalServerException(ErrorStatus.FAIL_REQUEST_KAKAO_USER_INFO.getMessage());
        } catch (Exception e) {
            throw new InternalServerException(ErrorStatus.SERVER_ERROR.getMessage());
        }
    }

    // KakaoUserInfoDTO로 변환
    private KakaoUserInfoDTO parseKakaoUserInfo(String body) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> result = objectMapper.readValue(body, Map.class);

            // 프로필 정보 추출
            Map<String, Object> kakaoAccount = (Map<String, Object>) result.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            // 사용자 정보 생성
            KakaoUserInfoDTO userInfo = new KakaoUserInfoDTO();
            userInfo.setId(result.get("id").toString());
            userInfo.setEmail(kakaoAccount.get("email") != null ? kakaoAccount.get("email").toString() : null);
            userInfo.setNickname(profile.get("nickname") != null ? profile.get("nickname").toString() : null);
            userInfo.setProfileImage(profile.get("thumbnail_image_url") != null ? profile.get("thumbnail_image_url").toString() : null);

            return userInfo;
        } catch (Exception e) {
            throw new InternalServerException(ErrorStatus.FAIL_PARSE_KAKAO_USER_INFO.getMessage());
        }
    }

    // 카카오 앱 연결 해제
    public void unlinkKakaoUser(String kakaoUserId) {
        String url = "https://kapi.kakao.com/v1/user/unlink";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "KakaoAK " + kakaoAdminKey);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("target_id_type", "user_id");
        params.add("target_id", kakaoUserId);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(ErrorStatus.FAIL_UNLINK_KAKAO_OAUTH_EXCEPTION.getMessage() + response.getBody());
        }
    }
}
