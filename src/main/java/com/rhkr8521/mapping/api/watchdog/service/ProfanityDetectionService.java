package com.rhkr8521.mapping.api.watchdog.service;

import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.api.watchdog.dto.ProfanityResponseDTO;
import com.rhkr8521.mapping.api.watchdog.entity.ProfanityDetect;
import com.rhkr8521.mapping.api.watchdog.repository.ProfanityDetectRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ProfanityDetectionService {

    private final ProfanityDetectRepository profanityDetectRepository;

    @Value("${watchdog.profanity.url}")
    private String profanityUrl;

    private final RestTemplate restTemplate;

    // 생성자에서 RestTemplate과 ProfanityDetectRepository 모두 주입받음
    public ProfanityDetectionService(RestTemplateBuilder builder, ProfanityDetectRepository profanityDetectRepository) {
        this.restTemplate = builder.build();
        this.profanityDetectRepository = profanityDetectRepository;
    }

    // 외부 API에 텍스트를 전달하여 비속어 검증 결과를 반환
    public ProfanityResponseDTO checkText(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 요청 바디 생성: {"text": "입력 텍스트"}
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text", text);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<ProfanityResponseDTO> responseEntity =
                restTemplate.postForEntity(profanityUrl, requestEntity, ProfanityResponseDTO.class);

        return responseEntity.getBody();
    }

    // 검증 결과에 따라 ProfanityDetect 엔티티를 생성 및 저장
    @Transactional
    public void saveProfanityDetect(Member member, String originalText, ProfanityResponseDTO response) {
        String detectedWordsStr = String.join(",", response.getDetectedWords());

        ProfanityDetect profanityDetect = ProfanityDetect.builder()
                .member(member)
                .originalText(originalText)
                .censoredText(response.getCensoredText())
                .detectedWords(detectedWordsStr)
                .build();

        profanityDetectRepository.save(profanityDetect);
    }

    // 멤버 정보를 포함해 텍스트 검증 후, 응답이 true일 경우 DB 저장까지 처리하는 메서드
    @Transactional
    public ProfanityResponseDTO checkTextAndSave(Member member, String text) {
        ProfanityResponseDTO response = checkText(text);
        if (response.isContainsProfanity()) {
            saveProfanityDetect(member, text, response);
        }
        return response;
    }
}
