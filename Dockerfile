FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw -DskipTests package

EXPOSE 8080

CMD ["java", "-jar", "target/revenuesync-0.0.1-SNAPSHOT.jar"]
