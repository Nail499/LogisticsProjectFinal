# Çoxmərhələli (multi-stage) build — DigitalOcean App Platform Java/Maven
# layihələrini buildpack ilə avtomatik tanımır (yalnız Go/Node/PHP/Python/
# Ruby/Rust/.NET dəstəklənir), ona görə Docker mütləqdir.
#
# 1-ci mərhələ: Maven ilə jar faylını tərtib et. ./mvnw əvəzinə əlavə edilmiş
# Maven image istifadə olunur ki, repo-daxili wrapper-in şəbəkə/icazə
# məsələləri (bax CI-dakı "Permission denied" təcrübəsi) build mühitinə
# təsir etməsin.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Asılılıqları əvvəlcədən yükləmək üçün yalnız pom.xml kopyalanır — mənbə
# kodu dəyişəndə Docker layer cache-i asılılıqlar üçün etibarlı qalır.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

# 2-ci mərhələ: yalnız JRE (tam JDK yox) və tərtib olunmuş jar — kiçik və
# təhlükəsiz runtime image.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# DO App Platform-un http_port dəyəri (bax .do/app.yaml) bununla üst-üstə
# düşməlidir. server.port təyin olunmayıb — Spring Boot defolt 8080-də
# dinləyir.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
