FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY pom-parent.xml ./pom-parent.xml
COPY firmador-core ./firmador-core
COPY firmador-gui ./firmador-gui
COPY firmador-backend ./firmador-backend

RUN mvn -f ./pom-parent.xml -pl ./firmador-core -am install -DskipTests && \
    mvn -f ./firmador-backend/pom.xml package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/firmador-backend/target/firmador-backend-2.0.0.jar ./app.jar

EXPOSE 8080

CMD ["sh", "-c", "java -jar ./app.jar --server.port=${PORT:-8080}"]