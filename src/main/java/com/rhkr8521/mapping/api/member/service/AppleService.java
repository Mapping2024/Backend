package com.rhkr8521.mapping.api.member.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.rhkr8521.mapping.api.member.dto.AppleLoginDTO;
import com.rhkr8521.mapping.common.exception.InternalServerException;
import com.rhkr8521.mapping.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URL;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppleService {

    @Value("${social-login.provider.apple.team-id}")
    private String APPLE_TEAM_ID;

    @Value("${social-login.provider.apple.key-id}")
    private String APPLE_LOGIN_KEY;

    @Value("${spring.security.oauth2.client.registration.apple.clientId}")
    private String APPLE_CLIENT_ID;

    @Value("${social-login.provider.apple.redirect-uri}")
    private String APPLE_REDIRECT_URL;

    @Value("${spring.security.oauth2.client.registration.apple.clientSecret}")
    private String APPLE_KEY_PATH;

    private final static String APPLE_AUTH_URL = "https://appleid.apple.com";

    /**
     * Apple authorization code를 받아 토큰 교환 후 사용자 정보를 파싱
     */
    public AppleLoginDTO getAppleInfo(String code) throws Exception {
        if (code == null) throw new Exception("authorization code가 없습니다.");

        String clientSecret = createClientSecret();
        String userId = "";
        String email = "";
        String accessToken = "";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", APPLE_CLIENT_ID);
            params.add("client_secret", clientSecret);
            params.add("code", code);
            params.add("redirect_uri", APPLE_REDIRECT_URL);

            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    APPLE_AUTH_URL + "/auth/token",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObj = (JSONObject) jsonParser.parse(response.getBody());
            accessToken = String.valueOf(jsonObj.get("access_token"));

            // ID 토큰 파싱하여 사용자 정보 추출
            SignedJWT signedJWT = SignedJWT.parse(String.valueOf(jsonObj.get("id_token")));
            JWTClaimsSet payload = signedJWT.getJWTClaimsSet();

            ObjectMapper objectMapper = new ObjectMapper();
            String payloadJson = objectMapper.writeValueAsString(payload.toJSONObject());
            JSONObject payloadObj = objectMapper.readValue(payloadJson, JSONObject.class);
            userId = String.valueOf(payloadObj.get("sub"));
            email = String.valueOf(payloadObj.get("email"));
        } catch (Exception e) {
            log.error("Apple 토큰 교환 오류: {}", e.getMessage());
            throw new Exception("Apple API 호출 실패", e);
        }

        return AppleLoginDTO.builder()
                .id(userId)
                .token(accessToken)
                .email(email)
                .build();
    }

    /**
     * Apple과 통신하기 위한 client secret 생성
     */
    private String createClientSecret() throws Exception {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(APPLE_LOGIN_KEY)
                .build();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(APPLE_TEAM_ID)
                .issueTime(new Date())
                .expirationTime(new Date(new Date().getTime() + 3600000)) // 1시간 유효
                .audience(APPLE_AUTH_URL)
                .subject(APPLE_CLIENT_ID)
                .build();

        SignedJWT jwt = new SignedJWT(header, claimsSet);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(getPrivateKey());
        KeyFactory kf = KeyFactory.getInstance("EC");
        ECPrivateKey ecPrivateKey = (ECPrivateKey) kf.generatePrivate(spec);

        JWSSigner jwsSigner = new ECDSASigner(ecPrivateKey);
        try {
            jwt.sign(jwsSigner);
        } catch (JOSEException e) {
            throw new Exception("JWT 서명 실패", e);
        }
        return jwt.serialize();
    }

    /**
     * .p8 키 파일에서 개인키를 읽어 byte[] 반환
     */
    private byte[] getPrivateKey() throws Exception {
        byte[] content;
        File file = new File(APPLE_KEY_PATH);
        URL res = getClass().getResource(APPLE_KEY_PATH);

        if (res == null) {
            file = new File(APPLE_KEY_PATH);
        } else if ("jar".equals(res.getProtocol())) {
            try (InputStream input = getClass().getResourceAsStream(APPLE_KEY_PATH)) {
                file = File.createTempFile("tempfile", ".tmp");
                try (OutputStream out = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
                file.deleteOnExit();
            }
        }

        if (file.exists()) {
            try (FileReader keyReader = new FileReader(file);
                 PemReader pemReader = new PemReader(keyReader)) {
                PemObject pemObject = pemReader.readPemObject();
                content = pemObject.getContent();
            } catch (IOException e) {
                throw new Exception("개인키 읽기 실패", e);
            }
        } else {
            throw new Exception("키 파일을 찾을 수 없습니다: " + file);
        }
        return content;
    }

    /**
     * 애플 리프레시 토큰을 이용해 엑세스 토큰 재발급
     */
    public String refreshAppleAccessToken(String refreshToken) throws Exception {
        String url = APPLE_AUTH_URL + "/auth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String clientSecret = createClientSecret();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", APPLE_CLIENT_ID);
        params.add("client_secret", clientSecret);
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
            String accessToken = result.get("access_token").toString();
            return accessToken;
        } else {
            throw new InternalServerException(ErrorStatus.FAIL_REISSUE_APPLE_OAUTH_ACCESS_TOKEN.getMessage() + response.getBody());
        }
    }

    /**
     * 애플 앱 연결 해제(토큰 리보크)
     */
    public void unlinkAppleUser(String appleAccessToken) throws Exception {
        if (appleAccessToken == null || appleAccessToken.isEmpty()) {
            throw new InternalServerException(ErrorStatus.MISSING_APPLE_OAUTH_ACCESS_TOKEN.getMessage());
        }

        String url = APPLE_AUTH_URL + "/auth/revoke";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // client secret 생성 (내부 메서드 createClientSecret() 재사용)
        String clientSecret = createClientSecret();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", APPLE_CLIENT_ID);
        params.add("client_secret", clientSecret);
        params.add("token", appleAccessToken);
        params.add("token_type_hint", "access_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new InternalServerException(ErrorStatus.FAIL_UNLINK_APPLE_OAUTH_EXCEPTION.getMessage() + response.getBody());
        }
    }
}
