# Commonplace Master Opening Portal & LFS-Quest Trilogy Codex Upgrade

Upgrade **Commonplace** to serve as the **Master Opening Portal, Style Authority, and Wing I Hub** for the merged **Commonplace & LFS-Quest Trilogy (`Part 1`, `Part 2`, and `Part 3`)**, wired into **LFS Oasis Aegis (`U1`/`U2`/`U3`/`U4`)**, the **Zero-Trust Tailscale Mesh (`tail420c4d.ts.net` / `lfs-prod.tail420c4d.ts.net` / `100.106.12.104:13379`)**, and the **TrashArcade Launcher (`NativeArcadeAssetBridge` / `window.ArcadeAssetBridge`)**, while preserving 100% of existing screens, ViewModel flows, audio/TTS, PDF export, and tests.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and UX choices were confirmed during Phase 1 clarification:

- **Confirmed Decision 1 — 4-Wing Trilogy Presentation**: Selecting **Wings II, III, or IV** in the Quest Trilogy Codex opens an **interactive in-app wing preview with live Oasis sync bridge**, allowing users to inspect and interact with Scriptorium Chronicles (`Part 1`), Guild Chapterhouse Raid & Mission Contracts (`Part 2`), and the Obsidian Vigil 3D Guild Desk Roster & Bestiary (`Part 3`) directly inside the app while syncing state with `U2`/`U3`.
- **Confirmed Decision 2 — Philosophical Doctrine Mapping**: Saved quotes and Salon Trivia victories **auto-map from quote category** to a Philosophical Doctrine (`aggression`, `arcane`, or `stealth`), while providing an interactive **manual doctrine override** selector in the Quest Trilogy Codex so users can tune their active Guild Doctrine buff at any time.
- **Confirmed Decision 3 — 20-Slot Aegis Save Ledger (`slot_01`..`slot_20`)**: The app **auto-syncs primary `slot_01`** whenever a quote is saved/stamped or a trivia round completes, and provides an interactive **manual 20-slot Aegis Save Ledger (`slot_01`..`slot_20`)** in the Codex Drawer to seal, replicate (`replicateToAnchor: true`), and restore 6-Gate `aegisEnvelopeV1` snapshots.

---

## 1. Overview & Core Concept

- **What It Does**: Elevates Commonplace into the unified entry point and literary style authority for the four-wing Commonplace & LFS-Quest ecosystem. Every quote saved with an archival provenance stamp or won in a Salon Trivia round emits a live `CommonplaceQuestDirectiveV1` that fuels quests, forum prompts, bestiary lore, and philosophical doctrine buffs (`Aggression`, `Arcane`, `Stealth`) across all four wings.
- **Target Audience / Persona**: Readers, scholars, and guild chroniclers operating across the Lewis Family Systems (`LFS`) Oasis mesh and offline mobile environments who want seamless literary discovery, zero-trust Tailscale verification, and cross-wing RPG progression.
- **Key Value**: Unifies literary contemplation with multi-wing guild progression under strict formal invariants (Euclidean $\mathbb{Z}_{1440}$ curfew arithmetic, 32-state `FIRST_APPLICABLE_DENY` ABAC lattice evaluation, and Total Lexical Order `(ts, replicaId, val)` LWW conflict resolution) with non-blocking (`<= 1600ms`) network resilience.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Master Portal & TopBar Codex Access**: From both the pre-auth `AuthScreen` / `LoginScreen` and the authenticated `TopAppBar`, users can open the **Quest Trilogy Codex & TrashArcade Bridge** sheet/drawer or inspect the live Tailscale & Trilogy status badge.
  2. **4-Wing Switcher & Interactive Wing Previews**: Users switch seamlessly between:
     - **Wing I**: *Commonplace Book* (Literary Quotes, Epigraphs, Zero-Trust World Map, Family Salon & Symposium)
     - **Wing II**: *LFS-Quest Part 1 — Scriptorium & PowerBlocks* (Keep Chronicles, Forum Board, PowerBlocks Pipeline, Task Queue)
     - **Wing III**: *LFS-Quest Part 2 — Guild Chapterhouse & Intel Lab* (Mission Board, Co-Op Raid Boss, Gemini Intel Lab, SaveSheet Ledger)
     - **Wing IV**: *LFS-Quest Part 3 — Obsidian Vigil 3D Guild Desk* (First-Person 3D Mahogany Desk, Adventurer Roster, Realm Map, Bestiary & Lore Index)
  3. **Automatic Directive Emission & Doctrine Override**: Saving a quote or completing a Salon Trivia round automatically mints a `CommonplaceQuestDirectiveV1` (rewarding Guild Gold, TrashArcade Rupees, Reputation, and Bestiary lore seeds) and pushes it to `/api/lfs/sync/state` and primary save `slot_01`. Users can also manually override their active Philosophical Doctrine (`Aggression`, `Arcane`, `Stealth`) or seal/restore any of `slot_01`..`slot_20`.
  4. **4-Pillar Tailscale Handshake & ABAC Inspector**: Users can probe `/lfs-sso/handshake` across presets (`TAILSCALE_ANCHOR_PROD`, `TAILSCALE_LAPUTA_U3`, `LFS_CLOUD_U2`, `LOCAL_HOSTER_19448`), inspect their `stableNodeId`, `persona`, `trustRing` (`1..4`), 5-bit ABAC lattice state (`stateBits`), and verify the 4 Machado CVD-safe `[PASS]` / `[WARN]` pillar badges (`tailscale_tunnel`, `abac_perimeter`, `isolated_profile`, `user_data_backup`).

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Archival Literary Folio & Guild Scriptorium—combining warm aged parchment, antique brass/gold rules, and nocturnal mahogany study tones.
  - *Color Palette & Mood*:
    - Light Folio (`--background`: `#FBF7F0` Warm Parchment, `--surface`: `#F5EFE4`, `--primary`: `#6E261E` Oxblood Seal, `--tertiary`: `#B89146` Antique Brass/Gold).
    - Nocturnal Study (`--background`: `#141210` Obsidian Study, `--surface`: `#1E1B18`, `--primary`: `#E5A93C` Illuminated Gold, `--tertiary`: `#C8A96E` Burnished Brass).
    - Machado CVD-Safe Status Tokens: `[PASS]` uses high-luminance Sapphire/Teal (`#0072B2` / `#56B4E9`) and `[WARN]` uses high-contrast Vermilion/Amber (`#D55E00` / `#E69F00`) paired with explicit text badges (`[PASS]` / `[WARN]`) so status is never conveyed by color alone.
  - *Typography & Hierarchy*: `Playfair Display` for display headers, wing titles, and epigraphs; `Lora` for literary body copy, chronicles, and marginalia; monospace tabular numerals for `stateBits`, LWW timestamps, `OASIS-XXXX` room codes, and Rupee denominations.
  - *Component Styling & Layout*: Embellished with `EditorialCrestBanner`, double-ruled `1.5.dp` antique gold borders, radial parchment canvas washes, and adaptive cards with `48.dp` minimum touch targets.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Non-Blocking `<= 1600ms` Network Budget with Deterministic Local Synthesis**:
  - *Chosen Approach*: Configure all `U2` (`/api/lfs/sync/*`), `U3` (`:19448` `/v1/hoster/*`), and Tailscale (`/lfs-sso/handshake`) HTTP probes with a strict `<= 1600ms` connect/read/write timeout and automatic offline/local synthesis.
  - *Why*: Guarantees zero UI stalls when running outside the Tailnet or when `U2`/`U3` nodes are in standby, while immediately lighting up live `Tier A` endpoints when Tailscale is active.
- **Decision 2 — Formal Mathematical Invariants in Client & Sync Layer**:
  - *Chosen Approach*: Implement exact Euclidean positive modulo in $\mathbb{Z}_{1440}$ (`isWithinCurfewZ1440`), the 32-state `FIRST_APPLICABLE_DENY` ABAC lattice (`evaluateAbac32Lattice`), and Total Lexical Order `(ts, replicaId, val)` LWW (`resolveTotalOrderLww`) using `stableNodeId` as `replicaId`.
  - *Why*: Ensures 100% deterministic parity between the Android client, `U2` Sync server, and `U3` Laputa hoster across overnight curfews and concurrent multi-device edits.
- **Decision 3 — 6-Gate `aegisEnvelopeV1` Cryptographic Save Snapshots**:
  - *Chosen Approach*: Extend `CommonplaceCryptoEnvelope` to seal and verify 6-Gate `aegisEnvelopeV1` JSON envelopes alongside existing `CPF1:` AES-256-GCM folio envelopes, supporting slots `slot_01`..`slot_20` with `replicateToAnchor: true`.
  - *Why*: Preserves backward compatibility with all existing salon envelope tests while enabling cross-wing save replication to `U2` and the Tailscale Anchor (`100.106.12.104:13379`).

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────────────┐
│                   Commonplace Master Portal & Wing I Hub (UI)                     │
│  ┌──────────────────────────┐  ┌───────────────────────────────────────────────┐  │
│  │ AuthScreen / LoginScreen │  │ MainActivity TopAppBar + 5 Core Tabs          │  │
│  │ + Portal Codex Preview   │  │ + Quest Trilogy Codex & TrashArcade Drawer    │  │
│  └────────────┬─────────────┘  └───────────────────────┬───────────────────────┘  │
│               │                                        │                          │
│               ▼                                        ▼                          │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │           QuestTrilogyCodexDrawer & Interactive 4-Wing Switcher             │  │
│  │  • Wing I: Commonplace Book (Quotes, World Map, Family Salon, Symposium)    │  │
│  │  • Wing II: LFS-Quest Part 1 (Scriptorium, Forum Board, PowerBlocks)        │  │
│  │  • Wing III: LFS-Quest Part 2 (Chapterhouse, Co-Op Raid, SaveSheet Ledger)  │  │
│  │  • Wing IV: LFS-Quest Part 3 (Obsidian Vigil 3D Desk, Roster, Bestiary)     │  │
│  │  • TrashArcade Rupee Satchel (9 CMY Gems 1..1000 + 4 Cross-Game Relics)     │  │
│  └─────────────────────────────────────────┬───────────────────────────────────┘  │
└────────────────────────────────────────────┼──────────────────────────────────────┘
                                             │
                                             ▼
┌───────────────────────────────────────────────────────────────────────────────────┐
│                    CommonplaceViewModel & Trilogy State Engine                    │
│  • Emits CommonplaceQuestDirectiveV1 on Quote Save/Stamp & Salon Trivia Victory   │
│  • Auto-maps Quote Category -> Doctrine (Aggression/Arcane/Stealth) + Override    │
│  • Maintains CommonplaceTrilogySyncEnvelopeV1 & 20-Slot Aegis Save Ledger         │
│  • Resolves state conflicts via Total Lexical Order LWW (ts, stableNodeId, val)   │
└─────────────────────┬─────────────────────────────────────────┬───────────────────┘
                      │                                         │
                      ▼                                         ▼
┌───────────────────────────────────────────┐ ┌─────────────────────────────────────┐
│   OasisCommonplaceServerClient (<=1600ms) │ │  CommonplaceCryptoEnvelope (6-Gate) │
│  • GET /lfs-sso/handshake (Tailscale SSO) │ │  • CPF1: AES-256-GCM Folio Cipher   │
│  • Z_1440 Curfew & 32-State ABAC Lattice  │ │  • 6-Gate aegisEnvelopeV1 Sealer    │
│  • U2 Sync (/api/lfs/sync/rooms|state|    │ │  • NativeArcadeAssetBridge Wallet   │
│    saves|presence|rmblyn)                 │ └─────────────────────────────────────┘
│  • U3 Hoster :19448 (/v1/hoster/rooms|    │
│    migrate|wake|join|status & /v1/overlay)│
└───────────────────────────────────────────┘
```

### Data Model & State
- **`LfsOasisServerPreset`**: Enumerates `TAILSCALE_ANCHOR_PROD` (`https://lfs-prod.tail420c4d.ts.net` / `http://100.106.12.104:13379`), `TAILSCALE_LAPUTA_U3` (`http://100.115.219.120:19448`), `LFS_CLOUD_U2` (`https://play.lewisfamilysystems.com`), and `LOCAL_HOSTER_19448` (`http://127.0.0.1:19448` / `http://10.0.2.2:19448`).
- **`TailscaleSsoHandshakeResult` & `Abac32EvaluationResult`**: Captures `stableNodeId` (`Node.StableID`), `subuserId`, `persona` (`OWNER`, `ADULT_PEER`, `CHILD_PROFILE`, `GUEST_PASS`), `trustRing` (`1..4`), `generationEpoch`, 5-bit `stateBits` (`"01110"`), and the 4 Machado CVD-safe status checks (`tailscale_tunnel`, `abac_perimeter`, `isolated_profile`, `user_data_backup`).
- **`CommonplaceQuestDirectiveV1` & `CommonplaceTrilogySyncEnvelopeV1`**: Models cross-wing quest directives (`directiveId`, `quoteText`, `author`, `category`, `philosophicalDoctrine`, `goldReward`, `rupeeReward`, `reputationReward`, `bestiarySeed`), 4-wing status (`Wing I`..`Wing IV`), TrashArcade Rupee Satchel (9 CMY gem denominations `1, 5, 10, 20, 50, 100, 200, 500, 1000` and 4 relics `porch-chronicler-quill`, `viren-epistemic-bookmark`, `undercity-delver-torch`, `willow-visor-emerald`), and the 20-slot Aegis Save Ledger (`slot_01`..`slot_20`).

### Interactive Component & State Mapping
- **Quote Save & Trivia Completion Hooks**: Saving a quote (`saveQuoteWithStamp`) or answering a Salon Trivia round (`submitTriviaAnswer`) automatically generates a `CommonplaceQuestDirectiveV1`, increments Guild Gold, Rupees, and Reputation, updates the active Wing II/III/IV chronicle/mission/bestiary queues, posts `CrdtStateDeltaOp` to `/api/lfs/sync/state`, and seals `slot_01` via `/api/lfs/sync/saves`.
- **4-Wing Interactive Switcher**: Selecting any wing in the Codex Drawer displays its live interactive wing preview panel (allowing users to execute Scriptorium PowerBlocks tasks in Wing II, trigger Co-Op Raid strikes in Wing III, or inspect the 3D Mahogany Desk Adventurer Roster & Bestiary in Wing IV) and dispatches presence updates (`POST /api/lfs/sync/presence`).
- **20-Slot Aegis Save Ledger & Rmblyn Overlay**: Users can select any slot from `slot_01` to `slot_20` to seal or restore a 6-Gate `aegisEnvelopeV1` snapshot with `replicateToAnchor: true`, and send cross-wing commands via `POST /api/lfs/sync/rmblyn` and `POST /v1/overlay/rmblyn`.
