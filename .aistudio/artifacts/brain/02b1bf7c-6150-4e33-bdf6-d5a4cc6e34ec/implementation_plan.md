# Implementation Plan: LFS Oasis "Disney Imagineering Engagement Translator", Uninterrupted Universal Shell Ribbon & Sanctuary Storyteller (`cb64f9e7`)

## 1. "Disney Imagineering Engagement Translator" (Grade <= 8.0 All-Ages Vocabulary)
- **Warm All-Ages UI Terminology Mapping (`DisneyImagineeringTranslator` in `QuoteModels.kt`)** (preserving all wire/schema keys such as `stableNodeId`, `aegisEnvelopeV1`, `HOST_LOCAL_DEVICE`, `LAUNCH_U2_HOSTING_SERVICE`, `zt9_comfort_overrides`):
  - Tailscale Auto-SSO (`Node.StableID`) -> **"Hearth Key (Instant Family Pass)"**
  - `aegisEnvelopeV1` AES-256-GCM Save Slots (`slot_01`..`slot_20`) -> **"Time-Machine Memory Vaults"**
  - `HOST_LOCAL_PC` / `HOST_LOCAL_DEVICE` & `LAUNCH_U2_HOSTING_SERVICE` -> **"Host From My Desk"** / **"Open a Family Cloud Room"**
  - `ZT-6` Error & Status Banners -> **"Friendly Guide Cards"** structured in 4 clear, reassuring steps:
    1. **What Happened**: Plain-language summary of the event or offline fallback.
    2. **Why**: Simple explanation without intimidating jargon.
    3. **Your Progress Is Safe**: Confirmation that quotes, stamps, Starlight Rupees, and Memory Vaults are safely stored.
    4. **One-Click Next Step**: Immediate action button to retry, continue offline, or switch rooms.
  - `Z_1440` Curfew -> **"Starlight Quiet Hours (Rest & Recharge Time)"**
  - TrashArcade CMY Rupees & Assets -> **"Starlight Rupees & Wonder Relics"**
  - `BoxBoy-Zombies` & `Boxhead-Fort` `zt9_comfort_overrides` (**Family-Friendly Arcade Mode** enabled by default):
    - Frames opponents and eliminations as playful **"Cardboard Critters"**, **"Neon Arcade Bots"**, **"Confetti KOs"**, and **"Tag-Team Rescue"** so Ring 1 (Child) through Ring 4 (Owner) players can play together comfortably.

## 2. Uninterrupted Top `OasisMultiplayerBar` Shell Ribbon, `Shift+Tab` / `Ctrl+K` Overlay & 7-Destination Switcher
- **Persistent Top Shell Ribbon (`OasisMultiplayerBar` mounted in `MainActivity.kt` above both `LoginScreen` and `CommonplaceAppShell`)**:
  - Remains mounted continuously across Title (`LoginScreen`), Gameplay/Discovery, Modals, and Wing switches without ever dropping the active party room (`OASIS-XXXX`) or the **16-Channel Rmblyn Family Chat (`@willow`, `@cinder`, `@mote`)**.
- **`oasis:overlay-toggle` (`Shift+Tab` / `Ctrl+K` & 1-Click Ribbon Toggle)**:
  - Dispatches and listens for `oasis:overlay-toggle` (including hardware keyboard `Shift+Tab` and `Ctrl+K` key events plus a dedicated ribbon button), cleanly pausing single-player action physics (`isActionPhysicsPaused = true`) while keeping 16-Channel Rmblyn Family Chat (`@willow`, `@cinder`, `@mote`), menus, and LFS navigation responsive.
- **1-Click "Switch Wing / Return to TrashArcade Hall" 7-Destination Selector**:
  - Supports all 7 canonical destinations:
    1. `Commonplace Wing I`
    2. `Scriptorium Wing II`
    3. `Chapterhouse Wing III`
    4. `3D Vigil Wing IV`
    5. `Code Runner`
    6. `Boxhead Fort`
    7. `BoxBoy Defense`
  - Combines the **best of both** for arcade destinations (`Code Runner`, `Boxhead Fort`, `BoxBoy Defense`): interactive in-app arcade preview cards with live `zt9_comfort_overrides` Family-Friendly Arcade Mode controls ("Cardboard Critters / Neon Arcade Bots / Confetti KOs / Tag-Team Rescue"), party readiness status, and Starlight Rupee rewards.

## 3. AudioBookshelf & OpenAudible "Sanctuary Storyteller" Pill (`cb64f9e7`)
- **Persistent Top-Bar Audio Pill (`SanctuaryStorytellerPill`) & Service (`OasisCommonplaceServerClient.kt` + `QuoteAudioAndVoiceService.kt`)**:
  - Wired to `GET /api/lfs/openaudible/library` and `GET /api/lfs/openaudible/stream/[key]` with Tailscale `:13379/lfs-sso/handshake` Hearth Key identity headers (`<= 1600ms` non-blocking timeout).
  - **Best-of-Both Offline Fallback**: When offline or unreachable, provides synthesized ambient storyteller audio + expressive literary narration alongside a readable chapter transcript card and live chapter progress timer.
  - Includes **Play/Pause**, **Chapter Title selector**, and a **15m / 30m / 45m Starlight Sleep-Fade Timer** that gently attenuates volume as bedtime approaches.

## 4. Ratchet Hygiene & Verification
- Audit all Kotlin source files to guarantee **0 raw Unicode emoji characters** (using Material/Lucide vector icons or `\uXXXX` escapes where needed) and **0 `/home/` path leaks**.
- Add unit tests in `ExampleUnitTest.kt` verifying the Disney Imagineering Translator mappings, `zt9_comfort_overrides` defaults, 7-destination switcher state, `oasis:overlay-toggle` pause state, Friendly Guide Card 4-step builder, and Sanctuary Storyteller (`cb64f9e7`) library/stream endpoints and sleep-fade timer.
- Verify clean compilation via `compile_applet`.
