FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew ./
COPY gradle/ gradle/
RUN chmod +x gradlew

COPY settings.gradle ./
COPY my-blog-backend-app/build.gradle my-blog-backend-app/
RUN ./gradlew :my-blog-backend-app:dependencies --no-daemon --quiet

COPY my-blog-backend-app/src/ my-blog-backend-app/src/
RUN ./gradlew :my-blog-backend-app:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/my-blog-backend-app/build/libs/*.jar app.jar

VOLUME /app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--app.upload.dir=/app/uploads/"]
