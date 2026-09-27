# =====================================================
# ÉTAPE 1 : BUILD - Compilation avec Maven
# =====================================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# 1. Copier le pom.xml en premier (pour profiter du cache Docker)
COPY pom.xml .

# 2. Télécharger les dépendances (couche cachée si pom.xml inchangé)
RUN mvn dependency:go-offline -B

# 3. Copier le code source
COPY src ./src

# 4. Compiler et packager (sans tests — on les a déjà lancés)
RUN mvn clean package -DskipTests

# =====================================================
# ÉTAPE 2 : RUNTIME - Image légère avec le JAR
# =====================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Créer un utilisateur non-root (sécurité)
RUN addgroup -S innova && adduser -S innova -G innova

# Copier le JAR depuis l'étape build
COPY --from=build /app/target/*.jar app.jar

# Changer de propriétaire
RUN chown -R innova:innova /app

# Utiliser l'utilisateur non-root
USER innova

# Port exposé
EXPOSE 8080

# Point d'entrée
ENTRYPOINT ["java", "-jar", "app.jar"]

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/actuator/health || exit 1