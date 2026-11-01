# Security Specification (Payload-First Security TDD)

## 1. Data Invariants
- **Multi-User Isolation**: Every document under `/users/{userId}/saved_quotes/{quoteId}` and `/users/{userId}/preferences/{prefId}` belongs strictly to `userId` (`request.auth.uid == userId` and `resource.data.userId == request.auth.uid`).
- **Path Variable Hardening**: `userId`, `quoteId`, and `prefId` must satisfy `isValidId()` (`size() <= 128` and `matches('^[a-zA-Z0-9_\\-]+$')`).
- **Mandatory Stamp & Schema Validation**: Every `SavedQuote` must include non-empty `quoteText`, `author`, `category`, `locationStamp`, `dateStamp`, `timeStamp`, bounded `onlinePopularityScore` (`1..100`), bounded `personalNote` (`<= 1000` chars), and valid server timestamps (`createdAt <= request.time`, `updatedAt <= request.time`).
- **Immortal Fields**: `userId` and `createdAt` cannot be modified on `update`.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: `get` or `list` on `/users/alice_123/saved_quotes` with `auth == null` -> REJECT.
2. **Cross-User Read**: Bob (`bob_456`) attempts `get` or `list` on `/users/alice_123/saved_quotes` -> REJECT.
3. **Identity Spoofing on Create**: Alice creates a quote under `/users/alice_123/saved_quotes/q1` with `userId: "bob_456"` -> REJECT.
4. **Shadow/Ghost Field Injection**: Alice includes `"isAdmin": true` in `SavedQuote` create or update -> REJECT via `hasOnly()`.
5. **Missing Provenance Stamp**: Alice creates `SavedQuote` without `locationStamp`, `dateStamp`, or `timeStamp` -> REJECT.
6. **String Overflow (Denial of Wallet)**: Alice submits a 5000-character `quoteText` or 2000-character `personalNote` -> REJECT.
7. **Out-of-Bounds Popularity Score**: Alice sets `onlinePopularityScore: 999` or `-5` -> REJECT.
8. **Future Timestamp Spoofing**: Client sends `createdAt` in the future (`> request.time`) -> REJECT.
9. **Immortal Field Mutation**: Alice attempts to change `createdAt` or `userId` during `update` -> REJECT.
10. **Value Poisoning on Update**: Alice updates `personalNote` with an integer `12345` instead of string -> REJECT via `isValidSavedQuote(incoming())`.
11. **Unbounded Array in Preferences**: Alice submits 50 items in `selectedInterests` (exceeding max 15) -> REJECT.
12. **Invalid Enum in Priority Mode**: Alice submits `priorityMode: "HackMode"` -> REJECT.
