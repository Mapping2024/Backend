package com.rhkr8521.mapping.api.memo.service;

import com.rhkr8521.mapping.api.aws.s3.S3Service;
import com.rhkr8521.mapping.api.comment.repository.CommentLikeRepository;
import com.rhkr8521.mapping.api.comment.repository.CommentRepository;
import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.api.member.repository.MemberRepository;
import com.rhkr8521.mapping.api.member.service.MemberService;
import com.rhkr8521.mapping.api.memo.dto.*;
import com.rhkr8521.mapping.api.memo.entity.*;
import com.rhkr8521.mapping.api.memo.repository.MemoHateRepository;
import com.rhkr8521.mapping.api.memo.repository.MemoLikeRepository;
import com.rhkr8521.mapping.api.memo.repository.MemoReportRepository;
import com.rhkr8521.mapping.api.memo.repository.MemoRepository;
import com.rhkr8521.mapping.common.exception.BadRequestException;
import com.rhkr8521.mapping.common.exception.NotFoundException;
import com.rhkr8521.mapping.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemoService {
    private final MemoRepository memoRepository;
    private final MemoLikeRepository memoLikeRepository;
    private final MemoHateRepository memoHateRepository;
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final MemoReportRepository memoReportRepository;
    private final MemberService memberService;
    private final S3Service s3Service;

    // 메모 생성
    public void createMemo(Long userId, MemoCreateRequestDTO memoRequest, List<MultipartFile> images, HttpServletRequest request) throws IOException {

        // 해당 유저를 찾을 수 없을 경우 예외처리
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));

        // 접속 IP 추출
        String clientIp = extractClientIp(request);

        // 인증 여부 판단
        boolean certified = false;
        if (!memoRequest.isSecret()) {
            double distanceKm = calculateDistance(memoRequest.getLat(), memoRequest.getLng(), memoRequest.getCurrentLat(), memoRequest.getCurrentLng());
            // 10m = 0.01km
            if (distanceKm <= 0.1) {
                certified = true;
            }
        }

        Memo memo = Memo.builder()
                .member(member)
                .title(memoRequest.getTitle())
                .content(memoRequest.getContent())
                .lat(memoRequest.getLat())
                .lng(memoRequest.getLng())
                .category(memoRequest.getCategory())
                .likeCnt(0)
                .hateCnt(0)
                .createIp(clientIp)
                .lastModifyIp(null)
                .secret(memoRequest.isSecret())
                .certified(certified)
                .modify(false)
                .isHidden(false)
                .isDeleted(false)
                .build();

        // 이미지 처리
        if (images != null && !images.isEmpty()) {

            List<String> imageUrls = s3Service.uploadMemoImages(String.valueOf(userId), images);
            memo.addImages(imageUrls);
        }

        memoRepository.save(memo);
    }

    // 거리 계산 메서드(단위: km)
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = earthRadius * c;
        return dist;
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

    // 전체 메모 조회(공개 + 비공개)
    public List<MemoTotalListResponseDTO> getMemosWithinRadius(double lat, double lng, double km, UserDetails userDetails) {
        List<Memo> allMemos = memoRepository.findMemosWithinRadius(lat, lng, km);
        final List<Long> blockedIds;

        if (userDetails != null) {
            Long userId = memberService.getUserIdByEmail(userDetails.getUsername());
            blockedIds = memberService.getBlockedUserIds(userId);
        } else {
            blockedIds = Collections.emptyList();
        }

        // 공개 메모 필터: 차단된 사용자의 메모는 제외
        List<Memo> publicMemos = allMemos.stream()
                .filter(m -> !m.isSecret())
                .filter(m -> blockedIds.isEmpty() || !blockedIds.contains(m.getMember().getId()))
                .toList();

        List<Memo> privateMemos = new ArrayList<>();
        if (userDetails != null) {
            Long userId = memberService.getUserIdByEmail(userDetails.getUsername());
            privateMemos = allMemos.stream()
                    .filter(m -> m.isSecret() && m.getMember().getId().equals(userId))
                    .toList();
        }

        // 공개 메모와 내 프라이빗 메모 합치기
        List<Memo> combinedMemos = new ArrayList<>(publicMemos);
        combinedMemos.addAll(privateMemos);

        return combinedMemos.stream()
                .map(memo -> new MemoTotalListResponseDTO(
                        memo.getId(),
                        memo.getTitle(),
                        memo.getCategory(),
                        memo.getLat(),
                        memo.getLng(),
                        memo.isCertified(),
                        memo.isSecret()))
                .collect(Collectors.toList());
    }

    // 메모 상세 조회
    public MemoDetailResponseDTO getMemoDetail(Long memoId, UserDetails userDetails) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage()));

        // 삭제된 메모인 경우
        if (memo.isDeleted()) {
            throw new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage());
        }

        // 유저가 로그인한 경우 차단한 사용자의 메모라면 조회 못하도록 처리
        if (userDetails != null) {
            Long userId = memberService.getUserIdByEmail(userDetails.getUsername());
            final List<Long> blockedIds = memberService.getBlockedUserIds(userId);
            if (!blockedIds.isEmpty() && blockedIds.contains(memo.getMember().getId())) {
                throw new BadRequestException(ErrorStatus.CANT_ACCESS_BLOCK_USER_MEMO_EXCEPTION.getMessage());
            }
        }

        // 프라이빗 메모인 경우 접근 권한 체크
        if (memo.isSecret()) {
            if (userDetails == null) {
                throw new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage());
            }
            Long userId = memberService.getUserIdByEmail(userDetails.getUsername());
            if (!memo.getMember().getId().equals(userId)) {
                throw new NotFoundException(ErrorStatus.INVALID_VIEW_AUTH.getMessage());
            }
        }

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
        String formattedDate = memo.getCreatedAt().format(formatter);

        String nickname = memo.getMember().isDeleted() ? "(알수없음)" : memo.getMember().getNickname();

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
                .nickname(nickname)
                .profileImage(memo.getMember().getImageUrl())
                .certified(memo.isCertified())
                .modify(memo.isModify())
                .build();
    }

    // 내가 작성한 메모 조회
    public List<MyMemoListResponseDTO> getMyMemoList(Long userId){
        List<Memo> myMemos = memoRepository.findByMemberIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

        return myMemos.stream()
                .map(memo -> new MyMemoListResponseDTO(
                        memo.getId(),
                        memo.getTitle(),
                        memo.getContent(),
                        memo.getCategory(),
                        memo.getLikeCnt(),
                        memo.getHateCnt(),
                        memo.getImages().stream().map(MemoImage::getImageUrl).collect(Collectors.toList()),
                        memo.isSecret()
                )).collect(Collectors.toList());
    }

    // 댓글 삭제(하드삭제)
//    @Transactional
//    public void deleteComment(Long commentId, Long userId) {
//        // 댓글을 ID로 찾고, 존재하지 않으면 예외 처리
//        Comment comment = commentRepository.findById(commentId)
//                .orElseThrow(() -> new NotFoundException(ErrorStatus.COMMENT_NOTFOUND_EXCPETION.getMessage()));
//
//        // 해당 댓글의 작성자가 요청한 사용자와 동일한지 검증
//        if (!comment.getMember().getId().equals(userId)) {
//            throw new UnauthorizedException(ErrorStatus.INVALID_DELETE_AUTH.getMessage());
//        }
//
//        // CommentLike 삭제
//        commentLikeRepository.deleteAllByCommentId(commentId);
//
//        commentRepository.delete(comment);
//    }

    // 메모 삭제(소프트삭제)
    @Transactional
    public void deleteMemo(Long memoId, Long userId, HttpServletRequest request) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage()));

        // 게시글 작성자와 삭제 요청자가 다를 경우 예외 처리
        if (!memo.getMember().getId().equals(userId)) {
            throw new NotFoundException(ErrorStatus.MEMO_WRITER_NOT_SAME_USER_EXCEPTION.getMessage());
        }

        // 접속 IP 추출
        String clientIp = extractClientIp(request);

        Memo deletedMemo = memo.toBuilder()
                .isDeleted(true)
                .lastModifyIp(clientIp)
                .build();

        memoRepository.save(deletedMemo);
    }

    // 메모 수정
    public void updateMemo(Long memoId, Long userId, MemoCreateRequestDTO memoRequest,
                           List<MultipartFile> newImages, List<String> deleteImageUrls,
                           HttpServletRequest request) throws IOException {

        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage()));

        // 메모 작성자 확인
        if (!memo.getMember().getId().equals(userId)) {
            throw new BadRequestException(ErrorStatus.MEMO_WRITER_NOT_SAME_USER_EXCEPTION.getMessage());
        }

        // 접속 IP 추출
        String clientIp = extractClientIp(request);

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
                .lastModifyIp(clientIp)
                .createIp(memo.getCreateIp())
                .images(new ArrayList<>(memo.getImages()))
                .modify(true)
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
    @Transactional
    public void toggleLike(Long memoId, Long userId) {
        // 메모와 회원 존재 여부 체크
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage()));
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));

        // 좋아요 상태 확인
        Optional<MemoLike> existingLike = memoLikeRepository.findByMemoIdAndMemberId(memoId, userId);

        if (existingLike.isPresent()) {
            // 좋아요 취소
            memoLikeRepository.delete(existingLike.get());
            memoRepository.decrementLikeCount(memoId);
        } else {
            // 싫어요 상태 확인 (상호 배타성 보장)
            Optional<MemoHate> existingHate = memoHateRepository.findByMemoIdAndMemberId(memoId, userId);
            if (existingHate.isPresent()) {
                memoHateRepository.delete(existingHate.get());
                memoRepository.decrementHateCount(memoId);
            }
            // 좋아요 추가
            MemoLike memoLike = MemoLike.builder()
                    .memo(memo)
                    .member(member)
                    .build();
            memoLikeRepository.save(memoLike);
            memoRepository.incrementLikeCount(memoId);
        }
    }

    // 싫어요 토글
    @Transactional
    public void toggleHate(Long memoId, Long userId) {
        // 메모와 회원 존재 여부 체크
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage()));
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));

        // 싫어요 상태 확인
        Optional<MemoHate> existingHate = memoHateRepository.findByMemoIdAndMemberId(memoId, userId);

        if (existingHate.isPresent()) {
            // 싫어요 취소
            memoHateRepository.delete(existingHate.get());
            memoRepository.decrementHateCount(memoId);
        } else {
            // 좋아요 상태 확인 (상호 배타성 보장)
            Optional<MemoLike> existingLike = memoLikeRepository.findByMemoIdAndMemberId(memoId, userId);
            if (existingLike.isPresent()) {
                memoLikeRepository.delete(existingLike.get());
                memoRepository.decrementLikeCount(memoId);
            }
            // 싫어요 추가
            MemoHate memoHate = MemoHate.builder()
                    .memo(memo)
                    .member(member)
                    .build();
            memoHateRepository.save(memoHate);
            memoRepository.incrementHateCount(memoId);
        }
    }

    // 내가 댓글 작성한 메모 목록 조회
    @Transactional(readOnly = true)
    public List<MemoListResponseDTO> getMemosWithMyComments(UserDetails userDetails) {
        Long userId = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()))
                .getId();

        List<Memo> memos = commentRepository.findDistinctMemoByMemberId(userId);

        return memos.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 내가 좋아요 누른 메모 목록 조회
    @Transactional(readOnly = true)
    public List<MemoListResponseDTO> getMemosILiked(UserDetails userDetails) {
        Long userId = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()))
                .getId();

        List<Memo> likedMemos = memoLikeRepository.findMemosByMemberId(userId);

        return likedMemos.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Memo -> MemoListResponseDTO 변환
    private MemoListResponseDTO convertToDTO(Memo memo) {
        List<String> imageUrls = memo.getImages().stream()
                .map(MemoImage::getImageUrl)
                .collect(Collectors.toList());

        return MemoListResponseDTO.builder()
                .id(memo.getId())
                .title(memo.getTitle())
                .content(memo.getContent())
                .category(memo.getCategory())
                .likeCnt(memo.getLikeCnt())
                .hateCnt(memo.getHateCnt())
                .images(imageUrls)
                .build();
    }

    // 메모 신고 기능
    @Transactional
    public void reportMemo(MemoReportRequestDTO reportRequest, UserDetails userDetails) {
        if(reportRequest.getMemoId() == null || reportRequest.getReportReason() == null) {
            throw new BadRequestException(ErrorStatus.VALIDATION_CONTENT_MISSING_EXCEPTION.getMessage());
        }

        // 신고할 메모 조회
        Memo memo = memoRepository.findById(reportRequest.getMemoId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage()));

        // 신고하는 회원 조회
        Long userId = memberService.getUserIdByEmail(userDetails.getUsername());
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));

        // 이미 신고한 내역이 있는지 체크
        if (memoReportRepository.existsByMemoAndMember(memo, member)) {
            throw new BadRequestException(ErrorStatus.ALREADY_REPORT_MEMO_EXCEPTION.getMessage());
        }

        MemoReport memoReport = MemoReport.builder()
                .memo(memo)
                .member(member)
                .reportReason(reportRequest.getReportReason())
                .build();

        memoReportRepository.save(memoReport);
    }

}
