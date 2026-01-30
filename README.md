# 🎮 KaelisCore

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1+-green?style=for-the-badge" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Java-21+-orange?style=for-the-badge" alt="Java Version">
  <img src="https://img.shields.io/badge/Paper-Compatible-blue?style=for-the-badge" alt="Paper">
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
- Système de warps serveur

### 🛡️ Claims (Protection de terrain)
- Protection par zone
- Système de membres et rôles
- Flags personnalisables (PvP, explosions, etc.)
- Visualisation des claims

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

### 📋 Quêtes
- Système de quêtes modulaire
- Récompenses (argent, XP, items)
- Progression persistante
- GUI de quêtes

### 🎉 Events
- Events automatiques programmables
- Drop Party
- Double XP / Double Money
- Extensible

### 🛡️ Anti-grief
- Protection automatique des claims
- Prévention explosions/feu
- Protection contre endermen/creepers
- Détection de grief

### 🤖 Intégrations
- **Vault** - Économie et permissions
- **PlaceholderAPI** - Placeholders complets
- **LuckPerms** - Préfixes/suffixes chat
- **Discord** - Bot avec bridge chat

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
| `homes.yml` | Système de homes |
| `claims.yml` | Protection de terrain |
| `kits.yml` | Kits disponibles |
| `chat.yml` | Configuration du chat |
| `quests.yml` | Quêtes et missions |
| `stats.yml` | Statistiques |
| `events.yml` | Events automatiques |
| `antigrief.yml` | Protection anti-grief |
| `discord.yml` | Bot Discord |
| `messages.yml` | Tous les messages (100% personnalisables) |

## 📝 Commandes

| Commande | Description | Permission |
|----------|-------------|------------|
| `/balance` | Voir son solde | `kaeliscore.economy.balance` |
| `/pay <joueur> <montant>` | Payer un joueur | `kaeliscore.economy.pay` |
| `/shop` | Ouvrir le shop | `kaeliscore.economy.shop` |
| `/eco <give/take/set> <joueur> <montant>` | Admin économie | `kaeliscore.economy.admin` |
| `/home [nom]` | Téléportation home | `kaeliscore.homes.teleport` |
| `/sethome [nom]` | Créer un home | `kaeliscore.homes.set` |
| `/delhome <nom>` | Supprimer un home | `kaeliscore.homes.delete` |
| `/homes` | Liste des homes (GUI) | `kaeliscore.homes.teleport` |
| `/kit [nom]` | Obtenir un kit | `kaeliscore.kits.use` |
| `/quest` | Voir les quêtes (GUI) | `kaeliscore.quests.view` |
| `/stats [joueur]` | Voir les stats (GUI) | `kaeliscore.stats.view` |
| `/claim` | Commandes de claim | `kaeliscore.claims.create` |
| `/kc reload` | Recharger la config | `kaeliscore.admin` |

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
%kaeliscore_stat_<key>%       - Stat personnalisée
```

## 🔨 Compilation

```bash
# Cloner le repo
git clone https://github.com/k4elis/KaelisCore.git
cd KaelisCore

# Compiler avec Gradle
./gradlew build

# Le JAR sera dans build/libs/
```

## 📋 Prérequis

- **Minecraft**: 1.21.1+
- **Java**: 21+
- **Serveur**: Paper, Purpur, Leaf ou fork compatible
- **Optionnel**: Vault, PlaceholderAPI, LuckPerms

## 🤝 Dépendances optionnelles

- [Vault](https://github.com/MilkBowl/VaultAPI) - Pour l'intégration économie externe
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) - Pour les placeholders
- [LuckPerms](https://luckperms.net/) - Pour les préfixes/suffixes

## 📜 License

MIT License - Voir [LICENSE](LICENSE) pour plus de détails.

## 👤 Auteur

Développé par **Kaelis**

---

<p align="center">
  <i>Un plugin, toutes les fonctionnalités survie dont vous avez besoin.</i>
</p>