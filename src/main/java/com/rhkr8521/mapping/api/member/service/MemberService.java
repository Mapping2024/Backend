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
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final JwtService jwtService;
    private final OAuthService oAuthService;
    private final S3Service s3Service;

    private static final List<String> FIRST_WORDS = Arrays.asList(
            "멍청한", "빠른", "귀여운", "화난", "배고픈", "행복한", "똑똑한", "졸린", "심술궂은", "시끄러운",
            "고요한", "차가운", "뜨거운", "용감한", "겁쟁이", "수줍은", "대담한", "게으른", "성실한", "조용한",
            "활발한", "이상한", "웃긴", "짜증난", "애매한", "창의적인", "독특한", "신나는", "졸린", "수상한",
            "무서운", "어리석은", "슬픈", "고마운", "느린", "적극적인", "부끄러운", "당당한", "예민한", "단순한"
    );

    private static final List<String> SECOND_WORDS = Arrays.asList(
            "고양이", "강아지", "토끼", "사자", "호랑이", "펭귄", "코끼리", "여우", "늑대", "곰", "너구리",
            "다람쥐", "치타", "하이에나", "고릴라", "캥거루", "햄스터", "카멜레온", "악어", "두더지", "수달",
            "부엉이", "참새", "독수리", "오리", "거북이", "물개", "돌고래", "고래", "불가사리", "미어캣",
            "해파리", "코알라", "낙타", "아기돼지", "강치", "이구아나", "오징어", "문어", "갈매기", "오소리"
    );

    private static String generateRandomNickname() {
        Random random = new Random();

        // 첫 번째 단어(형용사)와 두 번째 단어(명사)를 랜덤 선택
        String firstWord = FIRST_WORDS.get(random.nextInt(FIRST_WORDS.size()));
        String secondWord = SECOND_WORDS.get(random.nextInt(SECOND_WORDS.size()));

        // 두 자리 숫자 생성 (00~99)
        int randomNumber = random.nextInt(100); // 0~99
        String formattedNumber = String.format("%02d", randomNumber); // 두 자리로 변환 (ex: 03, 57)

        return firstWord + secondWord + "#" + formattedNumber;
    }

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
                .nickname(generateRandomNickname())
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
