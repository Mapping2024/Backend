package com.rhkr8521.mapping.api.memo.service;

import com.rhkr8521.mapping.api.aws.s3.S3Service;
import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.api.member.repository.MemberRepository;
import com.rhkr8521.mapping.api.memo.dto.MemoCreateRequestDTO;
import com.rhkr8521.mapping.api.memo.dto.MemoTotalListResponseDTO;
import com.rhkr8521.mapping.api.memo.entity.Memo;
import com.rhkr8521.mapping.api.memo.repository.MemoRepository;
import com.rhkr8521.mapping.common.exception.NotFoundException;
import com.rhkr8521.mapping.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemoService {
    private final MemoRepository memoRepository;
    private final MemberRepository memberRepository;
    private final S3Service s3Service;

    // 메모 생성
    public void createMemo(Long userId, MemoCreateRequestDTO memoRequest, List<MultipartFile> images, HttpServletRequest request) throws IOException {

        // 해당 유저를 찾을 수 없을 경우 예외처리
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));

        // 접속 IP 추출
        String clientIp = extractClientIp(request);

        Memo memo = Memo.builder()
                .member(member)
                .title(memoRequest.getTitle())
                .content(memoRequest.getContent())
                .lat(memoRequest.getLat())
                .lng(memoRequest.getLng())
                .category(memoRequest.getCategory())
                .likeCnt(0)
                .hateCnt(0)
                .ip(clientIp)
                .build();

        // 이미지 처리
        if (images != null && !images.isEmpty()) {

            List<String> imageUrls = s3Service.uploadMemoImages(String.valueOf(userId), images);
            memo.addImages(imageUrls);
        }

        memoRepository.save(memo);
    }

    // 클라이언트 IP 추출 메소드
    private String extractClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("WL-Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }
        return clientIp;
    }

    // 전체 메모 조회
    public List<MemoTotalListResponseDTO> getMemosWithinRadius(double lat, double lng, double km) {

        // Haversine formula를 사용하여 특정 반경 내 메모를 조회
        double earthRadiusKm = 6371.0;

        List<Memo> memos = memoRepository.findAll().stream()
                .filter(memo -> {
                    double dLat = Math.toRadians(memo.getLat() - lat);
                    double dLng = Math.toRadians(memo.getLng() - lng);

                    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                            Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(memo.getLat())) *
                                    Math.sin(dLng / 2) * Math.sin(dLng / 2);
                    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

                    double distance = earthRadiusKm * c;
                    return distance <= km;
                })
                .collect(Collectors.toList());

        return memos.stream()
                .map(memo -> new MemoTotalListResponseDTO(memo.getId(), memo.getTitle(), memo.getCategory()))
                .collect(Collectors.toList());
    }
}
