# Dockerfile

# jdk17 Image Start
FROM openjdk:17

ARG JAR_FILE=build/libs/mapping-0.0.1-SNAPSHOT.jar
ADD ${JAR_FILE} mapping_Backend.jar
ENTRYPOINT ["java","-jar","-Duser.timezone=Asia/Seoul","mapping_Backend.jar"]