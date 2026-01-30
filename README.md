# 🎮 KaelisCore

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1+-green?style=for-the-badge" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Java-21+-orange?style=for-the-badge" alt="Java Version">
  <img src="https://img.shields.io/badge/Paper-Compatible-blue?style=for-the-badge" alt="Paper">
  <img src="https://img.shields.io/badge/Maven-3.9+-red?style=for-the-badge" alt="Maven">
  <img src="https://img.shields.io/badge/Gradle-8.5+-02303A?style=for-the-badge" alt="Gradle">
  <img src="https://img.shields.io/badge/License-MIT-purple?style=for-the-badge" alt="License">
</p>

**Plugin survie complet pour serveurs Minecraft** - Compatible Paper, Purpur, Leaf et autres forks depuis 1.21.1+

## ✨ Fonctionnalités

### 💰 Économie
- Système monétaire complet avec transactions
- Shop GUI avec catégories personnalisables
- Intégration Vault native
- Historique des transactions

### 🏠 Homes & Warps
- Homes illimités (configurable par permissions)
- Téléportation avec délai et cooldown
- GUI de gestion des homes
- **Système de warps serveur avec GUI**
- Commandes `/warp`, `/setwarp`, `/delwarp`

### 🚀 Téléportation
- `/spawn` - Téléportation au spawn serveur
- `/tpa <joueur>` - Demande de téléportation
- `/tpaccept` / `/tpdeny` - Gérer les demandes
- `/back` - Retourner à la dernière position

### 🛡️ Claims (Protection de terrain)
- **Mode houe dorée** (style GriefPrevention)
- Protection par zone avec visualisation particules
- Système de membres et rôles
- Flags personnalisables (PvP, explosions, feu)
- Clic droit avec houe dorée pour créer des claims

### 🎁 Kits
- Kits personnalisables
- Cooldowns configurables
- GUI de sélection
- Permissions par kit

### 💬 Chat
- Format personnalisable avec MiniMessage
- Intégration LuckPerms (préfixes/suffixes)
- Filtres anti-spam et anti-pub
- Couleurs avec permissions

### 📊 Statistiques
- Suivi complet des stats joueurs
- Leaderboards
- GUI de profil
- Sync avec stats vanilla

### 🏆 Système de Rangs
- **Progression automatique** basée sur le temps de jeu, kills, quêtes
- Rangs par défaut: Débutant → Apprenti → Avancé → Expert → Maître → Légende
- Récompenses à chaque promotion
- Intégration LuckPerms

### 🎯 Scoreboard
- **Sidebar en temps réel** avec infos joueur
- Affiche: rang, solde, kills/deaths, temps de jeu
- Personnalisable dans `scoreboard.yml`
- Toggle avec commande

### 📋 Quêtes
- Système de quêtes modulaire
- Récompenses (argent, XP, items)
- Progression persistante
- GUI de quêtes

### 🎉 Events Automatiques
- **Programmation horaire** des events
- Drop Party (12h et 18h par défaut)
- Double XP (14h et 20h par défaut)
- Double Money (19h par défaut)
- Extensible et personnalisable

### 🛡️ Anti-grief
- Protection automatique des claims
- Prévention explosions/feu
- Protection contre endermen/creepers
- Détection de grief

### 🤖 Bot Discord
- Bridge chat bidirectionnel
- **Commandes avancées:**
  - `!status` - Statut complet du serveur
  - `!players` - Joueurs en ligne
  - `!leaderboard <type>` - Classements
  - `!stats <joueur>` - Stats d'un joueur
  - `!events` - Événements actifs
  - `!link <code>` - Lier son compte
  - `!help` - Aide

## 📦 Installation

1. Téléchargez le fichier JAR depuis les releases
2. Placez-le dans le dossier `plugins/` de votre serveur
3. Redémarrez le serveur
4. Configurez les fichiers dans `plugins/KaelisCore/`

## 🔧 Configuration

Tous les fichiers de configuration sont dans `plugins/KaelisCore/`:

| Fichier | Description |
|---------|-------------|
| `config.yml` | Configuration générale |
| `database.yml` | Base de données (MySQL/SQLite) |
| `economy.yml` | Économie et shop |
| `homes.yml` | Système de homes et téléportation |
| `claims.yml` | Protection de terrain |
| `kits.yml` | Kits disponibles |
| `chat.yml` | Configuration du chat |
| `quests.yml` | Quêtes et missions |
| `stats.yml` | Statistiques |
| `events.yml` | Events automatiques programmés |
| `ranks.yml` | Système de rangs automatique |
| `scoreboard.yml` | Sidebar personnalisable |
| `antigrief.yml` | Protection anti-grief |
| `discord.yml` | Bot Discord |
| `messages.yml` | Tous les messages (100% personnalisables) |

## 📝 Commandes

### Économie
| Commande | Description | Permission |
|----------|-------------|------------|
| `/balance` | Voir son solde | `kaeliscore.economy.balance` |
| `/pay <joueur> <montant>` | Payer un joueur | `kaeliscore.economy.pay` |
| `/shop` | Ouvrir le shop | `kaeliscore.economy.shop` |
| `/eco <give/take/set> <joueur> <montant>` | Admin économie | `kaeliscore.economy.admin` |

### Homes
| Commande | Description | Permission |
|----------|-------------|------------|
| `/home [nom]` | Téléportation home | `kaeliscore.homes.teleport` |
| `/sethome [nom]` | Créer un home | `kaeliscore.homes.set` |
| `/delhome <nom>` | Supprimer un home | `kaeliscore.homes.delete` |
| `/homes` | Liste des homes (GUI) | `kaeliscore.homes.teleport` |

### Warps
| Commande | Description | Permission |
|----------|-------------|------------|
| `/warp [nom]` | Téléportation warp | `kaeliscore.warps.use` |
| `/setwarp <nom>` | Créer un warp | `kaeliscore.warps.create` |
| `/delwarp <nom>` | Supprimer un warp | `kaeliscore.warps.delete` |

### Téléportation
| Commande | Description | Permission |
|----------|-------------|------------|
| `/spawn` | Téléportation au spawn | `kaeliscore.teleport.spawn` |
| `/setspawn` | Définir le spawn | `kaeliscore.admin` |
| `/tpa <joueur>` | Demander une téléportation | `kaeliscore.teleport.tpa` |
| `/tpaccept` | Accepter une demande | `kaeliscore.teleport.tpa` |
| `/tpdeny` | Refuser une demande | `kaeliscore.teleport.tpa` |
| `/back` | Dernière position | `kaeliscore.teleport.back` |

### Autres
| Commande | Description | Permission |
|----------|-------------|------------|
| `/kit [nom]` | Obtenir un kit | `kaeliscore.kits.use` |
| `/quest` | Voir les quêtes (GUI) | `kaeliscore.quests.view` |
| `/stats [joueur]` | Voir les stats (GUI) | `kaeliscore.stats.view` |
| `/claim` | Commandes de claim | `kaeliscore.claims.create` |
| `/kc reload` | Recharger la config | `kaeliscore.admin` |

### Claims avec houe dorée 🪙
1. Équipez une **houe en or**
2. Clic droit sur le premier coin
3. Clic droit sur le second coin
4. Le claim est créé ! Les bordures s'affichent avec des particules

## 🏷️ PlaceholderAPI

```
%kaeliscore_balance%          - Solde formaté
%kaeliscore_balance_raw%      - Solde brut
%kaeliscore_playtime%         - Temps de jeu formaté
%kaeliscore_playtime_hours%   - Heures de jeu
%kaeliscore_kills%            - Kills PvP
%kaeliscore_deaths%           - Morts
%kaeliscore_kdr%              - Ratio K/D
%kaeliscore_mob_kills%        - Mobs tués
%kaeliscore_rank%             - Rang actuel
%kaeliscore_stat_<key>%       - Stat personnalisée
```

## 🔨 Compilation

```bash
# Cloner le repo
git clone https://github.com/k4elis/KaelisCore.git
cd KaelisCore
```

### Option 1: Avec Maven (pom.xml)
```bash
# Compiler avec Maven
mvn clean package

# Le JAR sera dans target/KaelisCore-1.0.0.jar
```

### Option 2: Avec Gradle (build.gradle.kts)
```bash
# Compiler avec Gradle
./gradlew build

# Le JAR sera dans build/libs/
```

> **Note**: Les deux méthodes produisent un JAR identique avec toutes les dépendances incluses (shaded/shadow JAR).

## 📋 Prérequis

- **Minecraft**: 1.21.1+
- **Java**: 21+
- **Serveur**: Paper, Purpur, Leaf ou fork compatible
- **Optionnel**: Vault, PlaceholderAPI, LuckPerms

## 🤝 Dépendances optionnelles

- [Vault](https://github.com/MilkBowl/VaultAPI) - Pour l'intégration économie externe
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) - Pour les placeholders
- [LuckPerms](https://luckperms.net/) - Pour les préfixes/suffixes et rangs

## 📜 License

MIT License - Voir [LICENSE](LICENSE) pour plus de détails.

## 👤 Auteur

Développé par **Kaelis**

---

<p align="center">
  <i>Un plugin, toutes les fonctionnalités survie dont vous avez besoin.</i>
</p>