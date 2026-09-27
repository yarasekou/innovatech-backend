# 🚀 InnovaTech API

> API REST de gestion pour une entreprise de services informatiques.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![JWT](https://img.shields.io/badge/JWT-JJWT%200.12.6-purple.svg)](https://github.com/jwtk/jjwt)
[![Docker](https://img.shields.io/badge/Docker-ready-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

---

## 📖 Présentation

**InnovaTech API** est une API REST complète de gestion pour une entreprise de services informatiques. Elle permet de gérer :

- 👥 **Clients** (particuliers et entreprises)
- 📦 **Produits** et services (catalogue + stock)
- 📋 **Commandes** (cycle de vie complet)
- 🔐 **Utilisateurs** avec authentification JWT et rôles

L'API est **sécurisée**, **documentée** (Swagger), **testée** (JUnit + Mockito) et **dockerisée**.

---

## 🛠 Stack technique

| Composant | Version | Usage |
|-----------|---------|-------|
| **Java** | 21 (LTS) | Langage |
| **Spring Boot** | 4.1.1 | Framework |
| **Spring Web MVC** | 7.x | REST |
| **Spring Data JPA** | 4.x | Persistance |
| **Spring Security** | 7.x | Sécurité |
| **JJWT** | 0.12.6 | Tokens JWT |
| **MySQL** | 8.0 | Base de données |
| **SpringDoc OpenAPI** | 2.8.5 | Swagger UI |
| **JUnit 5 / Mockito** | 6.0.3 / 5.23 | Tests |
| **Docker** | 29.x | Conteneurisation |

---

## 🏗 Architecture
Client HTTP → Controller → Service → Repository → Entity → MySQL



**Packages** :

com.innovatech.api/
├── config/ → Configuration Spring
├── controller/ → Endpoints REST
├── dto/ → Data Transfer Objects
├── entity/ → Entités JPA
├── exception/ → Gestion des erreurs
├── mapper/ → Conversion Entity ↔ DTO
├── repository/ → Spring Data JPA
├── security/ → JWT, filtres, config
└── service/ → Logique métier

