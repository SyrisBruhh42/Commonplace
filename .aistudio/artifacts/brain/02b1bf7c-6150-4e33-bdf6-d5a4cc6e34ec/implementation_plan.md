# Commonplace — Literary Quote Discovery & Archival Journal

**Commonplace** is an Android application crafted in the tradition of a classic literary commonplace book. It generates and prioritizes famous quotes based on recent activity, popular online cultural usage, and personal interest filters, while stamping every saved quote with precise location, date, and time provenance and unlocking deep multimodal Gemini AI exploration.

## User Review & Critical Decisions

> [!IMPORTANT]
> All core product, aesthetic, and persistence preferences have been aligned based on your selections. Review the architectural blueprint below and click **Proceed** to begin provisioning Firebase and building the application.

- **Confirmed Visual Aesthetic**: **Editorial Literary Journal with Warm Parchment Tones** — Archival cream and warm parchment surfaces, deep letterpress ink typography, terracotta-sepia and antique gold accents, deckle-inspired card borders, and library stamp metadata badges.
- **Confirmed Persistence & Sync**: **Cloud Sync with Google Sign-In Across Devices** — Backed by Firebase Authentication (Google Sign-In via Jetpack Credential Manager) and Cloud Firestore so your saved quotes, location/time/date stamps, usage history, and personal interest filters persist seamlessly across devices.
- **Confirmed Gemini AI Capabilities**: All four AI exploration pillars are integrated into the primary navigation and quote detail workflows:
  1. **Photo-to-Quote Matcher** (`gemini-4-argon` multimodal image analysis via Android Photo Picker).
  2. **Philosophical Chat Companion** (multi-turn streaming chat with literary & philosophical persona).
  3. **Real-Time Web & Location Context** (`gemini-3.8-flash` with Google Search and Google Maps grounding for online popularity and historical landmarks).
  4. **Deep Meaning Breakdown & Backstory Generator** (`gemini-4-argon` with `ThinkingLevel.HIGH` for deep philosophical synthesis, paired with low-latency `gemini-3.1-flash-lite` for instant quote micro-summaries and mood tags).

---

## 1. Overview & Core Concept

- **What It Does**: Commonplace intelligently surfaces famous quotes tailored to the user's evolving interests, recent reading patterns, and broader online cultural resonance. When a user saves a quote, the app records an archival stamp containing the **geographic location**, **local time**, and **date** of the moment it resonated, organizing saved quotes into a searchable, categorized library.
- **Target Audience / Persona**: Readers, writers, thinkers, and lifelong learners who want more than a static quote widget—seeking a thoughtful personal archive that remembers *where* and *when* an idea struck them, enriched by deep philosophical context and visual inspiration.
- **Key Value**: Transforms fleeting quote browsing into a durable, context-aware literary journal combining algorithmic priority scoring, physical-world provenance stamps (location, time, date), and frontier Gemini intelligence.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Authentication & Onboarding Gate**:
   - Launches into an editorial cover screen featuring a classic epigraph and a **"Sign in with Google"** button powered by Jetpack Credential Manager (with automatic silent sign-in on returning sessions).
   - Once signed in, users can toggle their **Personal Interest Filters** (e.g., *Stoicism, Literature, Philosophy, Science, Resilience, Art, Leadership, Poetry, History, Mindfulness*).
2. **Priority Quote Discovery ("Discover" Tab)**:
   - Displays a hero editorial quote card with typographic drop-quotes, author attribution, work citation, category pill, and a transparent **Priority Breakdown Badge** (*Personal Interest Match + Online Popularity + Freshness/Recency Weight*).
   - **Quick-Spark Bar (Low-Latency AI)**: Uses `gemini-3.1-flash-lite` to deliver an instant one-line modern takeaway or mood resonance tag on demand.
   - **Action Bar**:
     - **Next Priority Quote**: Generates or selects the next highest-priority quote weighted by personal interest filters, popular online usage, and recency cooldowns.
     - **Save with Stamp**: Requests location permission (if not yet granted), resolves the user's current city/neighborhood or coordinates via GPS + Geocoder, attaches the exact **Location, Date, and Time Stamp** (plus an optional personal reflection note), and syncs to Firestore.
     - **Share Quote**: Opens the native Android Share sheet with formatted quote text, author, and optional archival stamp.
     - **Deep Dive (High Thinking & Grounding)**: Opens the rich AI Literary Dossier sheet.
3. **Searchable & Categorized Quote Library ("Library & Archive" Tab)**:
   - Dual-mode view switching between **My Stamped Archive** (saved favorites with location, date, and time stamps) and the **Master Quote Catalog** (curated + AI-discovered quotes).
   - **Instant Search & Multi-Faceted Filtering**: Real-time search bar matching quote text, author, category, or saved city/location name; horizontal category chips; and sort selector (*Priority Score, Popular Online, Recently Used, Date Saved*).
   - Every saved quote card showcases a styled **Archival Provenance Stamp** (e.g., `📍 Kyoto, Japan • Nov 1, 2026 • 09:07 AM`).
4. **Photo-to-Quote Matcher ("Visual Muse" Tab)**:
   - Uses the zero-permission Android Photo Picker (`PickVisualMedia`) so users can select any photograph (a sunset, street scene, book page, or artwork).
   - Analyzes the image with **`gemini-4-argon`** to extract mood, visual motifs, and philosophical themes, returning matched famous quotes that can be immediately saved with a location/time/date stamp.
5. **Philosophical Companion ("Symposium Chat" Tab)**:
   - Multi-turn conversational thread where users can debate a quote's meaning, ask for counter-perspectives (e.g., Stoic vs. Existentialist view), or find quotes for specific life moments.
   - Users can jump directly into chat from any quote card ("Discuss this Quote") with the quote pre-loaded into conversation context.

### Visual Identity & Theme

- **Aesthetic Direction**: **Editorial Literary Journal** — Inspired by fine-press books, archival index cards, and marginalia. High-contrast serif display headers paired with warm, tactile paper surfaces.
- **Color Palette & Mood**:
  - **Light Mode (Warm Parchment)**:
    - `background`: `#FBF7F0` (Aged Cream Parchment)
    - `surface`: `#F4EFE6` (Warm Vellum Card Surface)
    - `surfaceVariant`: `#EAE2D3` (Pressed Linen)
    - `primary`: `#8C4A27` (Terracotta Bookcloth / Sepia Ink)
    - `secondary`: `#3D5A45` (Library Banker's Lamp Green)
    - `tertiary`: `#B8860B` (Antique Gold Leaf Stamp)
    - `onBackground` / `onSurface`: `#1C1917` (Deep Letterpress Charcoal Ink)
  - **Dark Mode (Nocturnal Study)**:
    - `background`: `#141210` (Deep Espresso Leather)
    - `surface`: `#1E1B18` (Warm Obsidian)
    - `surfaceVariant`: `#2B2622` (Smoked Walnut)
    - `primary`: `#E09F7D` (Warm Copper)
    - `secondary`: `#8AB092` (Sage Leaf)
    - `tertiary`: `#E5C158` (Gilded Brass)
- **Typography & Hierarchy**:
  - Bundled locally via `font-util` in `res/font/`:
    - **Display & Quote Text**: `Playfair Display` — High-contrast editorial serif for quote body text, drop caps, and section titles.
    - **Body & UI Labels**: `Lora` and `Source Sans 3` — Crisp readable text for historical context, search inputs, and archival location/time/date stamps.
- **Component Styling & Layout**:
  - **Archival Quote Cards**: Subtle 1dp warm border, asymmetric quotation mark watermark drawn on Canvas, and a bottom perforated-style divider separating the quote from its **Location • Date • Time** stamp.
  - **Adaptive Layout**: Bottom `NavigationBar` on compact phones; transitions cleanly to wide-screen constraints (`widthIn(max = 640.dp)`) for comfortable reading measure on tablets/foldables.
- **Interactive Feedback & Motion**:
  - Smooth spring-physics card transitions when cycling priority quotes, animated ink-stamp reveal when saving a quote with GPS/time metadata, and streaming text reveal during philosophical chat and high-thinking analyses.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Hybrid Priority Engine (Deterministic Scoring + Gemini Discovery)**
  - *Chosen Approach*: Combine a rich curated seed catalog of famous quotes with a dynamic priority scoring engine (`PriorityScore = InterestMatchWeight * 0.45 + OnlinePopularityScore * 0.35 + RecencyBoost * 0.20 - RecentlySeenPenalty`) and an on-demand Gemini Quote Discovery generator that fetches new famous quotes tailored to the user's active filters.
  - *Why*: Guarantees instant zero-latency quote rendering on launch while allowing limitless personalized discovery weighted by recent uses, popular online uses, and personal interest filters.
- **Decision 2: Cloud Firestore Per-User Archival Sync + Runtime Location Stamping**
  - *Chosen Approach*: Store saved quotes, provenance stamps (`locationName`, `latitude`, `longitude`, `savedDateFormatted`, `savedTimeFormatted`, `savedAt`), usage logs, and user interest profiles in Cloud Firestore under `/users/{userId}/...`, paired with Android's `LocationManager` + `Geocoder` for human-readable location stamps.
  - *Why*: Ensures users never lose their personal commonplace book across devices while gracefully allowing manual location entry or "Location Stamp Unavailable" fallback if GPS permission is denied.
- **Decision 3: Multi-Model Gemini Architecture via Firebase AI Logic**
  - *Chosen Approach*: Route each AI task to the specialized Gemini model specified in the requirements:
    - **`gemini-4-argon` (with `ThinkingLevel.HIGH`)**: Deep Meaning Breakdown & Historical Backstory generator, plus complex philosophical reasoning.
    - **`gemini-4-argon` (Multimodal)**: Photo-to-Quote image analysis.
    - **`gemini-3.8-flash` (with Google Search & Google Maps Grounding)**: Real-time web context (how the quote is used online today, fact-checking attributions) and geographic context (author birthplaces, museums, and literary landmarks).
    - **`gemini-3.1-flash-lite`**: Low-latency instant quote takeaways, mood tags, and quick filter suggestions.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        MainActivity (Edge-to-Edge)                         │
│     configureAppCheck(this, intent) -> AuthGate (CredentialManager)        │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │
        ┌─────────────────────────────┴─────────────────────────────┐
        ▼ (Unauthenticated)                                         ▼ (Authenticated)
┌───────────────────────────┐                       ┌───────────────────────────────────────┐
│        AuthScreen         │                       │           CommonplaceAppShell         │
│  • Silent Google Sign-In  │                       │  • M3 NavigationBar (4 Primary Tabs)  │
│  • Interactive Sign-In    │                       │  • Quote Detail & AI Dossier Sheet    │
└───────────────────────────┘                       └───────┬───────────┬───────────┬───────┘
                                                            │           │           │
      ┌─────────────────────────────────────────────────────┘           │           └─────────────────────────┐
      ▼                                             ▼                   ▼                                     ▼
┌───────────────────────────┐         ┌───────────────────────────┐ ┌───────────────────────────┐ ┌───────────────────────────┐
│      DiscoverScreen       │         │       LibraryScreen       │ │     VisualMuseScreen      │ │    SymposiumChatScreen    │
│ • Priority Quote Generator│         │ • Stamped Favorites       │ │ • PickVisualMedia Picker  │ │ • Multi-turn Chat Thread  │
│ • Personal Interest Chips │         │ • Master Catalog & Search │ │ • gemini-4-argon Vision   │ │ • Literary Persona System │
│ • Save w/ Location/Time   │         │ • Category & Sort Filters │ │ • Save Matched Quotes     │ │ • Quote Context Injection │
│ • Low-Latency Spark       │         │ • Stamp Inspector & Share │ └─────────────┬─────────────┘ └─────────────┬─────────────┘
└─────────────┬─────────────┘         └─────────────┬─────────────┘               │                             │
              │                                     │                             │                             │
              └──────────────────┬──────────────────┘                             └──────────────┬──────────────┘
                                 ▼                                                               ▼
┌─────────────────────────────────────────────────────────┐     ┌─────────────────────────────────────────────────────────────┐
│            QuoteRepository & LocationStamper            │     │                    GeminiQuoteAiService                     │
│ • Firestore (Custom DB ID: R.string.firestore_database) │     │ • gemini-4-argon (High Thinking: Meaning & Image Analysis)  │
│ • /users/{uid}/saved_quotes (Location, Time, Date)      │     │ • gemini-3.8-flash (Google Search + Google Maps Grounding)  │
│ • /users/{uid}/preferences (Interests, Recent Uses)     │     │ • gemini-3.1-flash-lite (Low-latency Micro-Takeaways)       │
│ • Android LocationManager + Geocoder Stamp Resolver     │     │ • Streaming Multi-Turn Chat Session                         │
└─────────────────────────────────────────────────────────┘     └─────────────────────────────────────────────────────────────┘
```

### Data Model & State

1. **`QuoteItem` (Domain & Catalog Model)**:
   - `id`: Unique identifier.
   - `text`: The quote text.
   - `author`: Author full name and era/role.
   - `sourceWork`: Book, speech, or letter citation.
   - `categories`: List of thematic categories (e.g., `["Stoicism", "Resilience"]`).
   - `onlinePopularityScore`: Popularity index (`1..100`) reflecting online cultural frequency.
   - `popularOnlineContext`: Brief note on how the quote is popularly referenced online.
   - `recentUseCount` & `lastUsedTimestamp`: Tracks recent interactions to compute priority rank.
2. **`SavedQuoteDocument` (Firestore `/users/{userId}/saved_quotes/{quoteId}`)**:
   - `userId`: Owner UID (`request.auth.uid`).
   - `quoteText`, `author`, `sourceWork`, `categories`, `onlinePopularityScore`.
   - **Provenance Stamp Fields**:
     - `locationStamp`: Human-readable place name (e.g., `"San Francisco, CA"` or `"37.77° N, 122.41° W"`).
     - `latitude` / `longitude`: Optional double coordinates.
     - `dateStamp`: Formatted date string (e.g., `"Nov 1, 2026"`).
     - `timeStamp`: Formatted local time string (e.g., `"9:07 AM PST"`).
     - `personalNote`: Optional user marginalia/reflection.
     - `createdAt` / `updatedAt`: Firestore `@ServerTimestamp`.
3. **`UserPreferencesDocument` (Firestore `/users/{userId}/settings/preferences`)**:
   - `userId`: Owner UID.
   - `selectedInterests`: List of personal interest categories chosen by the user.
   - `priorityMode`: Weighting preset (`"Balanced"`, `"Personal Interests"`, `"Popular Online"`, `"Recent & Trending"`).
   - `recentQuoteIds`: Rolling list of recently generated/viewed quotes.
   - `updatedAt`: Firestore `@ServerTimestamp`.

### Interactive Component & State Mapping

- **Priority Generator & Filter Bar**: Toggling any personal interest chip or priority mode immediately recalculates each quote's composite priority score, persists the updated filter list to Firestore, and animates the top-priority quote card.
- **Save with Location, Date & Time Stamp**: Tapping "Save to Archive" checks `ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION` via `rememberLauncherForActivityResult`. Whether GPS resolves a city name via `Geocoder` or the user enters/confirms a custom location label, the repository writes the complete `SavedQuoteDocument` with `locationStamp`, `dateStamp`, `timeStamp`, and server timestamps to Firestore.
- **Quote Library Search & Categorization**: Typing in the search bar or selecting a category chip reactively filters `StateFlow<LibraryUiState>` across quote text, author, source work, category tags, and saved location stamps.
- **AI Deep-Dive Dossier Modal**:
  - **Instant Takeaway Button**: Invokes `gemini-3.1-flash-lite` for sub-second practical wisdom.
  - **Deep Meaning & Backstory Button**: Invokes `gemini-4-argon` with `ThinkingLevel.HIGH` to unpack philosophical lineage and rhetorical structure.
  - **Web & Maps Grounding Tab**: Invokes `gemini-3.8-flash` configured with `Tool.googleSearch()` and `Tool.googleMaps()` to surface live web references, modern usage trends, and historical places tied to the author or quote, rendering clickable source attribution links.
