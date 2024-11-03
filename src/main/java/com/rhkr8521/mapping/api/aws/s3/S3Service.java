package com.rhkr8521.mapping.api.aws.s3;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class S3Service {
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucketName}")
    private String bucketName;

    @Value("${cloud.aws.s3.domain}")
    private String domain;

    public List<String> uploadMemoImages(String userIdentifier, List<MultipartFile> files) throws IOException {
        String dir = "memo-images";
        List<String> imageUrls = new ArrayList<>();

        // 하나의 랜덤 문자열 생성
        String randomString = RandomStringUtils.randomAlphanumeric(16);

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String currentDateTime = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
            String originalFilename = file.getOriginalFilename();
            String extension = "";

            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                originalFilename = originalFilename.substring(0, originalFilename.lastIndexOf(".")); // 확장자 제거
            }

            // 파일명 구성: {원파일이름}_{currentDateTime}{extension}
            String fileName = originalFilename + "_" + currentDateTime + extension;
            // 파일 경로 구성: memo-images/{userId}/{랜덤문자}/파일명
            String fileKey = dir + "/" + userIdentifier + "/" + randomString + "/" + fileName;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .acl("public-read")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            imageUrls.add(domain + "/" + fileKey);
        }

        return imageUrls;
    }
}
