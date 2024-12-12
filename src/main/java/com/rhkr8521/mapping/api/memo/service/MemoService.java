package com.rhkr8521.mapping.api.memo.service;

import com.rhkr8521.mapping.api.aws.s3.S3Service;
import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.api.member.repository.MemberRepository;
import com.rhkr8521.mapping.api.member.service.MemberService;
import com.rhkr8521.mapping.api.memo.dto.*;
import com.rhkr8521.mapping.api.memo.entity.Memo;
import com.rhkr8521.mapping.api.memo.entity.MemoHate;
import com.rhkr8521.mapping.api.memo.entity.MemoImage;
import com.rhkr8521.mapping.api.memo.entity.MemoLike;
import com.rhkr8521.mapping.api.memo.repository.MemoHateRepository;
import com.rhkr8521.mapping.api.memo.repository.MemoLikeRepository;
import com.rhkr8521.mapping.api.memo.repository.MemoRepository;
import com.rhkr8521.mapping.common.exception.BadRequestException;
import com.rhkr8521.mapping.common.exception.NotFoundException;
import com.rhkr8521.mapping.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemoService {
    private final MemoRepository memoRepository;
    private final MemoLikeRepository memoLikeRepository;
    private final MemoHateRepository memoHateRepository;
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
                .isPublic(memoRequest.isPublic())
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
        List<Memo> memos = memoRepository.findMemosWithinRadius(lat, lng, km)
                .stream()
                .filter(Memo::isPublic)
                .toList();

        return memos.stream()
                .map(memo -> new MemoTotalListResponseDTO(memo.getId(), memo.getTitle(), memo.getCategory(), memo.getLat(), memo.getLng()))
                .collect(Collectors.toList());
    }

    // 메모 상세 조회
    public MemoDetailResponseDTO getMemoDetail(Long memoId, UserDetails userDetails) {
        Memo memo = memoRepository.findById(memoId)
                .filter(Memo::isPublic)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage()));

        boolean myMemo = false;
        boolean myLike = false;
        boolean myHate = false;

        if (userDetails != null) {
            Long userId = memberService.getUserIdByEmail(userDetails.getUsername());
            myLike = memoLikeRepository.findByMemoIdAndMemberId(memoId, userId).isPresent();
            myHate = !myLike && memoHateRepository.findByMemoIdAndMemberId(memoId, userId).isPresent();
            myMemo = memo.getMember().getId().equals(userId);
        }

        List<String> imageUrls = memo.getImages().isEmpty() ? null :
                memo.getImages().stream().map(MemoImage::getImageUrl).collect(Collectors.toList());

        // 날짜 포맷팅
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd:HH:mm:ss");
        String formattedDate = memo.getUpdatedAt().format(formatter);

        return MemoDetailResponseDTO.builder()
                .id(memo.getId())
                .title(memo.getTitle())
                .date(formattedDate)
                .content(memo.getContent())
                .likeCnt(memo.getLikeCnt())
                .hateCnt(memo.getHateCnt())
                .images(imageUrls)
                .lat(memo.getLat())
                .lng(memo.getLng())
                .category(memo.getCategory())
                .myMemo(myMemo)
                .myLike(myLike)
                .myHate(myHate)
                .authorId(memo.getMember().getId())
                .nickname(memo.getMember().getNickname())
                .profileImage(memo.getMember().getImageUrl())
                .build();
    }

    // 내가 작성한 메모 조회
    public List<MyMemoListResponseDTO> getMyMemoList(Long userId){

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

    // 프라이빗 메모 전체 조회
    public List<MemoTotalListResponseDTO> getPrivateMemosWithinRadius(Long userId, double lat, double lng, double km) {

        List<Memo> privateMemos = memoRepository.findMemosWithinRadius(lat, lng, km)
                .stream()
                .filter(memo -> !memo.isPublic() && memo.getMember().getId().equals(userId)) // 본인이 작성한 프라이빗 메모만 필터링
                .toList();

        return privateMemos.stream()
                .map(memo -> new MemoTotalListResponseDTO(memo.getId(), memo.getTitle(), memo.getCategory(), memo.getLat(), memo.getLng()))
                .collect(Collectors.toList());
    }

    // 프라이빗 메모 상세 조회
    public MemoPrivateDetailResponseDTO getPrivateMemoDetail(Long userId, Long memoId) {

        // 메모 조회 및 작성자 확인
        Memo memo = memoRepository.findById(memoId)
                .filter(m -> !m.isPublic() && m.getMember().getId().equals(userId)) // 프라이빗 메모 여부와 작성자 확인
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage()));

        List<String> imageUrls = memo.getImages().isEmpty() ? null :
                memo.getImages().stream().map(MemoImage::getImageUrl).collect(Collectors.toList());

        // 날짜 포맷팅
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd:HH:mm:ss");
        String formattedDate = memo.getUpdatedAt().format(formatter);

        // 상세 조회 응답 DTO 생성
        return MemoPrivateDetailResponseDTO.builder()
                .id(memo.getId())
                .title(memo.getTitle())
                .date(formattedDate)
                .content(memo.getContent())
                .images(imageUrls)
                .lat(memo.getLat())
                .lng(memo.getLng())
                .category(memo.getCategory())
                .authorId(memo.getMember().getId())
                .nickname(memo.getMember().getNickname())
                .profileImage(memo.getMember().getImageUrl())
                .build();
    }

    // 메모 삭제
    public void deleteMemo(Long memoId, Long userId) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage()));

        // 게시글 작성자와 삭제 요청자가 다를 경우 예외 처리
        if (!memo.getMember().getId().equals(userId)) {
            throw new NotFoundException(ErrorStatus.MEMO_WRITER_NOT_SAME_USER_EXCEPTION.getMessage());
        }

        memoRepository.delete(memo);
    }

    // 메모 수정
    public void updateMemo(Long memoId, Long userId, MemoCreateRequestDTO memoRequest,
                           List<MultipartFile> newImages, List<String> deleteImageUrls) throws IOException {

        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage()));

        // 메모 작성자 확인
        if (!memo.getMember().getId().equals(userId)) {
            throw new BadRequestException(ErrorStatus.MEMO_WRITER_NOT_SAME_USER_EXCEPTION.getMessage());
        }

        // 메모 정보 업데이트 (Builder 패턴 사용)
        Memo updatedMemo = Memo.builder()
                .id(memo.getId())
                .member(memo.getMember())
                .title(memoRequest.getTitle())
                .content(memoRequest.getContent())
                .lat(memo.getLat())
                .lng(memo.getLng())
                .category(memoRequest.getCategory())
                .likeCnt(memo.getLikeCnt())
                .hateCnt(memo.getHateCnt())
                .ip(memo.getIp())
                .images(new ArrayList<>(memo.getImages()))
                .build();

        // 삭제할 이미지 처리
        if (deleteImageUrls != null && !deleteImageUrls.isEmpty()) {
            List<MemoImage> imagesToRemove = memo.getImages().stream()
                    .filter(image -> deleteImageUrls.contains(image.getImageUrl()))
                    .toList();

            for (MemoImage image : imagesToRemove) {
                // S3에서 이미지 삭제
                s3Service.deleteFile(image.getImageUrl());
                // 메모에서 이미지 제거
                updatedMemo.getImages().remove(image);
            }
        }

        // 새로운 이미지 업로드 및 추가
        if (newImages != null && !newImages.isEmpty()) {
            List<String> imageUrls = s3Service.uploadMemoImages(String.valueOf(userId), newImages);
            for (String url : imageUrls) {
                MemoImage image = MemoImage.builder()
                        .imageUrl(url)
                        .memo(updatedMemo)
                        .build();
                updatedMemo.getImages().add(image);
            }
        }

        memoRepository.save(updatedMemo);
    }

    // 좋아요 토글
    public void toggleLike(Long memoId, Long userId) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage()));

        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));

        // 좋아요 상태 확인
        Optional<MemoLike> existingLike = memoLikeRepository.findByMemoIdAndMemberId(memoId, userId);

        if (existingLike.isPresent()) {
            // 좋아요를 취소
            memoLikeRepository.delete(existingLike.get());
            memo = memo.decreaseLikeCnt();
        } else {
            // 싫어요를 취소 (상호 배타성 보장)
            Optional<MemoHate> existingHate = memoHateRepository.findByMemoIdAndMemberId(memoId, userId);
            if (existingHate.isPresent()) {
                memoHateRepository.delete(existingHate.get());
                memo = memo.decreaseHateCnt();
            }

            // 좋아요 추가
            MemoLike memoLike = MemoLike.builder()
                    .memo(memo)
                    .member(member)
                    .build();
            memoLikeRepository.save(memoLike);
            memo = memo.increaseLikeCnt();
        }

        memoRepository.save(memo);
    }

    // 싫어요 토글
    public void toggleHate(Long memoId, Long userId) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage()));

        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));

        // 싫어요 상태 확인
        Optional<MemoHate> existingHate = memoHateRepository.findByMemoIdAndMemberId(memoId, userId);

        if (existingHate.isPresent()) {
            // 싫어요를 취소
            memoHateRepository.delete(existingHate.get());
            memo = memo.decreaseHateCnt();
        } else {
            // 좋아요를 취소 (상호 배타성 보장)
            Optional<MemoLike> existingLike = memoLikeRepository.findByMemoIdAndMemberId(memoId, userId);
            if (existingLike.isPresent()) {
                memoLikeRepository.delete(existingLike.get());
                memo = memo.decreaseLikeCnt();
            }

            // 싫어요 추가
            MemoHate memoHate = MemoHate.builder()
                    .memo(memo)
                    .member(member)
                    .build();
            memoHateRepository.save(memoHate);
            memo = memo.increaseHateCnt();
        }

        memoRepository.save(memo);
    }

}
