FROM openjdk:8-jdk-alpine
VOLUME /tmp
WORKDIR /opt/backend
COPY build/libs/crowdfunding-backend-0.0.1.jar crowdfunding-backend.jar
EXPOSE 8123
ENV spring_profiles_active=dev
ENTRYPOINT ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=dev", "crowdfunding-backend.jar"]
