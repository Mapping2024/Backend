package com.rhkr8521.mapping.api.memo.service;

import com.rhkr8521.mapping.api.aws.s3.S3Service;
import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.api.member.repository.MemberRepository;
import com.rhkr8521.mapping.api.member.service.MemberService;
import com.rhkr8521.mapping.api.memo.dto.MemoCreateRequestDTO;
import com.rhkr8521.mapping.api.memo.dto.MemoDetailResponseDTO;
import com.rhkr8521.mapping.api.memo.dto.MemoTotalListResponseDTO;
import com.rhkr8521.mapping.api.memo.dto.MyMemoListResponseDTO;
import com.rhkr8521.mapping.api.memo.entity.Memo;
import com.rhkr8521.mapping.api.memo.entity.MemoImage;
import com.rhkr8521.mapping.api.memo.repository.MemoRepository;
import com.rhkr8521.mapping.common.exception.NotFoundException;
import com.rhkr8521.mapping.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final MemberService memberService;
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
        List<Memo> memos = memoRepository.findMemosWithinRadius(lat, lng, km);

        return memos.stream()
                .map(memo -> new MemoTotalListResponseDTO(memo.getId(), memo.getTitle(), memo.getCategory(), memo.getLat(), memo.getLng()))
                .collect(Collectors.toList());
    }

    // 메모 상세 조회
    public MemoDetailResponseDTO getMemoDetail(Long memoId, UserDetails userDetails) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage()));

        boolean myMemo = false;
        if (userDetails != null) {
            Long userId = memberService.getUserIdByEmail(userDetails.getUsername());
            myMemo = memo.getMember().getId().equals(userId);
        }

        List<String> imageUrls = memo.getImages().isEmpty() ? null :
                memo.getImages().stream().map(MemoImage::getImageUrl).collect(Collectors.toList());

        return MemoDetailResponseDTO.builder()
                .id(memo.getId())
                .title(memo.getTitle())
                .content(memo.getContent())
                .likeCnt(memo.getLikeCnt())
                .hateCnt(memo.getHateCnt())
                .images(imageUrls)
                .myMemo(myMemo)
                .authorId(memo.getMember().getId())
                .nickname(memo.getMember().getNickname())
                .profileImage(memo.getMember().getImageUrl())
                .build();
    }

    // 내가 작성한 메모 조회
    public List<MyMemoListResponseDTO> getMyMemoList(UserDetails userDetails){

        // 현재 사용자 조회
        Long userId = memberService.getUserIdByEmail(userDetails.getUsername());

        // 사용자 작성 메모 조회
        List<Memo> myMemos = memoRepository.findMemosByMemberId(userId);

        // DTO 변환 및 반환
        return myMemos.stream()
                .map(memo -> new MyMemoListResponseDTO(
                        memo.getId(),
                        memo.getTitle(),
                        memo.getContent(),
                        memo.getCategory(),
                        memo.getLikeCnt(),
                        memo.getHateCnt(),
                        memo.getImages().stream().map(MemoImage::getImageUrl).collect(Collectors.toList())
                )).collect(Collectors.toList());
    }
}
