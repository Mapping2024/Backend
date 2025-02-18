# Dockerfile

# jdk17 Image Start
FROM openjdk:17

ARG JAR_FILE=build/libs/mapping-0.0.1-SNAPSHOT.jar
ADD ${JAR_FILE} mapping_Backend.jar

COPY /home/ubuntu/key/AuthKey.p8 /app/key/AuthKey.p8
ENTRYPOINT ["java","-jar","-Duser.timezone=Asia/Seoul","mapping_Backend.jar"]