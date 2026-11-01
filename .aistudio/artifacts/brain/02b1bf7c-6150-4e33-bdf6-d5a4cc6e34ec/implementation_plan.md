# Zero-Trust Map Discovery, Privacy Control Overlay & Archival Literary Suite Plan

## Overview
We will implement a **MapDiscoveryScreen** powered by a hybrid Google Maps SDK and interactive archival cartographic atlas that enables scholars to drop a single **Daily Quote Pin** (`1 pin per UTC day`) onto a shared world map under a strict **Zero-Trust privacy architecture**. Alongside the map, a dedicated **PrivacyControlOverlay** lets users toggle their global public map visibility (hiding from the public map to share exclusively with permitted friends when disabled), configure per-friend location sharing permissions with selectable **Exact GPS & Venue** vs. **Coarse Neighborhood** precision, and filter the map to display only trusted connections. We will also complete the wiring for the **Home Screen Daily Quote Widget**, **Scheduled Quote of the Day Notifications**, and **Interactive Library Passage Highlighting & Stylized PDF Journal Page Export**.

---

## Visual Design & Aesthetics
- **Cartographic & Archival Palette**:
  - **Map Discovery Canvas**: Warm antique vellum and charcoal slate cartography (`#1C1915` deep sepia ink, `#D4AF37` burnished gold markers for connected circle friends, `#2E6F65` verdigris oxidation accents for public anonymous ciphers, and `#B85D3B` sealing-wax terracotta for the user's own daily pin).
  - **Hybrid Rendering Engine**: When a Google Maps API key is configured in AI Studio Secrets, the screen renders native Google Maps SDK vector tiles styled with a custom literary sepia JSON map style; when no external key is present (or when toggled by the user), it seamlessly renders an interactive pan-and-zoom Archival Cartographic Atlas with graticule lines, regional clusters, and animated compass bearings so the map is always functional in the emulator.
- **PrivacyControlOverlay Aesthetics**:
  - Styled as an **Archival Cipher Ledger** modal sheet and floating HUD pill (`Zero-Trust Shield Status`) displaying real-time privacy posture:
    - **Global Map Visibility Switch**: Clearly indicates whether the daily pin is broadcast anonymously to the public map (`Anonymous Stoic #XXXX` with jittered ~2.5 km coarse coordinates) or restricted strictly to permitted friends (`Trusted Circles Only`).
    - **Per-Friend Location Permission Matrix**: Lists every scholar from the user's connected Reading Circles with an individual permission toggle (`Hidden`, `Coarse Neighborhood (~2.5 km)`, or `Exact GPS & Venue Name`).
    - **Trusted Connections Filter Bar**: Quick-toggle chips on the map HUD (`All Public Pins`, `Trusted Connections Only`, `My Daily Pin`).
- **Accessibility & AAA High-Contrast Support**:
  - All map pins, privacy toggles, and overlay controls honor the app's **WCAG AAA High-Contrast Mode** with high-luminance borders, minimum `48dp` interactive targets, and descriptive screen-reader semantics.

---

## Architecture & Technical Approach

### Zero-Trust Split-Collection Firestore Architecture
Because Firestore security rules evaluate access at the document level rather than filtering individual fields inside a document, true Zero-Trust requires splitting each daily quote pin into two separate collections keyed by a deterministic daily ID (`{uid}_{utcDayKey}`):

```text
+-----------------------------------------------------------------------------------+
|                        MapDiscoveryScreen & PrivacyControlOverlay                 |
|  - Hybrid Google Maps SDK + Interactive Archival Atlas Fallback                   |
|  - "Pin Quote of the Day" (1/day limit)   - Filter: All vs. Trusted Connections   |
+-----------------------------------------+-----------------------------------------+
                                          |
                     Deterministic Pin ID: {userUid}_{YYYY-MM-DD}
                                          |
                 +------------------------+------------------------+
                 |                                                 |
                 v                                                 v
+----------------------------------------+   +--------------------------------------+
|     Public Collection (Optional)       |   |     Zero-Trust Identity Vault        |
|        /map_quote_pins/{pinId}         |   |      /map_pin_identities/{pinId}     |
+----------------------------------------+   +--------------------------------------+
| - Written ONLY when Global Map         |   | - Contains author display name,      |
|   Visibility is ON                     |   |   email, personal marginalia,        |
| - Contains quote text, author, work,   |   |   coarse neighborhood & exact GPS    |
|   coarse jittered lat/lng (~2.5km),    |   | - Per-friend precision map:          |
|   region label & anonymous cipher      |   |   (EXACT_GPS vs NEIGHBORHOOD)        |
| - ZERO PII: No user name, no email,    |   | - Firestore Rule: Read allowed ONLY  |
|   no exact GPS coordinates             |   |   if request.auth.uid == ownerUid    |
| - Firestore Rule: Enforces pinId ==    |   |   OR caller UID/email is in          |
|   uid + "_" + dayKey (max 1 per day)   |   |   connectedUserUids / Emails         |
+----------------------------------------+   +--------------------------------------+
```

1. **Public Anonymous Pin Layer (`/map_quote_pins/{pinId}`)**:
   - Contains only the literary quote, author, work, an anonymized coordinate rounded to ~2.5 km (`0.02°` grid) so exact street addresses are never exposed, a general city/region label, and a deterministic cipher badge (`Anonymous Stoic #4F9A`).
   - When the user turns **Global Map Visibility OFF** in the `PrivacyControlOverlay`, their public document in `/map_quote_pins/{pinId}` is automatically omitted or removed so that the pin is invisible on the global public map and exists exclusively in the Zero-Trust identity vault for permitted friends.
2. **Zero-Trust Friend-Gated Identity Vault (`/map_pin_identities/{pinId}`)**:
   - Stores the author's display name, email, personal marginalia note, connected circle names, and location coordinates alongside a per-friend precision map (`friendPrecisionLevels`).
   - **Firestore Security Rules Enforcement**: Read requests to `/map_pin_identities/{pinId}` are strictly rejected (`PERMISSION_DENIED`) unless the authenticated requester is the pin owner (`request.auth.uid == resource.data.ownerUid`) or their UID/email is explicitly present in the pin's approved `connectedUserUids` / `connectedUserEmails` access control lists.
   - **Per-Friend Location Precision**: When the owner sets a friend's precision level to `NEIGHBORHOOD`, the identity payload sanitizes coordinates for that connection to neighborhood precision while still revealing the friend's name and marginalia; when set to `EXACT_GPS`, permitted friends see the exact GPS pin and venue label.

---

## Key Features & User Flows

### 1. MapDiscoveryScreen & Daily Quote Pinning (`1/Day`)
- **Hybrid Google Maps SDK + Archival Atlas**:
  - Automatically detects whether a valid `MAPS_API_KEY` is provided in `BuildConfig` (configured via AI Studio Secrets). Supports switching between the Google Maps SDK view and the custom interactive Archival Cartographic Canvas at any time.
- **Once-a-Day Pin Quote Flow**:
  - Tapping **Pin Daily Quote** opens a streamlined composer pre-filled with the current Quote of the Day (or any saved quote from the user's Library), an optional personal marginalia reflection (supporting Gemini 3.5 voice transcription), and a live preview of their current Zero-Trust privacy settings.
  - Captures device location via runtime location permission (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`) or lets the user select a literary city preset if location permission is declined.
  - Enforces the 1-pin-per-UTC-day constraint both in the UI (showing a "Daily Pin Anchored" badge with countdown/update/delete controls) and in Firestore Security Rules.

### 2. PrivacyControlOverlay
- **Global Map Visibility Toggle**:
  - Switching **Global Map Visibility ON** publishes an anonymized cipher pin to the global map while sharing identity details only with permitted friends.
  - Switching **Global Map Visibility OFF** hides the pin from the public map entirely and shares the pinned quote and location exclusively with permitted friends in `/map_pin_identities`.
- **Per-Friend Location Sharing & Precision Controls**:
  - Aggregates members from the user's connected Reading Circles (`SalonCircle`).
  - For each friend, the user can toggle sharing ON/OFF and select between **Exact GPS & Venue** and **Coarse Neighborhood (~2.5 km)**.
  - Applying changes immediately updates the user's active daily pin ACLs (`connectedUserUids`, `connectedUserEmails`, and per-friend precision settings) in Firestore.
- **Trusted Connections Map Filter**:
  - Allows the user to toggle **"Trusted Connections Only"** so the map filters out public anonymous pins and highlights only pins from verified circle friends.

### 3. Home Screen Widget, Scheduled Daily Notifications & Library Folio Export
- **Home Screen Daily Quote Widget**:
  - Displays the Quote of the Day on an archival parchment card with its provenance timestamp (`ARCHIVED • YYYY.MM.DD • HH:MM UTC`), tapping through to launch the app or cycle the quote.
- **Scheduled 'Quote of the Day' Notification**:
  - Configurable daily delivery time (`HH:MM`) in Discovery & Settings using battery-safe inexact repeating alarms (`AlarmManager`) and Android 13+ runtime `POST_NOTIFICATIONS` permission handling.
- **Interactive Library Highlighting & Stylized PDF Journal Export**:
  - Word-tap passage highlighting inside saved Library quotes with four archival ink colors (`Amber Gold`, `Verdigris`, `Crimson`, `Cobalt`), timestamped marginalia notes, and one-tap local export to a stylized A4 PDF Journal Page (`Archival Parchment` or `Modern Monograph` theme) complete with an engraved Lyria ambient soundscape waveform scorecard.

---

## Verification & Polish
- **Zero-Trust Security Rules Verification**:
  - Validate `/map_quote_pins/{pinId}`, `/map_pin_identities/{pinId}`, and `/users/{userId}/saved_quotes/{quoteId}/annotations/{annotationId}` against the local Firestore Emulator (`firestore.test.js` and Robolectric rule tests) to prove that unconnected users receive `PERMISSION_DENIED` when attempting to read `/map_pin_identities`, while permitted friends in `connectedUserUids`/`connectedUserEmails` succeed, and that `pinId` strictly matches `{uid}_{dayKey}`.
  - Deploy updated `firestore.rules` using `deploy_firebase`.
- **Compilation & Policy Compliance**:
  - Verify all third-party dependencies with the Dependency Advisor tool (`restricted_check`) and run `compile_applet` to ensure zero build or lint issues.
