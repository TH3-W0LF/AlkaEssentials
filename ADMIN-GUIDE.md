# AlkaEssentials — Guia do Administrador (rede Alka)

Plugin utilitário da rede (Minecraft 1.21+/Paper). Substitui yEssentials/CMI. Tudo
editável via YAML (MiniMessage nos textos) e permissões próprias.

---

## 1. Como o nick (nome de exibição) funciona

O jogador tem 4 comandos de nome, todos **aplicados ao próprio nick**:

| Comando | Função |
|---------|--------|
| `/nick <nome>` | Troca o **texto** do nick (VIP) |
| `/color <nickColorido>` | Só muda a **cor** do nick atual (valida se o texto é o seu) |
| `/namecolor` | Abre **GUI** com 16 cores + estilos (bold, italic, etc.) |
| `/gradient <cor1> [cor2]` | Aplica **gradiente** ao nick |
| `/whois <nick>` / `/realname <nick>` | Descobre o **nome real** de quem tem nick |

O nick é persistido em `plugins/AlkaEssentials/nicks.yml`.

**Importante:** o nick é renderizado via `displayName` do Bukkit. Por isso, plugins
que controlam chat/TAB precisam usar esse valor para exibir o nick. As mudanças
abaixo já estão aplicadas, mas documentadas para quando criar/reinstalar.

---

## 2. Mudança necessária no nChat (para o nick aparecer no chat)

O nChat mostra o nome do jogador com a tag `{sender}`. Para usar o nick do
AlkaEssentials, o formato de chat deve usar `{displayname}` no lugar de `{sender}`.

**Já aplicado** nos canais públicos:
- `plugins/nChat/channels/public/global.yml`
- `plugins/nChat/channels/public/local.yml`

Linha alterada (default e spy):
```yaml
format:
  default: '... {bprefix} {displayname} {bsuffix} {suffix}: {message}'
```

**Se você criar novos canais**, repita: no `format.default` (e `spy`), troque
`{sender}` por `{displayname}`.

**Depois de editar**: rode `/nchat reload` (ou reinicie o servidor).

> Obs.: `{displayname}` = nome de exibição do Bukkit, que é exatamente o nick que o
> AlkaEssentials seta (incluindo cor/gradiente).

---

## 3. Integração com o TAB (plugin TAB, NEZNAMY)

- O **tab list** (lista de jogadores) mostra o nick **automaticamente** — o
  AlkaEssentials seta via API do TAB (`TabListFormatManager.setName`) ao entrar e ao
  mudar o nick.
- O AlkaEssentials registra o placeholder **`%alkaessentials_nick%`** no TAB.

**Nametag (nome acima da cabeça) — JÁ APLICADO** em `plugins/TAB/groups.yml`:
```yaml
_DEFAULT_:
  tabprefix: "%leaftags_tag_tablistprefix%"
  tagprefix: "%leaftags_tag_prefix%"
  customtagname: "%alkaessentials_nick%"   # <-- mostra o nick no lugar do nome real
  tabsuffix: "%leaftags_suffix_suffix%"
  tagsuffix: "%leaftags_tag_suffix%"
```

**Se você criar novos grupos**, adicione a linha:
```yaml
  customtagname: "%alkaessentials_nick%"
```

**Depois de editar**: `/tab reload`.

> Nota: `%alkaessentials_nick%` retorna o nome real quando o jogador não tem nick,
> então não quebra nada. Os prefixos/sufixos do LeafTags continuam funcionando.

---

## 4. Config do AlkaEssentials (arquivos)

Tudo em `plugins/AlkaEssentials/`:

| Arquivo | O que tem |
|---------|-----------|
| `config.yml` | Teleporte, QoL, punições, moderação, mundo, chat, scoreboard |
| `messages.yml` | Todas as mensagens de chat (MiniMessage) |
| `menus.yml` | Títulos/ícones/lores das GUIs |
| `reasons.yml` | Motivos do menu `/punish` |
| `events.yml` | Comandos em gatilhos (on-join, on-first-join, etc.) |
| `scoreboards.yml` / `tablists.yml` | Scoreboard e tablist (AlkaScore) |
| `worldrules.yml` | Regras por mundo (editáveis na GUI `/worldrules`) |
| `nicks.yml`, `homes.yml`, `locations.yml` | Dados salvos |

- **Regras do mundo**: `/worldrules` (GUI) — mobs, animais, chuva, gravidade, fogo,
  folhas, fome, creeper, tnt, mob-grief, e tempo (dia/noite/parado). Ao vivo, sem
  reiniciar.
- **Scoreboard**: `/scoreboard toggle|reload`; títulos/linhas com `frames`+`interval`
  e tags `<rainbow:N>`, `<scroll:left>`, `<gradient>`, `<centralize>`.

---

## 5. Permissões principais (prefixo `alkassentials.*`)

| Permissão | Uso |
|-----------|-----|
| `alkassentials.admin.*` | setspawn/setwarp/invrestore |
| `alkassentials.teleport.bypass` | Ignora cooldown/warmup |
| `alkassentials.afkzone.admin` | Gerencia zonas AFK |
| `alkassentials.staff.*` | clear/heal/feed/fly/god/freeze/invsee/vanish/spies |
| `alkassentials.punish` | Todos os comandos de punição |
| `alkassentials.maintenance(.bypass)` | Manutenção |
| `alkassentials.worldrules` | GUI de regras do mundo |
| `alkassentials.chat.nick` / `.color` | `/nick` e `/color`/`/namecolor`/`/gradient` |
| `alkassentials.chat.realname` / `.ignore` / `.clearchat` / `.broadcast` | Comandos de chat |
| `alkassentials.scoreboard.toggle` / `.reload` | Scoreboard |

---

## 6. Observações

- Os textos aceitam **MiniMessage** (gradient, rainbow, bold, etc.).
- Punições e InvRestore ficam no **banco do AlkaCore** (tabelas
  `alka_essentials_punishments` e `alka_essentials_inv_snapshots`).
- O plugin depende do **AlkaCore** (`depend: [AlkaCore]`).
