package com.rhkr8521.mapping.slack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackNotificationService {

    @Value("${slack.webhook.url}")
    private String slackWebhookUrl;

    /**
     * 슬랙에 간단한 텍스트 메시지를 보내는 메서드
     */
    public void sendSlackMessage(String message) {
        try {
            RestTemplate restTemplate = new RestTemplate(getClientHttpRequestFactory());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String payload = "{\"text\": \"" + message + "\"}";

            HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            restTemplate.postForObject(slackWebhookUrl, entity, String.class);

        } catch (Exception e) {
            // Slack 전송 중 예외가 발생해도 서비스 로직에 영향 주지 않도록 로그만 남기고 처리
            log.error("슬랙 메시지 전송 실패: {}", e.getMessage());
        }
    }

    private SimpleClientHttpRequestFactory getClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 필요하다면 타임아웃 설정
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        return factory;
    }

    /**
     * 회원가입 알림
     * 예) "[회원] 1번째 사용자가 가입하였습니다."
     */
    public void sendMemberRegistrationMessage(Long memberId) {
        String message = String.format("[회원] %d번째 사용자가 가입하였습니다.", memberId);
        sendSlackMessage(message);
    }

    /**
     * 서버 오류 알림
     * 예) "[오류] 에러 내용"
     */
    public void sendServerErrorMessage(String errorMessage) {
        String message = String.format("[오류] %s", errorMessage);
        sendSlackMessage(message);
    }

    /**
     * 신고 알림
     * 예) "[신고] 메모 신고 (ID: 2) - 음란물입니다."
     */
    public void sendReportMessage(String reportType, Long memoId, String reportDescription) {
        // enum의 getDescription() 결과를 받아서 사용
        String message = String.format("[신고] %s 신고 (ID: %d) - %s", reportType, memoId, reportDescription);
        sendSlackMessage(message);
    }
}
