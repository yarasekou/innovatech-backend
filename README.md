# InnovaTech API

API REST **multi-tenant** développée avec Spring Boot pour la gestion de plusieurs entreprises et de leurs boutiques.

Chaque entreprise dispose d'un espace de gestion isolé comprenant les utilisateurs, les catégories, les produits, les clients, les fournisseurs, les commandes, les paiements, les factures et les retours.

---

## 🎯 Concept

### Rôles et permissions

| Rôle          | Description                                                               |
| ------------- | ------------------------------------------------------------------------- |
| `SUPER_ADMIN` | Équipe InnovaTech : administration globale et gestion des administrateurs |
| `ADMIN`       | Propriétaire d'une entreprise                                             |
| `USER`        | Gestionnaire d'une boutique au sein d'une entreprise                      |
| `SELLER`      | Vendeur rattaché à un utilisateur gestionnaire                            |

### Hiérarchie

```text
SUPER_ADMIN
    └── Crée et gère les ADMIN
          └── ADMIN : gère son entreprise
                └── Crée ses USER
                      └── USER : gère sa boutique
                            └── Crée ses SELLER
                                  └── SELLER : vendeur terrain
```

Chaque `USER` gère sa propre boutique isolée : ses produits, ses clients et ses commandes.

> Les permissions et les règles d'isolation doivent être appliquées côté serveur et vérifiées par les tests de sécurité.

---

## 🚀 Démarrage rapide

### Prérequis

* Java 21, si cette version correspond à celle configurée dans le projet.
* Maven 3.9+ ou le Maven Wrapper fourni avec le projet.
* MySQL 8+.
* IntelliJ IDEA ou Visual Studio Code.

### 1. Vérifier Java et Maven

Dans PowerShell, à la racine du projet :

```powershell
java -version
.\mvnw.cmd -version
```

Si la commande `.\mvnw.cmd` ne fonctionne pas, vérifie que le fichier `mvnw.cmd` existe à la racine du projet.

### 2. Créer la base de données

Connecte-toi à MySQL et exécute :

```sql
CREATE DATABASE innovatech
CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;
```

Cette instruction suppose que le serveur utilise une version de MySQL compatible avec cette collation.

### 3. Configurer la connexion MySQL

Vérifie les paramètres présents dans :

```text
src/main/resources/application.properties
src/main/resources/application-dev.properties
```

La configuration doit correspondre à ta base de données locale.

Exemple de propriétés Spring Boot :

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/innovatech}
spring.datasource.username=${DB_USER:root}
spring.datasource.password=${DB_PASSWORD}
```

Adapte ces propriétés à la configuration réellement utilisée par ton projet.

Ne publie jamais tes mots de passe dans Git.

### 4. Lancer l'application en développement

Sous Windows PowerShell :

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

L'application sera accessible à l'adresse suivante si le port et le contexte configurés correspondent à cet exemple :

```text
http://localhost:8080/api
```

### 5. Accéder à Swagger UI

Si Springdoc OpenAPI est installé et configuré :

```text
http://localhost:8080/api/swagger-ui.html
```

Selon la version et la configuration de Springdoc, l'adresse peut plutôt être :

```text
http://localhost:8080/swagger-ui/index.html
```

### 6. Arrêter l'application

Dans le terminal où l'application fonctionne, appuie sur :

```text
Ctrl + C
```

---

## 📁 Structure du projet

Structure cible proposée pour l'application :

```text
innovatech-backend/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── Dockerfile
├── README.md
├── uploads/
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/innovatech/api/
    │   │       ├── BackendApplication.java
    │   │       ├── config/
    │   │       ├── controller/
    │   │       ├── dto/
    │   │       ├── entity/
    │   │       ├── exception/
    │   │       ├── mapper/
    │   │       ├── repository/
    │   │       ├── security/
    │   │       └── service/
    │   └── resources/
    │       ├── application.properties
    │       ├── application-dev.properties
    │       ├── application-prod.properties
    │       └── application-docker.properties
    └── test/
        ├── java/
        └── resources/
            └── test.http
```

> Cette arborescence décrit l'organisation visée. Les dossiers et fichiers doivent être adaptés à ceux qui existent réellement dans le projet.

---

## 🔐 Sécurité

Les mécanismes de sécurité prévus pour le projet sont les suivants :

* Authentification par JWT.
* Gestion des rôles `SUPER_ADMIN`, `ADMIN`, `USER` et `SELLER`.
* Contrôle des permissions selon le rôle connecté.
* Isolation des données par entreprise et par boutique.
* Hachage des mots de passe avec BCrypt.
* Configuration CORS adaptée aux domaines du frontend.

### Isolation multi-tenant

L'application doit empêcher un utilisateur d'accéder aux données d'une autre entreprise ou d'une autre boutique sans autorisation.

Les contrôles doivent être effectués côté serveur, notamment dans les services et les requêtes d'accès aux données.

### Clé JWT

La clé de signature doit être suffisamment longue, aléatoire et conservée secrète.

Exemple de variable d'environnement PowerShell :

```powershell
$env:JWT_SECRET = "REMPLACER_PAR_UNE_CLE_SECRETE_ALEATOIRE"
```

Pour la production, configure cette variable dans l'environnement de déploiement plutôt que de conserver une clé secrète dans le code source.

---

## 📋 Endpoints REST prévus

Les endpoints ci-dessous reprennent le périmètre fonctionnel envisagé. Ils doivent être confirmés dans les contrôleurs de l'application.

### 🔓 Authentification

| Méthode | Endpoint                  | Description                                          |
| ------- | ------------------------- | ---------------------------------------------------- |
| POST    | `/auth/setup-super-admin` | Initialisation du SUPER_ADMIN                        |
| POST    | `/auth/register-admin`    | Inscription d'un ADMIN et création de son entreprise |
| POST    | `/auth/login`             | Connexion et obtention d'un JWT                      |
| POST    | `/auth/change-password`   | Changement de mot de passe                           |

**Important :** l'initialisation du SUPER_ADMIN doit être protégée et ne doit pas permettre à n'importe qui de créer un compte administrateur global.

### 👑 Administration globale

| Méthode | Endpoint                                | Description                       |
| ------- | --------------------------------------- | --------------------------------- |
| GET     | `/admin/users`                          | Lister les utilisateurs autorisés |
| PATCH   | `/admin/users/{id}/promote-super-admin` | Promouvoir un utilisateur         |
| PATCH   | `/admin/users/{id}/demote-super-admin`  | Retirer le rôle SUPER_ADMIN       |

### 🏢 Entreprises

| Méthode | Endpoint            | Description                       |
| ------- | ------------------- | --------------------------------- |
| GET     | `/entreprises/me`   | Consulter son entreprise          |
| PUT     | `/entreprises/{id}` | Modifier une entreprise autorisée |
| POST    | `/entreprises/logo` | Ajouter un logo                   |
| DELETE  | `/entreprises/logo` | Supprimer un logo                 |

### 👥 Gestion des USER

| Méthode | Endpoint                   | Description                   |
| ------- | -------------------------- | ----------------------------- |
| POST    | `/users`                   | Créer un USER                 |
| GET     | `/users`                   | Lister les USER autorisés     |
| PUT     | `/users/{id}`              | Modifier un USER              |
| PATCH   | `/users/{id}/toggle-actif` | Activer ou désactiver un USER |

### 🛒 Gestion des SELLER

| Méthode | Endpoint        | Description                 |
| ------- | --------------- | --------------------------- |
| POST    | `/sellers`      | Créer un SELLER             |
| GET     | `/sellers`      | Lister les SELLER autorisés |
| DELETE  | `/sellers/{id}` | Supprimer un SELLER         |

### 📂 Catégories

| Méthode | Endpoint           | Description                           |
| ------- | ------------------ | ------------------------------------- |
| GET     | `/categories`      | Lister les catégories                 |
| POST    | `/categories`      | Créer une catégorie                   |
| PUT     | `/categories/{id}` | Modifier une catégorie                |
| DELETE  | `/categories/{id}` | Supprimer une catégorie               |
| GET     | `/categories/tree` | Obtenir l'arborescence des catégories |

### 📦 Produits

| Méthode | Endpoint                 | Description                            |
| ------- | ------------------------ | -------------------------------------- |
| GET     | `/produits`              | Lister les produits autorisés          |
| POST    | `/produits`              | Créer un produit                       |
| PUT     | `/produits/{id}`         | Modifier un produit                    |
| DELETE  | `/produits/{id}`         | Supprimer un produit                   |
| POST    | `/produits/{id}/photos`  | Ajouter une photo                      |
| GET     | `/produits/search?q=`    | Rechercher des produits                |
| GET     | `/produits/stock-faible` | Consulter les produits en stock faible |

### 👤 Clients

| Méthode | Endpoint        | Description         |
| ------- | --------------- | ------------------- |
| GET     | `/clients`      | Lister les clients  |
| POST    | `/clients`      | Créer un client     |
| PUT     | `/clients/{id}` | Modifier un client  |
| DELETE  | `/clients/{id}` | Supprimer un client |

### 🚚 Fournisseurs

| Méthode | Endpoint             | Description              |
| ------- | -------------------- | ------------------------ |
| GET     | `/fournisseurs`      | Lister les fournisseurs  |
| POST    | `/fournisseurs`      | Créer un fournisseur     |
| PUT     | `/fournisseurs/{id}` | Modifier un fournisseur  |
| DELETE  | `/fournisseurs/{id}` | Supprimer un fournisseur |

### 📋 Commandes

| Méthode | Endpoint                          | Description                       |
| ------- | --------------------------------- | --------------------------------- |
| GET     | `/commandes`                      | Lister les commandes              |
| POST    | `/commandes`                      | Créer une commande multi-produits |
| PUT     | `/commandes/{id}`                 | Modifier une commande             |
| PATCH   | `/commandes/{id}/statut/{statut}` | Modifier le statut                |
| DELETE  | `/commandes/{id}`                 | Supprimer une commande            |

### 💰 Paiements

| Méthode | Endpoint                          | Description                      |
| ------- | --------------------------------- | -------------------------------- |
| GET     | `/paiements`                      | Lister les paiements             |
| POST    | `/paiements`                      | Enregistrer un paiement          |
| GET     | `/paiements/commande/{id}/reste`  | Consulter le reste à payer       |
| PATCH   | `/paiements/{id}/statut/{statut}` | Modifier le statut d'un paiement |

### 🧾 Factures

| Méthode | Endpoint                 | Description                     |
| ------- | ------------------------ | ------------------------------- |
| GET     | `/factures`              | Lister les factures             |
| POST    | `/factures`              | Créer une facture en brouillon  |
| PATCH   | `/factures/{id}/emettre` | Émettre une facture             |
| PATCH   | `/factures/{id}/payer`   | Marquer une facture comme payée |
| PATCH   | `/factures/{id}/annuler` | Annuler une facture             |

### ↩️ Retours

| Méthode | Endpoint                       | Description                    |
| ------- | ------------------------------ | ------------------------------ |
| GET     | `/retours`                     | Lister les retours             |
| POST    | `/retours`                     | Créer un retour                |
| PATCH   | `/retours/{id}/statut?statut=` | Modifier le statut d'un retour |

### 📥 Commandes d'achat

| Méthode | Endpoint                                | Description                  |
| ------- | --------------------------------------- | ---------------------------- |
| GET     | `/commandes-achat`                      | Lister les commandes d'achat |
| POST    | `/commandes-achat`                      | Créer une commande d'achat   |
| PATCH   | `/commandes-achat/{id}/statut/{statut}` | Modifier son statut          |

### 📊 Tableau de bord

| Méthode | Endpoint                          | Description                             |
| ------- | --------------------------------- | --------------------------------------- |
| GET     | `/dashboard/stats`                | Obtenir les statistiques                |
| GET     | `/dashboard/top-produits?limit=5` | Afficher les meilleurs produits         |
| GET     | `/dashboard/top-clients?limit=5`  | Afficher les meilleurs clients          |
| GET     | `/dashboard/ca-mensuel`           | Consulter le chiffre d'affaires mensuel |

---

## 🧪 Tests

Les tests doivent couvrir les fonctionnalités métier et les règles de sécurité.

Scénarios recommandés :

* Initialisation sécurisée du SUPER_ADMIN.
* Création d'entreprises et de comptes ADMIN.
* Création des boutiques et des vendeurs.
* Gestion des catégories, produits, clients et fournisseurs.
* Création de commandes multi-produits.
* Enregistrement des paiements et gestion des factures.
* Gestion des retours et des commandes d'achat.
* Vérification de l'isolation entre entreprises.
* Vérification de l'isolation entre boutiques.
* Vérification des permissions pour chaque rôle.

### Exécuter les tests

Dans PowerShell :

```powershell
.\mvnw.cmd test
```

### Construire le projet

```powershell
.\mvnw.cmd clean package
```

Le fichier JAR sera généré dans le dossier `target/`. Son nom exact dépend de la configuration Maven du projet.

---

## 📦 Build et déploiement

### Générer le JAR

```powershell
.\mvnw.cmd clean package -DskipTests
```

### Lancer le JAR

```powershell
java -jar .\target\api-0.0.1-SNAPSHOT.jar
```

Cette commande suppose que le JAR porte bien ce nom. Vérifie le contenu du dossier `target/` si nécessaire.

Pour activer un profil de production, configure-le conformément aux profils déclarés dans l'application.

### Docker

Si un `Dockerfile` est présent à la racine :

```powershell
docker build -t innovatech-api .
```

Exemple de lancement :

```powershell
docker run -p 8080:8080 `
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/innovatech" `
  -e DB_USER="innovatech_user" `
  -e DB_PASSWORD="REMPLACER_PAR_LE_MOT_DE_PASSE" `
  -e JWT_SECRET="REMPLACER_PAR_UNE_CLE_SECRETE_ALEATOIRE" `
  -v "${PWD}/uploads:/app/uploads" `
  innovatech-api
```

Ces variables doivent correspondre à celles effectivement utilisées par l'application. Le serveur MySQL doit être accessible depuis le conteneur.

---

## ⚙️ Variables d'environnement

| Variable      | Description                          |
| ------------- | ------------------------------------ |
| `DB_URL`      | URL JDBC de la base de données MySQL |
| `DB_USER`     | Utilisateur MySQL                    |
| `DB_PASSWORD` | Mot de passe MySQL                   |
| `JWT_SECRET`  | Clé secrète de signature des JWT     |

Exemple d'URL JDBC :

```text
jdbc:mysql://localhost:3306/innovatech
```

Ne place pas de vrais mots de passe ni de clés JWT dans ce README.

---

## 📄 Licence

Ce projet peut être distribué sous licence MIT si le fichier `LICENSE` correspondant est présent à la racine du dépôt.

---

## 📞 Contact

* **Projet :** InnovaTech API
* **Email :** [contact@innovatech.com](mailto:contact@innovatech.com)
* **Site web :** https://innovatech.com