package com.rhkr8521.mapping.api.member.service;

import com.rhkr8521.mapping.api.aws.s3.S3Service;
import com.rhkr8521.mapping.api.member.dto.KakaoUserInfoDTO;
import com.rhkr8521.mapping.api.member.dto.UserInfoResponseDTO;
import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.api.member.entity.Role;
import com.rhkr8521.mapping.api.member.jwt.service.JwtService;
import com.rhkr8521.mapping.api.member.repository.MemberRepository;
import com.rhkr8521.mapping.common.exception.BadRequestException;
import com.rhkr8521.mapping.common.exception.NotFoundException;
import com.rhkr8521.mapping.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final JwtService jwtService;
    private final OAuthService oAuthService;
    private final S3Service s3Service;

    @Transactional
    public Map<String, Object> loginWithKakao(String kakaoAccessToken) {
        // 카카오 Access Token을 이용해 사용자 정보 가져오기
        KakaoUserInfoDTO kakaoUserInfo = oAuthService.getKakaoUserInfo(kakaoAccessToken);

        // 사용자 정보를 저장
        Member member = registerOrLoginKakaoUser(kakaoUserInfo);

        // 엑세스,리프레시 토큰 생성
        Map<String, String> tokens = jwtService.createAccessAndRefreshToken(member.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("tokens", tokens);
        response.put("role", member.getRole());
        response.put("nickname", member.getNickname());
        response.put("profileImage", member.getImageUrl());
        response.put("socialId", member.getSocialId());

        return response;
    }

    // 카카오 사용자 정보를 사용해 회원가입 또는 로그인 처리
    public Member registerOrLoginKakaoUser(KakaoUserInfoDTO kakaoUserInfo) {
        // 카카오 사용자 ID로 사용자 조회
        return memberRepository.findBySocialId(kakaoUserInfo.getId())
                .orElseGet(() -> registerNewKakaoUser(kakaoUserInfo));  // 없으면 새 사용자 등록
    }

    // 새로운 카카오 사용자 등록
    private Member registerNewKakaoUser(KakaoUserInfoDTO kakaoUserInfo) {
        Member member = Member.builder()
                .socialId(kakaoUserInfo.getId())
                .email(UUID.randomUUID() + "@socialUser.com")
                .nickname(kakaoUserInfo.getNickname())
                .imageUrl(kakaoUserInfo.getProfileImage())
                .role(Role.USER)
                .build();

        memberRepository.save(member);

        return member;
    }

    @Transactional(readOnly = true)
    public Long getUserIdByEmail(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));
        return member.getId();
    }

    @Transactional
    public void changeNickname(Long userId, String nickname) {
        // 유저 조회 및 닉네임 변경 로직
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));

        Member updatedMember = member.updateNickname(nickname);
        memberRepository.save(updatedMember); // Member 객체 반환
    }

    @Transactional
    public void updateProfileImage(Long userId, MultipartFile image) throws IOException {
        // 해당 유저를 찾을 수 없을 경우 예외처리
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));

        // 기존 이미지가 S3에 있는 경우 삭제
        s3Service.deleteFile(member.getImageUrl());

        // 새로운 이미지 업로드
        String imageUrl = s3Service.uploadProfileImage(member.getEmail(), image);

        Member updatedMember = member.updateImageUrl(imageUrl);
        memberRepository.save(updatedMember);
    }

    @Transactional(readOnly = true)
    public UserInfoResponseDTO getUserInfo(Long userId) {
        // 해당 유저를 찾을 수 없을 경우 예외처리
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));

        return new UserInfoResponseDTO(member);
    }

}
