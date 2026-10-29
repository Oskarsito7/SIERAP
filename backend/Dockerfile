# Imagen del backend de SIERAP (ADR-019): compila el jar con el Maven Wrapper y lo
# ejecuta sobre un JRE 21. Uso: docker compose --profile app up -d --build

# --- Etapa 1: compilación ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Por si el repo se clonó en Windows con finales de línea CRLF.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
# Las pruebas se corren en CI y con ./mvnw verify; aquí solo se empaqueta.
RUN ./mvnw -B -q package -DskipTests && cp target/sierap-backend-*.jar app.jar

# --- Etapa 2: ejecución ---
FROM eclipse-temurin:21-jre
WORKDIR /app
# Carpeta de los archivos de las entregas (ADR-015), montada como volumen.
RUN mkdir -p /var/sierap-files
COPY --from=build /app/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
