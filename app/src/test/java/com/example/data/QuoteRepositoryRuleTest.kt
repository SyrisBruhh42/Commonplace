package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private val sampleQuote = SeedQuoteCatalog.allQuotes.first()

    @Test
    fun saveQuoteWithStamp_validPayload_createsQuoteAndPersistsTimestampInfo() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = QuoteRepository(firestore, auth)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveQuoteWithStamp(
                quote = sampleQuote,
                locationStamp = "San Francisco, CA",
                dateStamp = "Nov 1, 2026",
                timeStamp = "9:15 AM",
                personalNote = "Morning reflection."
            )
        }
        assertTrue("Expected saveQuoteWithStamp to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)
        val quoteId = saveResult.getOrThrow()

        val fetchedResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getSavedQuoteById(aliceUid, quoteId)
        }
        assertTrue(fetchedResult.isSuccess)
        val doc = fetchedResult.getOrThrow()
        assertNotNull(doc)
        assertEquals(sampleQuote.text, doc?.text)
        assertEquals(sampleQuote.author, doc?.author)
        assertTrue(doc?.tags?.contains("Stoicism") == true)
        assertEquals("San Francisco, CA", doc?.timestampInfo?.location)
        assertEquals("Nov 1, 2026", doc?.timestampInfo?.date)
        assertEquals("9:15 AM", doc?.timestampInfo?.time)
    }

    @Test
    fun fetchOrSeedDailyQuote_authenticatedUser_fetchesDailyQuoteFromCollection() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = QuoteRepository(firestore, auth)

        val dailyResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.fetchOrSeedDailyQuote("2026_11_01", sampleQuote)
        }
        assertTrue("Expected fetchOrSeedDailyQuote to succeed: ${dailyResult.exceptionOrNull()}", dailyResult.isSuccess)
        val dailyQuote = dailyResult.getOrThrow()
        assertEquals(sampleQuote.text, dailyQuote.text)
        assertEquals(sampleQuote.author, dailyQuote.author)
    }

    @Test
    fun familySocialCircle_zeroTrustSharing_allowsInvitedGoogleMemberAndDeniesOutsider() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = QuoteRepository(firestore, auth)

        // Alice creates a Zero-Trust Family Circle inviting Bob by Google email
        val circleId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createFamilyCircle(
                circleName = "The Aurelius Family Circle",
                invitedGoogleEmails = listOf(BOB_EMAIL)
            ).getOrThrow()
        }

        // Bob signs in with Google email and syncs his invite to bind his UID
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = QuoteRepository(firestore, auth)
        val joinedCount = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.syncGoogleEmailInvitesToCircles().getOrThrow()
        }
        assertEquals(1, joinedCount)

        val updatedCircle = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getFamilyCircleById(circleId).getOrThrow()
        }
        assertNotNull(updatedCircle)
        assertTrue(updatedCircle!!.memberUids.contains(aliceUid))
        assertTrue(updatedCircle.memberUids.contains(bobUid))

        // Alice shares a quote with TimestampInfo to the Family Circle
        signInTestUser(ALICE_EMAIL)
        val sharedId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.shareQuoteToFamilyCircle(
                circle = updatedCircle,
                quote = sampleQuote,
                timestampInfo = TimestampInfo(
                    location = "San Francisco, CA",
                    date = "Nov 1, 2026",
                    time = "10:45 AM",
                    capturedAt = "Nov 1, 2026 at 10:45 AM"
                ),
                personalNote = "For our family reading circle."
            ).getOrThrow()
        }

        // Bob (circle member) can read the shared quote
        signInTestUser(BOB_EMAIL)
        val bobReadShared = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getFamilySharedQuoteById(sharedId)
        }
        assertTrue("Expected Bob to read family shared quote", bobReadShared.isSuccess)
        assertEquals("San Francisco, CA", bobReadShared.getOrThrow()?.timestampInfo?.location)

        // Outsider (Mallory, not in circle) is strictly denied access under Zero-Trust rules
        signInTestUser(OUTSIDER_EMAIL)
        val outsiderRepo = QuoteRepository(firestore, auth)
        val outsiderReadCircle = withTimeout(DEFAULT_TIMEOUT_MS) {
            outsiderRepo.getFamilyCircleById(circleId)
        }
        assertTrue("Outsider must be denied circle read", outsiderReadCircle.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            outsiderReadCircle.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        val outsiderReadSharedQuote = withTimeout(DEFAULT_TIMEOUT_MS) {
            outsiderRepo.getFamilySharedQuoteById(sharedId)
        }
        assertTrue("Outsider must be denied shared quote read", outsiderReadSharedQuote.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            outsiderReadSharedQuote.exceptionOrNull()?.unwrapFirestoreException()?.code
        )
    }

    @Test
    fun getSavedQuoteById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = QuoteRepository(firestore, auth)
        val quoteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveQuoteWithStamp(
                quote = sampleQuote,
                locationStamp = "Boston, MA",
                dateStamp = "Nov 1, 2026",
                timeStamp = "11:00 AM"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = QuoteRepository(firestore, auth)
        val crossRead = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getSavedQuoteById(aliceUid, quoteId)
        }
        assertTrue("Cross-user access must fail", crossRead.isFailure)
        val firestoreEx = crossRead.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeSavedQuotes_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = QuoteRepository(firestore, auth)

        var caughtException: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeSavedQuotes(aliceUid).first()
            }
        } catch (e: Throwable) {
            caughtException = e
        }
        assertNotNull("Expected unauthenticated observeSavedQuotes to throw", caughtException)
        val firestoreEx = caughtException?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun saveUserPreferences_authenticatedOwner_createsAndUpdatesPreferences() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = QuoteRepository(firestore, auth)

        val firstSave = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserPreferences(
                selectedInterests = listOf("Stoicism", "Literature"),
                priorityMode = "Balanced",
                recentQuoteIds = listOf(sampleQuote.id)
            )
        }
        assertTrue("First preferences save failed: ${firstSave.exceptionOrNull()}", firstSave.isSuccess)

        val secondSave = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserPreferences(
                selectedInterests = listOf("Stoicism", "Science", "Poetry"),
                priorityMode = "Popular Online",
                recentQuoteIds = listOf(sampleQuote.id)
            )
        }
        assertTrue("Second preferences save failed: ${secondSave.exceptionOrNull()}", secondSave.isSuccess)

        val emittedPrefs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserPreferences(aliceUid).first { it != null && it.priorityMode == "Popular Online" }
        }
        assertNotNull(emittedPrefs)
        assertEquals("Popular Online", emittedPrefs?.priorityMode)
        assertTrue(emittedPrefs?.selectedInterests?.contains("Science") == true)
    }

    @Test
    fun createOrJoinSymposiumRoom_andPostSalonMessage_succeedsForAuthenticatedUsers() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = QuoteRepository(firestore, auth)

        val roomRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createOrJoinSymposiumRoom(
                roomCode = "LFS-7K9M2P",
                title = "Stoic Co-Reading Salon",
                gameMode = MultiplayerGameMode.CO_READING_SALON,
                lifecyclePolicy = LfsLifecyclePolicy.ON_DEMAND_WAKE,
                currentQuote = sampleQuote
            )
        }
        assertTrue("Expected createOrJoinSymposiumRoom to succeed: ${roomRes.exceptionOrNull()}", roomRes.isSuccess)
        assertTrue(roomRes.getOrThrow().participantUids.contains(aliceUid))

        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = QuoteRepository(firestore, auth)
        val joinRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.createOrJoinSymposiumRoom(
                roomCode = "LFS-7K9M2P",
                title = "Stoic Co-Reading Salon",
                gameMode = MultiplayerGameMode.CO_READING_SALON,
                lifecyclePolicy = LfsLifecyclePolicy.ON_DEMAND_WAKE,
                currentQuote = sampleQuote
            )
        }
        assertTrue("Expected Bob to join symposium room: ${joinRes.exceptionOrNull()}", joinRes.isSuccess)
        assertTrue(joinRes.getOrThrow().participantUids.contains(bobUid))

        val postRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.postSalonMessage(
                roomCode = "LFS-7K9M2P",
                messageType = "MARGINALIA",
                text = "Seneca's letter speaks directly to modern anxiety.",
                quoteSnippet = "imagination than in reality",
                scholarHandle = "@archivist"
            )
        }
        assertTrue("Expected postSalonMessage to succeed: ${postRes.exceptionOrNull()}", postRes.isSuccess)
    }

    @Test
    fun pinDailyQuoteToWorldMap_enforcesOncePerDayAndZeroTrustFriendIdentityAcl() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = QuoteRepository(firestore, auth)

        val circleId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createFamilyCircle(
                circleName = "Trusted Map Circle",
                invitedGoogleEmails = listOf(BOB_EMAIL)
            ).getOrThrow()
        }

        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = QuoteRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.syncGoogleEmailInvitesToCircles().getOrThrow()
        }
        val sharedCircle = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getFamilyCircleById(circleId).getOrThrow()!!
        }

        // Alice pins her daily quote to the World Map with Bob permitted at NEIGHBORHOOD precision
        signInTestUser(ALICE_EMAIL)
        val dayKey = "2026_11_01"
        val friendPerms = listOf(
            FriendPrivacyPermission(
                friendId = "bob",
                displayName = "Bob",
                email = BOB_EMAIL,
                uid = bobUid,
                circleName = sharedCircle.name,
                isSharingEnabled = true,
                precision = FriendLocationPrecision.NEIGHBORHOOD
            )
        )

        val firstPinResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.pinDailyQuoteToWorldMap(
                dayKey = dayKey,
                quote = sampleQuote,
                latitude = 37.7749,
                longitude = -122.4194,
                regionLabel = "San Francisco, CA",
                exactLocationLabel = "San Francisco Conservatory • Nov 1, 2026",
                personalMarginalia = "Morning pin from SF.",
                connectedCircles = listOf(sharedCircle),
                globalMapVisibilityEnabled = true,
                friendPermissions = friendPerms
            )
        }
        assertTrue("Expected first daily pin to succeed: ${firstPinResult.exceptionOrNull()}", firstPinResult.isSuccess)
        val pinId = firstPinResult.getOrThrow()
        assertEquals("${aliceUid}_$dayKey", pinId)

        // Second pin on the same day must be rejected by the 1/day limit
        val secondPinResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.pinDailyQuoteToWorldMap(
                dayKey = dayKey,
                quote = sampleQuote,
                latitude = 37.7749,
                longitude = -122.4194,
                regionLabel = "San Francisco, CA",
                exactLocationLabel = "San Francisco Conservatory",
                personalMarginalia = "Second attempt same day",
                connectedCircles = listOf(sharedCircle),
                globalMapVisibilityEnabled = true,
                friendPermissions = friendPerms
            )
        }
        assertTrue("Expected second pin on same day to fail", secondPinResult.isFailure)

        // Connected friend (Bob) can read Alice's Zero-Trust identity envelope
        signInTestUser(BOB_EMAIL)
        val bobIdentityRead = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getPinIdentityIfConnected(pinId)
        }
        assertTrue("Expected connected friend Bob to read identity: ${bobIdentityRead.exceptionOrNull()}", bobIdentityRead.isSuccess)
        assertNotNull(bobIdentityRead.getOrThrow())

        // Outsider (Mallory) is denied access to Alice's Zero-Trust identity envelope
        signInTestUser(OUTSIDER_EMAIL)
        val outsiderRepo = QuoteRepository(firestore, auth)
        val outsiderIdentityRead = withTimeout(DEFAULT_TIMEOUT_MS) {
            outsiderRepo.getPinIdentityIfConnected(pinId)
        }
        assertTrue("Expected outsider Mallory to be denied identity read", outsiderIdentityRead.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            outsiderIdentityRead.exceptionOrNull()?.unwrapFirestoreException()?.code
        )
    }

    private companion object {
        const val ALICE_EMAIL = "alice@commonplace.test"
        const val BOB_EMAIL = "bob@commonplace.test"
        const val OUTSIDER_EMAIL = "mallory@commonplace.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
