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

    fun currentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun currentUserEmail(): String {
        return auth.currentUser?.email?.trim()?.lowercase().orEmpty()
    }

    fun currentUserDisplayName(): String {
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
    // Time-Stamped Quote Passage Highlights & Annotations
    // (/users/{userId}/saved_quotes/{quoteId}/annotations/{annotationId})
    // =========================================================================

    suspend fun addQuoteHighlightAnnotation(
        quoteId: String,
        highlightedText: String,
        startCharIndex: Int,
        endCharIndex: Int,
        inkColor: HighlightInkColor,
        noteText: String,
        timestampLabel: String,
        customAnnotationId: String? = null
    ): Result<QuoteHighlightAnnotation> {
        val uid = requireUserId()
        val sanitizedQuoteId = quoteId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
        val rawId = customAnnotationId ?: "hl_${UUID.randomUUID().toString().replace("-", "").take(18)}"
        val sanitizedAnnotationId = rawId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)

        val safeStart = startCharIndex.coerceIn(0, 2000)
        val safeEnd = endCharIndex.coerceIn(safeStart, 2000)
        val safeHighlighted = highlightedText.trim().ifBlank { "Selected passage" }.take(1000)
        val safeNote = noteText.trim().ifBlank { "Highlighted passage annotation." }.take(1000)
        val safeTimestamp = timestampLabel.trim().ifBlank { "Nov 1, 2026 • 12:20 PM" }.take(120)

        val docRef = db.collection("users")
            .document(uid)
            .collection("saved_quotes")
            .document(sanitizedQuoteId)
            .collection("annotations")
            .document(sanitizedAnnotationId)

        val payload = mapOf(
            "quoteId" to sanitizedQuoteId,
            "userId" to uid,
            "highlightedText" to safeHighlighted,
            "startCharIndex" to safeStart.toLong(),
            "endCharIndex" to safeEnd.toLong(),
            "inkColor" to inkColor.wireId,
            "noteText" to safeNote,
            "timestampLabel" to safeTimestamp,
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(
                QuoteHighlightAnnotation(
                    id = sanitizedAnnotationId,
                    quoteId = sanitizedQuoteId,
                    userId = uid,
                    highlightedText = safeHighlighted,
                    startCharIndex = safeStart.toLong(),
                    endCharIndex = safeEnd.toLong(),
                    inkColor = inkColor.wireId,
                    noteText = safeNote,
                    timestampLabel = safeTimestamp
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getQuoteHighlightAnnotations(
        quoteId: String,
        ownerUserId: String = requireUserId()
    ): Result<List<QuoteHighlightAnnotation>> {
        val sanitizedQuoteId = quoteId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
        val path = "users/$ownerUserId/saved_quotes/$sanitizedQuoteId/annotations"
        return try {
            val snapshot = db.collection("users")
                .document(ownerUserId)
                .collection("saved_quotes")
                .document(sanitizedQuoteId)
                .collection("annotations")
                .whereEqualTo("userId", ownerUserId)
                .get()
                .await()
            val list = snapshot.toObjects(
                QuoteHighlightAnnotation::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedBy { it.startCharIndex }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    fun observeQuoteHighlightAnnotations(
        quoteId: String,
        userId: String = requireUserId()
    ): Flow<List<QuoteHighlightAnnotation>> = flow {
        val sanitizedQuoteId = quoteId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
        val path = "users/$userId/saved_quotes/$sanitizedQuoteId/annotations"
        val flowRef = db.collection("users")
            .document(userId)
            .collection("saved_quotes")
            .document(sanitizedQuoteId)
            .collection("annotations")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    QuoteHighlightAnnotation::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.startCharIndex }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
        flowRef.collect { emit(it) }
    }

    suspend fun deleteQuoteHighlightAnnotation(
        quoteId: String,
        annotationId: String
    ): Result<Unit> {
        val uid = requireUserId()
        val sanitizedQuoteId = quoteId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
        val sanitizedAnnotationId = annotationId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
        val docRef = db.collection("users")
            .document(uid)
            .collection("saved_quotes")
            .document(sanitizedQuoteId)
            .collection("annotations")
            .document(sanitizedAnnotationId)
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

    // =========================================================================
    // Tier-B Cloud Salon & Literary Trivia Relay (/symposium_rooms/{roomCode})
    // =========================================================================

    suspend fun createOrJoinSymposiumRoom(
        roomCode: String,
        title: String,
        gameMode: MultiplayerGameMode,
        lifecyclePolicy: LfsLifecyclePolicy,
        currentQuote: QuoteItem,
        activeScholars: List<String> = listOf("@archivist", "@willow", "@mote")
    ): Result<MultiplayerSalonDocument> {
        val uid = requireUserId()
        val displayName = currentUserDisplayName()
        val sanitizedCode = roomCode.uppercase()
            .replace(Regex("[^A-Z0-9_\\-]"), "")
            .ifBlank { "LFS-7K9M2P" }
            .take(32)
        val docRef = db.collection("symposium_rooms").document(sanitizedCode)

        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existingDoc = existingSnap.toObject(
                    MultiplayerSalonDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existingDoc != null) {
                    val updatedUids = (existingDoc.participantUids + uid).distinct().take(32)
                    val updatedNames = (existingDoc.participantNames + displayName.take(120)).distinct().take(32)
                    docRef.update(
                        mapOf(
                            "participantUids" to updatedUids,
                            "participantNames" to updatedNames,
                            "gameMode" to gameMode.name,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    return Result.success(
                        existingDoc.copy(
                            id = sanitizedCode,
                            participantUids = updatedUids,
                            participantNames = updatedNames,
                            gameMode = gameMode.name
                        )
                    )
                }
            }

            val boundedScholars = activeScholars.map { it.take(40) }.take(6)
            val payload = mapOf(
                "roomCode" to sanitizedCode,
                "title" to title.trim().ifBlank { "The Archival Co-Reading Salon" }.take(140),
                "hostUid" to uid,
                "hostName" to displayName.take(120),
                "gameMode" to gameMode.name,
                "lifecyclePolicy" to lifecyclePolicy.wireId,
                "currentQuoteId" to currentQuote.id.ifBlank { "q_1" }.take(128),
                "currentQuoteText" to currentQuote.text.ifBlank { "We suffer more often in imagination than in reality." }.take(2000),
                "currentQuoteAuthor" to currentQuote.author.ifBlank { "Seneca" }.take(200),
                "participantUids" to listOf(uid),
                "participantNames" to listOf(displayName.take(120)),
                "activeScholars" to boundedScholars,
                "roundNumber" to 1L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(
                MultiplayerSalonDocument(
                    id = sanitizedCode,
                    roomCode = sanitizedCode,
                    title = title.trim().ifBlank { "The Archival Co-Reading Salon" }.take(140),
                    hostUid = uid,
                    hostName = displayName.take(120),
                    gameMode = gameMode.name,
                    lifecyclePolicy = lifecyclePolicy.wireId,
                    currentQuoteId = currentQuote.id,
                    currentQuoteText = currentQuote.text,
                    currentQuoteAuthor = currentQuote.author,
                    participantUids = listOf(uid),
                    participantNames = listOf(displayName.take(120)),
                    activeScholars = boundedScholars,
                    roundNumber = 1L
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeSymposiumRoom(roomCode: String): Flow<MultiplayerSalonDocument?> = flow {
        requireUserId()
        val sanitizedCode = roomCode.uppercase().replace(Regex("[^A-Z0-9_\\-]"), "").take(32)
        val docRef = db.collection("symposium_rooms").document(sanitizedCode)
        val roomFlow = docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        MultiplayerSalonDocument::class.java,
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
        roomFlow.collect { emit(it) }
    }

    suspend fun postSalonMessage(
        roomCode: String,
        messageType: String,
        text: String,
        quoteSnippet: String = "",
        scholarHandle: String = "",
        triviaDeltaScore: Int = 0
    ): Result<String> {
        val uid = requireUserId()
        val senderName = currentUserDisplayName()
        val sanitizedCode = roomCode.uppercase().replace(Regex("[^A-Z0-9_\\-]"), "").take(32)
        val msgId = "smsg_${UUID.randomUUID().toString().replace("-", "").take(18)}"
        val docRef = db.collection("symposium_rooms")
            .document(sanitizedCode)
            .collection("messages")
            .document(msgId)

        val validType = if (messageType in listOf("MARGINALIA", "TRIVIA", "CHAT", "SCHOLAR")) {
            messageType
        } else {
            "MARGINALIA"
        }

        val payload = mapOf(
            "roomCode" to sanitizedCode,
            "senderUid" to uid,
            "senderName" to senderName.take(120),
            "messageType" to validType,
            "text" to text.trim().ifBlank { "Shared a marginalia highlight." }.take(2000),
            "quoteSnippet" to quoteSnippet.take(400),
            "scholarHandle" to scholarHandle.take(40),
            "triviaDeltaScore" to triviaDeltaScore.coerceIn(-500, 500).toLong(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(msgId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeSalonMessages(roomCode: String): Flow<List<SalonMessageDocument>> = flow {
        requireUserId()
        val sanitizedCode = roomCode.uppercase().replace(Regex("[^A-Z0-9_\\-]"), "").take(32)
        val path = "symposium_rooms/$sanitizedCode/messages"
        val messagesFlow = db.collection("symposium_rooms")
            .document(sanitizedCode)
            .collection("messages")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SalonMessageDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
        messagesFlow.collect { emit(it) }
    }

    // =========================================================================
    // Zero-Trust Multi-User Location-Based Quote Map (/map_quote_pins & /map_pin_identities)
    // =========================================================================

    fun buildDailyPinId(userId: String, dayKey: String): String {
        val cleanDayKey = dayKey.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "2026_11_01" }.take(32)
        return "${userId}_$cleanDayKey"
    }

    /**
     * Pins a quote to the shared visual map once per day.
     * - Public `/map_quote_pins/{uid}_{dayKey}` stores ONLY the quote and coarse coordinates (NO PII).
     * - Private `/map_pin_identities/{uid}_{dayKey}` stores the user's identity, exact location stamp,
     *   and personal marginalia, gated by `connectedUserUids` and `connectedUserEmails`.
     */
    suspend fun pinDailyQuoteToWorldMap(
        dayKey: String,
        quote: QuoteItem,
        latitude: Double,
        longitude: Double,
        regionLabel: String,
        exactLocationLabel: String = regionLabel,
        personalMarginalia: String = "",
        connectedCircles: List<FamilyCircle> = emptyList(),
        additionalConnectedUids: List<String> = emptyList(),
        additionalConnectedEmails: List<String> = emptyList(),
        globalMapVisibilityEnabled: Boolean = true,
        friendPermissions: List<FriendPrivacyPermission> = emptyList()
    ): Result<String> {
        val uid = requireUserId()
        val ownerEmail = currentUserEmail()
        val ownerName = currentUserDisplayName()
        val sanitizedDayKey = dayKey.trim()
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .let { if (it.length < 8) "2026_11_01" else it.take(32) }
        val pinId = "${uid}_$sanitizedDayKey"

        val pinDocRef = db.collection("map_quote_pins").document(pinId)
        val identityDocRef = db.collection("map_pin_identities").document(pinId)

        val coarseLat = ZeroTrustMapCoordinatesHelper.roundToCoarseGrid(latitude.coerceIn(-90.0, 90.0))
        val coarseLon = ZeroTrustMapCoordinatesHelper.roundToCoarseGrid(longitude.coerceIn(-180.0, 180.0))
        val alias = ZeroTrustMapCoordinatesHelper.buildAnonymousLiteraryAlias(uid, sanitizedDayKey)

        // Filter out any friends that the user explicitly disabled in PrivacyControlOverlay
        val disabledEmails = friendPermissions
            .filter { !it.isSharingEnabled }
            .map { it.email.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
        val disabledUids = friendPermissions
            .filter { !it.isSharingEnabled }
            .map { it.uid.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        val enabledFriendUids = friendPermissions
            .filter { it.isSharingEnabled && it.uid.isNotBlank() }
            .map { it.uid.trim() }
        val enabledFriendEmails = friendPermissions
            .filter { it.isSharingEnabled && it.email.isNotBlank() }
            .map { it.email.trim().lowercase() }

        val connectedUids = buildList {
            add(uid)
            connectedCircles.forEach { circle ->
                addAll(circle.memberUids.filter { it == uid || it !in disabledUids })
            }
            addAll(additionalConnectedUids.filter { it == uid || it !in disabledUids })
            addAll(enabledFriendUids)
        }.map { it.trim().take(128) }.filter { it.isNotEmpty() }.distinct().take(50)

        val connectedEmails = buildList {
            add(ownerEmail)
            connectedCircles.forEach { circle ->
                addAll(circle.memberEmails.filter {
                    val lower = it.trim().lowercase()
                    lower == ownerEmail.lowercase() || lower !in disabledEmails
                })
            }
            addAll(additionalConnectedEmails.filter {
                val lower = it.trim().lowercase()
                lower == ownerEmail.lowercase() || lower !in disabledEmails
            })
            addAll(enabledFriendEmails)
        }.map { it.trim().lowercase().take(200) }.filter { it.isNotEmpty() }.distinct().take(50)

        val rawCircleNames = connectedCircles
            .map { it.name.trim().take(100) }
            .filter { it.isNotEmpty() }
            .distinct()
        val circleNamesWithPrecision = ZeroTrustMapCoordinatesHelper.buildCircleNamesWithPrecisionTags(
            circleNames = rawCircleNames,
            friendPermissions = friendPermissions
        )

        val encodedExactLocation = ZeroTrustMapCoordinatesHelper.encodeIdentityLocationEnvelope(
            cleanLocationLabel = exactLocationLabel.trim().ifBlank { regionLabel.ifBlank { "Personal Study" } },
            exactLat = latitude,
            exactLon = longitude,
            isHiddenFromPublic = !globalMapVisibilityEnabled,
            quoteId = quote.id.ifBlank { "q_daily" },
            quoteAuthor = quote.author.ifBlank { "Seneca" },
            quoteText = quote.text.ifBlank { "We suffer more often in imagination than in reality." }
        )

        val publicPinPayload = mapOf(
            "ownerUid" to uid,
            "dayKey" to sanitizedDayKey,
            "quoteId" to quote.id.ifBlank { "q_daily" }.take(128),
            "quoteText" to quote.text.ifBlank { "We suffer more often in imagination than in reality." }.take(2000),
            "quoteAuthor" to quote.author.ifBlank { "Seneca" }.take(200),
            "sourceWork" to quote.sourceWork.take(300),
            "category" to quote.category.ifBlank { "Philosophy" }.take(100),
            "latitude" to coarseLat,
            "longitude" to coarseLon,
            "regionLabel" to regionLabel.trim().ifBlank { "Global Literary Commons" }.take(200),
            "anonymousAlias" to alias.take(100),
            "createdAt" to FieldValue.serverTimestamp()
        )

        val identityPayload = mapOf(
            "pinId" to pinId,
            "ownerUid" to uid,
            "ownerDisplayName" to ownerName.ifBlank { "Reader" }.take(150),
            "ownerEmail" to ownerEmail.ifBlank { "$uid@commonplace.local" }.take(200),
            "circleNames" to circleNamesWithPrecision,
            "personalMarginalia" to personalMarginalia.trim().take(1000),
            "exactLocationLabel" to encodedExactLocation,
            "connectedUserUids" to connectedUids,
            "connectedUserEmails" to connectedEmails,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            val existingPin = pinDocRef.get().await()
            val existingIdentity = identityDocRef.get().await()
            if (existingPin.exists() || existingIdentity.exists()) {
                return Result.failure(
                    IllegalStateException("Daily Pin Limit Reached: You have already pinned a quote today ($sanitizedDayKey).")
                )
            }
            if (globalMapVisibilityEnabled) {
                pinDocRef.set(publicPinPayload).await()
            }
            identityDocRef.set(identityPayload).await()
            Result.success(pinId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, pinDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncGlobalMapVisibilityForTodayPin(
        dayKey: String,
        quote: QuoteItem,
        latitude: Double,
        longitude: Double,
        regionLabel: String,
        globalMapVisibilityEnabled: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        val sanitizedDayKey = dayKey.trim()
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .let { if (it.length < 8) "2026_11_01" else it.take(32) }
        val pinId = "${uid}_$sanitizedDayKey"
        val pinDocRef = db.collection("map_quote_pins").document(pinId)

        return try {
            val existingPin = pinDocRef.get().await()
            if (!globalMapVisibilityEnabled) {
                if (existingPin.exists()) {
                    pinDocRef.delete().await()
                }
            } else {
                if (!existingPin.exists()) {
                    val coarseLat = ZeroTrustMapCoordinatesHelper.roundToCoarseGrid(latitude.coerceIn(-90.0, 90.0))
                    val coarseLon = ZeroTrustMapCoordinatesHelper.roundToCoarseGrid(longitude.coerceIn(-180.0, 180.0))
                    val alias = ZeroTrustMapCoordinatesHelper.buildAnonymousLiteraryAlias(uid, sanitizedDayKey)
                    val publicPinPayload = mapOf(
                        "ownerUid" to uid,
                        "dayKey" to sanitizedDayKey,
                        "quoteId" to quote.id.ifBlank { "q_daily" }.take(128),
                        "quoteText" to quote.text.ifBlank { "We suffer more often in imagination than in reality." }.take(2000),
                        "quoteAuthor" to quote.author.ifBlank { "Seneca" }.take(200),
                        "sourceWork" to quote.sourceWork.take(300),
                        "category" to quote.category.ifBlank { "Philosophy" }.take(100),
                        "latitude" to coarseLat,
                        "longitude" to coarseLon,
                        "regionLabel" to regionLabel.trim().ifBlank { "Global Literary Commons" }.take(200),
                        "anonymousAlias" to alias.take(100),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    pinDocRef.set(publicPinPayload).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, pinDocRef.path)
            Result.failure(e)
        }
    }

    fun observePublicMapQuotePins(): Flow<List<MapQuotePinDocument>> = flow {
        requireUserId()
        val path = "map_quote_pins"
        val pinsFlow = db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MapQuotePinDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
        pinsFlow.collect { emit(it) }
    }

    fun observeConnectedPinIdentities(userId: String = requireUserId()): Flow<List<MapPinIdentityDocument>> = flow {
        val path = "map_pin_identities"
        val idFlow = db.collection(path)
            .whereArrayContains("connectedUserUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MapPinIdentityDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
        idFlow.collect { emit(it) }
    }

    suspend fun getPublicMapQuotePin(pinId: String): Result<MapQuotePinDocument?> {
        requireUserId()
        val docRef = db.collection("map_quote_pins").document(pinId)
        return try {
            val snap = docRef.get().await()
            val doc = if (snap.exists()) {
                snap.toObject(
                    MapQuotePinDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else null
            Result.success(doc)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getPinIdentityIfConnected(pinId: String): Result<MapPinIdentityDocument?> {
        requireUserId()
        val docRef = db.collection("map_pin_identities").document(pinId)
        return try {
            val snap = docRef.get().await()
            val doc = if (snap.exists()) {
                snap.toObject(
                    MapPinIdentityDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else null
            Result.success(doc)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePinIdentityConnections(
        pinId: String,
        connectedCircles: List<FamilyCircle>,
        additionalConnectedUids: List<String> = emptyList(),
        additionalConnectedEmails: List<String> = emptyList(),
        updatedMarginalia: String? = null,
        friendPermissions: List<FriendPrivacyPermission> = emptyList()
    ): Result<Unit> {
        val uid = requireUserId()
        val ownerEmail = currentUserEmail()
        val docRef = db.collection("map_pin_identities").document(pinId)

        val disabledEmails = friendPermissions
            .filter { !it.isSharingEnabled }
            .map { it.email.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
        val disabledUids = friendPermissions
            .filter { !it.isSharingEnabled }
            .map { it.uid.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        val enabledFriendUids = friendPermissions
            .filter { it.isSharingEnabled && it.uid.isNotBlank() }
            .map { it.uid.trim() }
        val enabledFriendEmails = friendPermissions
            .filter { it.isSharingEnabled && it.email.isNotBlank() }
            .map { it.email.trim().lowercase() }

        val connectedUids = buildList {
            add(uid)
            connectedCircles.forEach { circle ->
                addAll(circle.memberUids.filter { it == uid || it !in disabledUids })
            }
            addAll(additionalConnectedUids.filter { it == uid || it !in disabledUids })
            addAll(enabledFriendUids)
        }.map { it.trim().take(128) }.filter { it.isNotEmpty() }.distinct().take(50)

        val connectedEmails = buildList {
            add(ownerEmail)
            connectedCircles.forEach { circle ->
                addAll(circle.memberEmails.filter {
                    val lower = it.trim().lowercase()
                    lower == ownerEmail.lowercase() || lower !in disabledEmails
                })
            }
            addAll(additionalConnectedEmails.filter {
                val lower = it.trim().lowercase()
                lower == ownerEmail.lowercase() || lower !in disabledEmails
            })
            addAll(enabledFriendEmails)
        }.map { it.trim().lowercase().take(200) }.filter { it.isNotEmpty() }.distinct().take(50)

        val rawCircleNames = connectedCircles
            .map { it.name.trim().take(100) }
            .filter { it.isNotEmpty() }
            .distinct()
        val circleNamesWithPrecision = ZeroTrustMapCoordinatesHelper.buildCircleNamesWithPrecisionTags(
            circleNames = rawCircleNames,
            friendPermissions = friendPermissions
        )

        val updates = mutableMapOf<String, Any>(
            "connectedUserUids" to connectedUids,
            "connectedUserEmails" to connectedEmails,
            "circleNames" to circleNamesWithPrecision,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (updatedMarginalia != null) {
            updates["personalMarginalia"] = updatedMarginalia.trim().take(1000)
        }

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMapQuotePin(pinId: String): Result<Unit> {
        requireUserId()
        val pinRef = db.collection("map_quote_pins").document(pinId)
        val idRef = db.collection("map_pin_identities").document(pinId)
        return try {
            pinRef.delete().await()
            idRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, pinRef.path)
            Result.failure(e)
        }
    }
}
