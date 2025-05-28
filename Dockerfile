# Dockerfile

# 1. JDK 17 베이스 이미지
FROM openjdk:17

# 2. JAR 파일 복사
ARG JAR_FILE=build/libs/mapping-0.0.1-SNAPSHOT.jar
ADD ${JAR_FILE} mapping_Backend.jar

# 3. 키 파일을 저장할 디렉터리 생성 및
#    APPLE_AUTH_KEY_P8 환경변수를 파일로 덤프한 뒤
#    애플리케이션 실행
ENTRYPOINT ["/bin/sh", "-c", "\
  mkdir -p /app/key && \
  echo \"$APPLE_AUTH_KEY_P8\" > /app/key/AuthKey.p8 && \
  exec java -jar -Duser.timezone=Asia/Seoul mapping_Backend.jar \
"]
