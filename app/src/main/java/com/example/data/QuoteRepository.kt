package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class QuoteRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun currentUserEmail(): String {
        return auth.currentUser?.email?.trim()?.lowercase().orEmpty()
    }

    private fun currentUserDisplayName(): String {
        val display = auth.currentUser?.displayName?.trim()
        if (!display.isNullOrBlank()) return display
        val email = currentUserEmail()
        return if (email.isNotBlank()) email.substringBefore("@") else "Family Archivist"
    }

    /**
     * Fetches the daily quote for [dayKey] from the Firestore `/daily_quotes` collection.
     * If today's document does not yet exist in the collection, seeds it with [fallbackQuote]
     * and returns the stored Firestore daily quote.
     */
    suspend fun fetchOrSeedDailyQuote(
        dayKey: String,
        fallbackQuote: QuoteItem
    ): Result<QuoteItem> {
        val sanitizedDayKey = dayKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
        val docId = "daily_$sanitizedDayKey"
        val docRef = db.collection("daily_quotes").document(docId)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val dailyDoc = snapshot.toObject(
                    DailyQuoteDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (dailyDoc != null && dailyDoc.text.isNotBlank()) {
                    return Result.success(dailyDoc.toQuoteItem())
                }
            }

            val boundedTags = fallbackQuote.tags.ifEmpty { listOf(fallbackQuote.category) }
                .map { it.take(60) }
                .take(15)
            val payload = mapOf(
                "text" to fallbackQuote.text.take(2000),
                "author" to fallbackQuote.author.take(200),
                "sourceWork" to fallbackQuote.sourceWork.take(300),
                "category" to fallbackQuote.category.take(100),
                "tags" to boundedTags,
                "onlinePopularityScore" to fallbackQuote.onlinePopularityScore.coerceIn(0, 100).toLong(),
                "popularOnlineContext" to fallbackQuote.popularOnlineContext.take(500),
                "era" to fallbackQuote.era.take(100),
                "dayKey" to sanitizedDayKey,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(fallbackQuote.copy(id = docId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeSavedQuotes(userId: String = requireUserId()): Flow<List<Quote>> = flow {
        val path = "users/$userId/saved_quotes"
        val collectionFlow = db.collection("users")
            .document(userId)
            .collection("saved_quotes")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Quote::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
        collectionFlow.collect { emit(it) }
    }

    suspend fun getSavedQuotes(userId: String = requireUserId()): Result<List<Quote>> {
        val path = "users/$userId/saved_quotes"
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("saved_quotes")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val list = snapshot.toObjects(
                Quote::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getSavedQuoteById(ownerUserId: String, quoteId: String): Result<Quote?> {
        val path = "users/$ownerUserId/saved_quotes/$quoteId"
        return try {
            val doc = db.collection("users")
                .document(ownerUserId)
                .collection("saved_quotes")
                .document(quoteId)
                .get()
                .await()
            val item = doc.toObject(
                Quote::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(item)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun saveQuote(
        quote: Quote,
        customDocumentId: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val candidateId = customDocumentId ?: quote.id.ifBlank {
            "sq_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        }
        val sanitizedId = candidateId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
        val docRef = db.collection("users")
            .document(uid)
            .collection("saved_quotes")
            .document(sanitizedId)

        val loc = quote.timestampInfo.location.ifBlank { "Archival Reading Room" }.take(300)
        val dateStr = quote.timestampInfo.date.ifBlank { "Nov 1, 2026" }.take(100)
        val timeStr = quote.timestampInfo.time.ifBlank { "9:00 AM" }.take(100)
        val capturedStr = quote.timestampInfo.capturedAt.ifBlank { "$dateStr at $timeStr" }.take(200)
        val boundedTags = quote.tags.ifEmpty { listOf(quote.category.ifBlank { "Philosophy" }) }
            .map { it.take(60) }
            .take(15)

        val payload = mapOf(
            "userId" to uid,
            "text" to quote.text.take(2000),
            "author" to quote.author.take(200),
            "tags" to boundedTags,
            "timestampInfo" to mapOf(
                "location" to loc,
                "date" to dateStr,
                "time" to timeStr,
                "capturedAt" to capturedStr
            ),
            "sourceWork" to quote.sourceWork.take(300),
            "category" to quote.category.ifBlank { boundedTags.first() }.take(100),
            "onlinePopularityScore" to quote.onlinePopularityScore.coerceIn(0L, 100L),
            "personalNote" to quote.personalNote.take(1000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(sanitizedId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveQuoteWithStamp(
        quote: QuoteItem,
        locationStamp: String,
        dateStamp: String,
        timeStamp: String,
        personalNote: String = "",
        customDocumentId: String? = null
    ): Result<String> {
        val loc = locationStamp.ifBlank { "Archival Reading Room" }.take(300)
        val dStamp = dateStamp.ifBlank { "Nov 1, 2026" }.take(100)
        val tStamp = timeStamp.ifBlank { "9:00 AM" }.take(100)
        val firestoreQuote = Quote(
            id = customDocumentId ?: "",
            text = quote.text,
            author = quote.author,
            tags = quote.tags.ifEmpty { listOf(quote.category) },
            timestampInfo = TimestampInfo(
                location = loc,
                date = dStamp,
                time = tStamp,
                capturedAt = "$dStamp at $tStamp"
            ),
            sourceWork = quote.sourceWork,
            category = quote.category,
            onlinePopularityScore = quote.onlinePopularityScore.toLong(),
            personalNote = personalNote
        )
        return saveQuote(firestoreQuote, customDocumentId)
    }

    suspend fun updatePersonalNote(quoteId: String, newNote: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users")
            .document(uid)
            .collection("saved_quotes")
            .document(quoteId)
        return try {
            docRef.update(
                mapOf(
                    "personalNote" to newNote.take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteSavedQuote(quoteId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users")
            .document(uid)
            .collection("saved_quotes")
            .document(quoteId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // Zero-Trust Family Social Circle (Google Connection)
    // =========================================================================

    fun observeFamilyCircles(userId: String = requireUserId()): Flow<List<FamilyCircle>> = flow {
        val path = "family_circles"
        val flowByUid = db.collection("family_circles")
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FamilyCircle::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
        flowByUid.collect { emit(it) }
    }

    /**
     * Queries any Family Circles where the signed-in user's Google email was invited,
     * and binds their authenticated Google UID into `memberUids` under zero-trust rules.
     */
    suspend fun syncGoogleEmailInvitesToCircles(): Result<Int> {
        val uid = requireUserId()
        val email = currentUserEmail()
        if (email.isBlank()) return Result.success(0)
        val path = "family_circles"
        return try {
            val invitedSnapshot = db.collection("family_circles")
                .whereArrayContains("memberEmails", email)
                .get()
                .await()
            var joinedCount = 0
            for (doc in invitedSnapshot.documents) {
                val circle = doc.toObject(
                    FamilyCircle::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: continue
                if (uid !in circle.memberUids && circle.memberUids.size < 20) {
                    val updatedUids = (circle.memberUids + uid).distinct().take(20)
                    doc.reference.update(
                        mapOf(
                            "memberUids" to updatedUids,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    joinedCount++
                }
            }
            Result.success(joinedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun createFamilyCircle(
        circleName: String,
        invitedGoogleEmails: List<String> = emptyList(),
        customCircleId: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val ownerEmail = currentUserEmail().ifBlank { "owner@google.user" }
        val rawId = customCircleId ?: "fc_${UUID.randomUUID().toString().replace("-", "").take(18)}"
        val sanitizedId = rawId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
        val docRef = db.collection("family_circles").document(sanitizedId)

        val cleanedEmails = (listOf(ownerEmail) + invitedGoogleEmails.map { it.trim().lowercase() })
            .filter { it.isNotBlank() && it.length <= 200 }
            .distinct()
            .take(20)

        val inviteCode = "FAM-${sanitizedId.takeLast(6).uppercase()}"
        val payload = mapOf(
            "name" to circleName.trim().ifBlank { "Family Literary Circle" }.take(100),
            "inviteCode" to inviteCode.take(32),
            "ownerUid" to uid,
            "ownerEmail" to ownerEmail.take(200),
            "memberUids" to listOf(uid),
            "memberEmails" to cleanedEmails,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(sanitizedId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun inviteGoogleMemberToCircle(
        circle: FamilyCircle,
        memberGoogleEmail: String,
        memberUidOptional: String? = null
    ): Result<Unit> {
        requireUserId()
        val normalizedEmail = memberGoogleEmail.trim().lowercase().take(200)
        if (normalizedEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Google email cannot be empty"))
        }
        val docRef = db.collection("family_circles").document(circle.id)
        val updatedEmails = (circle.memberEmails + normalizedEmail).distinct().take(20)
        val updatedUids = if (!memberUidOptional.isNullOrBlank()) {
            (circle.memberUids + memberUidOptional.trim()).distinct().take(20)
        } else {
            circle.memberUids
        }
        return try {
            docRef.update(
                mapOf(
                    "memberEmails" to updatedEmails,
                    "memberUids" to updatedUids,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getFamilyCircleById(circleId: String): Result<FamilyCircle?> {
        val path = "family_circles/$circleId"
        return try {
            val snapshot = db.collection("family_circles").document(circleId).get().await()
            val circle = snapshot.toObject(
                FamilyCircle::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(circle)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun shareQuoteToFamilyCircle(
        circle: FamilyCircle,
        quote: QuoteItem,
        timestampInfo: TimestampInfo,
        personalNote: String = "",
        customSharedQuoteId: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val senderEmail = currentUserEmail().ifBlank { circle.ownerEmail.ifBlank { "member@google.user" } }
        val senderName = currentUserDisplayName()
        val rawId = customSharedQuoteId ?: "fsq_${UUID.randomUUID().toString().replace("-", "").take(18)}"
        val sanitizedId = rawId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
        val docRef = db.collection("family_shared_quotes").document(sanitizedId)

        val allowedUids = (circle.memberUids + uid).distinct().take(20)
        val allowedEmails = (circle.memberEmails + senderEmail).filter { it.isNotBlank() }.distinct().take(20)
        val boundedTags = quote.tags.ifEmpty { listOf(quote.category) }.map { it.take(60) }.take(15)

        val loc = timestampInfo.location.ifBlank { "Family Reading Room" }.take(300)
        val dateStr = timestampInfo.date.ifBlank { "Nov 1, 2026" }.take(100)
        val timeStr = timestampInfo.time.ifBlank { "9:00 AM" }.take(100)
        val capturedStr = timestampInfo.capturedAt.ifBlank { "$dateStr at $timeStr" }.take(200)

        val payload = mapOf(
            "circleId" to circle.id.take(128),
            "circleName" to circle.name.ifBlank { "Family Circle" }.take(100),
            "senderUid" to uid,
            "senderName" to senderName.take(150),
            "senderEmail" to senderEmail.take(200),
            "allowedMemberUids" to allowedUids,
            "allowedEmails" to allowedEmails,
            "text" to quote.text.take(2000),
            "author" to quote.author.take(200),
            "tags" to boundedTags,
            "timestampInfo" to mapOf(
                "location" to loc,
                "date" to dateStr,
                "time" to timeStr,
                "capturedAt" to capturedStr
            ),
            "personalNote" to personalNote.take(1000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(sanitizedId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getFamilySharedQuoteById(sharedQuoteId: String): Result<FamilySharedQuote?> {
        val path = "family_shared_quotes/$sharedQuoteId"
        return try {
            val doc = db.collection("family_shared_quotes").document(sharedQuoteId).get().await()
            val item = doc.toObject(
                FamilySharedQuote::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(item)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    fun observeFamilySharedQuotes(userId: String = requireUserId()): Flow<List<FamilySharedQuote>> = flow {
        val path = "family_shared_quotes"
        val sharedFlow = db.collection("family_shared_quotes")
            .whereArrayContains("allowedMemberUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FamilySharedQuote::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
        sharedFlow.collect { emit(it) }
    }

    fun observeUserPreferences(userId: String = requireUserId()): Flow<UserPreferencesDocument?> = flow {
        val docRef = db.collection("users")
            .document(userId)
            .collection("preferences")
            .document("main")
        val prefFlow = docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserPreferencesDocument::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.GET, docRef.path)
                }
                throw error
            }
        prefFlow.collect { emit(it) }
    }

    suspend fun saveUserPreferences(
        selectedInterests: List<String>,
        priorityMode: String,
        recentQuoteIds: List<String>
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users")
            .document(uid)
            .collection("preferences")
            .document("main")

        val boundedInterests = selectedInterests.map { it.take(60) }.take(15)
        val validMode = if (priorityMode in listOf("Balanced", "Personal Interests", "Popular Online", "Recent & Trending")) {
            priorityMode
        } else {
            "Balanced"
        }
        val boundedRecent = recentQuoteIds.map { it.take(128) }.take(30)

        return try {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "selectedInterests" to boundedInterests,
                    "priorityMode" to validMode,
                    "recentQuoteIds" to boundedRecent,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }
}
