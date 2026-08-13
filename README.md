# AlkaEssentials

Utilitários da rede **AlkaStudio** para servidores **Minecraft 1.21+ (Paper, Java 21)**.
Substitui soluções genéricas (yEssentials, CMI etc.) por um plugin proprietário,
ultraleve e livre de conflitos, construído sobre a infraestrutura do **AlkaCore**.

## Módulos

| Módulo | O que faz |
|--------|-----------|
| **Teleporte** | `/spawn`, `/warp` (GUI), `/home` (GUI), `/tpa`, `/rtp`, `/back`, cooldowns/warmups, elevadores |
| **QoL** | `/craft`, `/lixo`, `/nv`, `/ping`, `/sit`, `/ride`, soup heal, bigorna infinita, ender pearl cooldown |
| **Morte/Inventário** | Túmulos (deathchest) com holograma, mensagens de morte custom, `/invrestore` |
| **Scoreboard** | AlkaScore: scoreboards/tablist por mundo com título e linhas animadas (`<rainbow>`, `<scroll>`, `<gradient>`) |
| **Admin/Moderação** | Punições (warn/mute/kick/ban + temporárias + menu `/punish`), `/staff`, `/invsee`, `/freeze`, `/fly`, `/god`, `/vanish`, spies, `/maintenance` |
| **Ambiente** | Regras por mundo via GUI (`/worldrules`): mobs, vilagers, tempo, chuva, gravidade, fogo, fome, etc. |
| **Chat/Social** | `/nick`, `/color`, `/namecolor` (GUI), `/gradient`, `/whois`, @menções, filtro, `/ignore`, auto-broadcast, `/broadcast` |

## Destaques

- **Tudo editável via YAML** (`config.yml`, `messages.yml`, `menus.yml`, `reasons.yml`, `events.yml`, `scoreboards.yml`…) com **MiniMessage** (gradient, rainbow, bold).
- **Integração** com `AlkaCore` (dependência), `PlaceholderAPI` (`%alkaessentials_nick%`), `TAB` e `nChat` (nick em chat e tab).
- **Punições e InvRestore** no banco do AlkaCore (tabelas `alka_essentials_punishments` e `alka_essentials_inv_snapshots`).
- **Regras do mundo ao vivo** pela GUI, sem reiniciar.

## Build

```bash
./gradlew build
```

O jar fica em `build/libs/AlkaEssentials-<versão>.jar`. Exige o **AlkaCore** instalado.

## Guia do administrador

Veja [`ADMIN-GUIDE.md`](ADMIN-GUIDE.md) para configurar nick, nChat, TAB e permissões.
