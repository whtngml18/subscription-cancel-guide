# 1단계: Gradle로 실행 파일(jar) 만들기
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성을 먼저 받아 두면 소스만 바뀌었을 때 다시 받지 않아 빌드가 빨라진다
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# 2단계: 실행에 필요한 JRE와 jar만 담기
FROM eclipse-temurin:21-jre
WORKDIR /app

# 제보 시각 등이 한국 시간으로 저장되게 한다
ENV TZ=Asia/Seoul
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
