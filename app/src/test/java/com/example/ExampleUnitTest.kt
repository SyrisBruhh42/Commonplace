package com.example

import com.example.data.CommonplaceTransportMode
import com.example.data.LfsHostMode
import com.example.data.LfsLifecyclePolicy
import com.example.data.LfsServerEndpointConfig
import com.example.data.LfsServerPreset
import com.example.data.MultiplayerGameMode
import com.example.data.NativeArcadeAssetBridge
import com.example.data.PhilosophicalDoctrine
import com.example.data.SeedQuoteCatalog
import com.example.data.TotalOrderLwwCell
import com.example.network.CommonplaceCryptoEnvelope
import com.example.network.OasisCommonplaceServerClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test


class ExampleUnitTest {

    @Test
    fun crockfordRoomCode_generationAndNormalization_areCanonical() {
        val code = OasisCommonplaceServerClient.generateRoomCode()
        assertTrue("Generated room code must be valid LFS-XXXXXX", OasisCommonplaceServerClient.isValidRoomCode(code))

        val normalized = OasisCommonplaceServerClient.normalizeRoomCode("lfs-ilo9ab")
        assertEquals("LFS-1109AB", normalized)
        assertTrue(OasisCommonplaceServerClient.isValidRoomCode(normalized))
    }

    @Test
    fun commonplaceCryptoEnvelope_sealAndOpen_roundTripsAndRejectsWrongPassphrase() {
        val secretPassage = "We suffer more often in imagination than in reality. — Seneca"
        val pass = "stoic-lantern-2026"

        val sealedResult = CommonplaceCryptoEnvelope.sealFolioEnvelope(secretPassage, pass)
        assertTrue(sealedResult.isSuccess)
        val envelope = sealedResult.getOrThrow()
        assertTrue(envelope.startsWith("CPF1:"))
        assertNotEquals(secretPassage, envelope)

        val openedResult = CommonplaceCryptoEnvelope.openFolioEnvelope(envelope, pass)
        assertTrue(openedResult.isSuccess)
        assertEquals(secretPassage, openedResult.getOrThrow())

        val wrongPassResult = CommonplaceCryptoEnvelope.openFolioEnvelope(envelope, "wrong-passphrase")
        assertTrue("Wrong passphrase must fail GCM authentication", wrongPassResult.isFailure)
    }

    @Test
    fun tailscaleSecretGate_hidesAndBypassesLfsServerUntilValidTailscaleDetected() = runBlocking {
        // 1. Standard LAN / Wi-Fi interface without Tailscale (e.g. 192.168.1.50)
        val standardLanStatus = OasisCommonplaceServerClient.detectTailscaleConnection(
            simulatedInterfaces = listOf("wlan0" to listOf("192.168.1.50", "fe80::1"))
        )
        assertFalse("Standard Wi-Fi IP must not trigger Tailscale detection", standardLanStatus.isDetected)

        val publicModes = OasisCommonplaceServerClient.availableTransportModes(isTailscaleConnected = false)
        assertFalse(publicModes.contains(CommonplaceTransportMode.LFS_HOSTED_SERVER))
        assertFalse(publicModes.contains(CommonplaceTransportMode.LFS_LOCAL_HOST_TUNNEL))
        assertTrue(publicModes.contains(CommonplaceTransportMode.FIRESTORE_CLOUD_RELAY))
        assertTrue(publicModes.contains(CommonplaceTransportMode.LAN_DIRECT_SOCKET))

        val client = OasisCommonplaceServerClient()
        // Even if caller requests LFS_HOSTED_SERVER while Tailscale is false, it must downgrade to FIRESTORE_CLOUD_RELAY
        val downgradedHandle = client.provisionOrJoinRoom(
            requestedRoomCode = "LFS-7K9M2P",
            roomTitle = "Standard Salon",
            hostMode = LfsHostMode.LAUNCH_U2_HOSTING_SERVICE,
            lifecyclePolicy = LfsLifecyclePolicy.ON_DEMAND_WAKE,
            preferredTransport = CommonplaceTransportMode.LFS_HOSTED_SERVER,
            gameMode = MultiplayerGameMode.CO_READING_SALON,
            endpointConfig = LfsServerEndpointConfig(),
            userId = "u_1",
            userDisplayName = "Reader",
            isTailscaleConnected = false
        )
        assertEquals(CommonplaceTransportMode.FIRESTORE_CLOUD_RELAY, downgradedHandle.activeTransport)
        assertEquals(LfsHostMode.HOST_LOCAL_DEVICE, downgradedHandle.hostMode)

        // 2. Valid Tailscale CGNAT interface (100.64.0.0/10 on tailscale0/tun0)
        val tailscaleStatus = OasisCommonplaceServerClient.detectTailscaleConnection(
            simulatedInterfaces = listOf(
                "wlan0" to listOf("192.168.1.50"),
                "tailscale0" to listOf("100.101.42.88", "fd7a:115c:a1e0::1234")
            )
        )
        assertTrue("Valid Tailscale 100.64.0.0/10 address must be detected", tailscaleStatus.isDetected)
        assertEquals("100.101.42.88", tailscaleStatus.tailnetIp)
        assertEquals("tailscale0", tailscaleStatus.interfaceName)

        val unlockedModes = OasisCommonplaceServerClient.availableTransportModes(isTailscaleConnected = true)
        assertTrue(unlockedModes.contains(CommonplaceTransportMode.LFS_HOSTED_SERVER))
        assertTrue(unlockedModes.contains(CommonplaceTransportMode.LFS_LOCAL_HOST_TUNNEL))
    }

    @Test
    fun residentScholarsAndTriviaGenerator_produceValidLiteraryOutputs() {
        val client = OasisCommonplaceServerClient()
        val quote = SeedQuoteCatalog.allQuotes.first()

        val archivistNote = client.generateResidentScholarResponse("@archivist", quote, "Context please")
        val willowNote = client.generateResidentScholarResponse("@willow", quote, "Poetic cadence")
        val moteNote = client.generateResidentScholarResponse("@mote", quote, "Hint please")

        assertEquals("@archivist", archivistNote.scholarHandle)
        assertEquals("@willow", willowNote.scholarHandle)
        assertEquals("@mote", moteNote.scholarHandle)
        assertTrue(archivistNote.marginNote.contains(quote.author))

        val round1 = client.generateTriviaRound(1, 5, SeedQuoteCatalog.allQuotes)
        val round2 = client.generateTriviaRound(2, 5, SeedQuoteCatalog.allQuotes)
        val round3 = client.generateTriviaRound(3, 5, SeedQuoteCatalog.allQuotes)

        assertEquals(4, round1.options.size)
        assertEquals(4, round2.options.size)
        assertEquals(4, round3.options.size)
        assertTrue(round1.correctOptionIndex in 0..3)
    }

    @Test
    fun networkHealthService_detectsTailscaleInterfacesAndVpnProfiles() {
        val noPrivateNet = com.example.network.NetworkHealthService.detectPrivateNetwork(
            simulatedInterfaces = listOf("wlan0" to listOf("192.168.1.20")),
            simulatedVpnProfile = null
        )
        assertFalse(noPrivateNet.isDetected)

        val vpnProfileMatch = com.example.network.NetworkHealthService.detectPrivateNetwork(
            simulatedInterfaces = listOf("tun0" to listOf("10.8.0.2")),
            simulatedVpnProfile = "Tailscale Oasis Private VPN (sync.lewisfamilysystems.com)"
        )
        assertTrue("Active VPN profile for Tailscale/Oasis must unlock private network state", vpnProfileMatch.isDetected)
        assertTrue(vpnProfileMatch.vpnTransportActive)

        val tailnetIpMatch = com.example.network.NetworkHealthService.detectPrivateNetwork(
            simulatedInterfaces = listOf("tailscale0" to listOf("100.88.14.9")),
            simulatedVpnProfile = null
        )
        assertTrue(tailnetIpMatch.isDetected)
        assertEquals("100.88.14.9", tailnetIpMatch.tailnetIp)
    }

    @Test
    fun quoteAudioAndVoiceService_encodesValidWavHeaderAndDerives30sMoodProfile() {
        val samplePcm = ShortArray(1600) { idx -> (idx % 200).toShort() }
        val wavBytes = com.example.network.QuoteAudioAndVoiceService.encodePcmToWavBytes(
            pcmSamples = samplePcm,
            sampleRate = 16000
        )
        // 44-byte RIFF WAVE header + 3200 bytes PCM payload
        assertEquals(44 + 3200, wavBytes.size)
        assertEquals('R'.code.toByte(), wavBytes[0])
        assertEquals('I'.code.toByte(), wavBytes[1])
        assertEquals('F'.code.toByte(), wavBytes[2])
        assertEquals('F'.code.toByte(), wavBytes[3])

        val quote = SeedQuoteCatalog.allQuotes.first()
        val mood = com.example.network.QuoteAudioAndVoiceService.deriveMusicalMoodProfile(quote)
        assertTrue(mood.title.isNotBlank())
        assertTrue(mood.caption.contains(quote.author))
        assertTrue(mood.rootHz > 100.0)

        // Verify expressive literary TTS prompt and saved Quote conversion
        val savedQuote = com.example.data.Quote(
            id = "saved_test_1",
            userId = "u_1",
            text = quote.text,
            author = quote.author,
            tags = listOf("Humor", "Wit"),
            sourceWork = quote.sourceWork,
            category = "Humor",
            personalNote = "Makes me smile every morning"
        )
        val convertedItem = com.example.network.QuoteAudioAndVoiceService.toQuoteItem(savedQuote)
        assertEquals("saved_test_1", convertedItem.id)
        assertEquals("Humor", convertedItem.category)

        val ttsPrompt = com.example.network.QuoteAudioAndVoiceService.buildExpressiveLiteraryTtsPrompt(
            quote = convertedItem,
            personalNote = savedQuote.personalNote
        )
        assertTrue(ttsPrompt.contains("expressive, warm, literary tone"))
        assertTrue(ttsPrompt.contains(quote.author))
        assertTrue(ttsPrompt.contains("Makes me smile every morning"))

        val humorMood = com.example.network.QuoteAudioAndVoiceService.deriveMusicalMoodProfile(convertedItem)
        assertEquals("Salon Scherzo & Continuo", humorMood.title)
        val lyriaPrompt = com.example.network.QuoteAudioAndVoiceService.buildMusicScenePrompt(convertedItem)
        assertTrue(lyriaPrompt.contains("lyria-3-clip-preview"))
        assertTrue(lyriaPrompt.contains("30-second ambient instrumental soundscape"))
    }

    @Test
    fun wcagAaaHighContrastTheme_meetsStrict7To1ContrastStandards() {
        val light = com.example.ui.theme.HighContrastLightColorScheme
        val dark = com.example.ui.theme.HighContrastDarkColorScheme

        // Light High-Contrast WCAG AAA checks (>= 7.0:1)
        assertTrue(com.example.ui.theme.meetsWcagAaa(light.onBackground, light.background))
        assertTrue(com.example.ui.theme.meetsWcagAaa(light.onSurface, light.surface))
        assertTrue(com.example.ui.theme.meetsWcagAaa(light.onPrimary, light.primary))
        assertTrue(com.example.ui.theme.meetsWcagAaa(light.onSecondary, light.secondary))
        assertTrue(com.example.ui.theme.meetsWcagAaa(light.onTertiary, light.tertiary))
        assertTrue(com.example.ui.theme.meetsWcagAaa(light.outline, light.background))

        // Dark High-Contrast WCAG AAA checks (>= 7.0:1)
        assertTrue(com.example.ui.theme.meetsWcagAaa(dark.onBackground, dark.background))
        assertTrue(com.example.ui.theme.meetsWcagAaa(dark.onSurface, dark.surface))
        assertTrue(com.example.ui.theme.meetsWcagAaa(dark.onPrimary, dark.primary))
        assertTrue(com.example.ui.theme.meetsWcagAaa(dark.onSecondary, dark.secondary))
        assertTrue(com.example.ui.theme.meetsWcagAaa(dark.onTertiary, dark.tertiary))
        assertTrue(com.example.ui.theme.meetsWcagAaa(dark.outline, dark.background))
    }

    @Test
    fun z1440CurfewAndAbac32LatticeAndTotalOrderLww_satisfyFormalInvariants() {
        // 1. Euclidean Positive Modulo in Z_1440 for overnight curfews (22:00 = 1320 to 06:00 = 360)
        assertTrue(OasisCommonplaceServerClient.isWithinCurfewZ1440(minuteOfDay = 23 * 60, startMin = 22 * 60, endMin = 6 * 60))
        assertTrue(OasisCommonplaceServerClient.isWithinCurfewZ1440(minuteOfDay = 2 * 60, startMin = 22 * 60, endMin = 6 * 60))
        assertFalse(OasisCommonplaceServerClient.isWithinCurfewZ1440(minuteOfDay = 14 * 60, startMin = 22 * 60, endMin = 6 * 60))

        // 2. 32-State FIRST_APPLICABLE_DENY ABAC Lattice Evaluator
        val permitted = OasisCommonplaceServerClient.evaluateAbac32Lattice(
            explicitDeny = false,
            trustRing = 3,
            minRing = 2,
            vipBypass = false,
            curfewOk = true,
            stepUpOk = true
        )
        assertTrue(permitted.permit)
        assertEquals("01011", permitted.stateBits)

        val vipPermittedDuringCurfew = OasisCommonplaceServerClient.evaluateAbac32Lattice(
            explicitDeny = false,
            trustRing = 4,
            minRing = 2,
            vipBypass = true,
            curfewOk = false,
            stepUpOk = false
        )
        assertTrue(vipPermittedDuringCurfew.permit)
        assertEquals("01100", vipPermittedDuringCurfew.stateBits)

        val explicitDenied = OasisCommonplaceServerClient.evaluateAbac32Lattice(
            explicitDeny = true,
            trustRing = 4,
            minRing = 1,
            vipBypass = true,
            curfewOk = true,
            stepUpOk = true
        )
        assertFalse(explicitDenied.permit)
        assertEquals("11111", explicitDenied.stateBits)

        // 3. Total Lexical Order (ts, replicaId, val) LWW Resolver
        val cellA = TotalOrderLwwCell(ts = 1700000000L, replicaId = "nStable420A", value = "alpha")
        val cellB = TotalOrderLwwCell(ts = 1700000000L, replicaId = "nStable420C", value = "beta")
        assertEquals(cellB, OasisCommonplaceServerClient.resolveTotalOrderLww(cellA, cellB))
    }

    @Test
    fun aegisEnvelopeV1AndTrashArcadeBridge_sealSixGateAndEmitCrossWingDirectives() {
        val statePayload = "{\"wing\":\"WING_I_COMMONPLACE\",\"gold\":420,\"rep\":128}"
        val sealedResult = CommonplaceCryptoEnvelope.sealAegisEnvelopeV1(
            slotKey = "slot_01",
            capsuleTarget = "lfs-trash-arcade",
            cartridgeSlug = "commonplace-codex",
            stableNodeId = "nStable420c4d",
            subuserId = "sub_archivist_01",
            generationEpoch = 14L,
            abacStateBits = "01111",
            replicateToAnchor = true,
            stateJson = statePayload,
            passphrase = "oasis-aegis-TNoZe9MmWS11CNTRL"
        )
        assertTrue(sealedResult.isSuccess)
        val aegisWrapper = sealedResult.getOrThrow()
        assertTrue(aegisWrapper.has("aegisEnvelopeV1"))

        val unsealedResult = CommonplaceCryptoEnvelope.verifyAndUnsealAegisEnvelopeV1(
            envelopeWrapper = aegisWrapper,
            passphraseOverride = "oasis-aegis-TNoZe9MmWS11CNTRL"
        )
        assertTrue(unsealedResult.isSuccess)
        assertEquals(statePayload, unsealedResult.getOrThrow())

        // Verify TrashArcade Rupee denominations (1..1000) & Cross-Wing Quest Directive
        val defaultEnvelope = com.example.data.CommonplaceTrilogySyncEnvelopeV1()
        assertEquals(9, defaultEnvelope.rupeeDenominations.size)
        assertEquals(1, defaultEnvelope.rupeeDenominations.first().value)
        assertEquals(1000, defaultEnvelope.rupeeDenominations.last().value)
        assertEquals(4, defaultEnvelope.unlockedRelics.size)
        assertEquals(20, defaultEnvelope.saveSlots.size)

        val quote = SeedQuoteCatalog.allQuotes.first()
        val directive = NativeArcadeAssetBridge.buildDirectiveFromQuote(
            quote = quote,
            sourceEvent = "UNIT_TEST",
            manualDoctrineOverride = PhilosophicalDoctrine.ARCANE,
            replicaId = "nStable420c4d"
        )
        assertEquals("arcane", directive.philosophicalDoctrine)
        assertTrue(directive.goldReward > 0)
        assertTrue(directive.rupeeReward > 0)
        assertEquals("http://100.106.12.104:13379/lfs-sso/handshake", LfsServerPreset.TAILSCALE_ANCHOR_PROD.handshakeUrl)
    }

    @Test
    fun disneyImagineeringTranslator_oasisRibbon_andSanctuaryStoryteller_satisfyAllAgesInvariants() {
        // 1. Disney Imagineering Engagement Translator (Grade <= 8.0) vocabulary
        assertEquals(
            "Hearth Key (Instant Family Pass)",
            com.example.data.DisneyImagineeringTranslator.HEARTH_KEY_LABEL
        )
        assertEquals(
            "Time-Machine Memory Vaults",
            com.example.data.DisneyImagineeringTranslator.TIME_MACHINE_VAULTS_LABEL
        )
        assertEquals(
            "Host From My Desk",
            com.example.data.LfsHostMode.HOST_LOCAL_DEVICE.label
        )
        assertEquals(
            "Open a Family Cloud Room",
            com.example.data.LfsHostMode.LAUNCH_U2_HOSTING_SERVICE.label
        )
        assertEquals(
            "Starlight Quiet Hours (Rest & Recharge Time)",
            com.example.data.DisneyImagineeringTranslator.STARLIGHT_QUIET_HOURS_LABEL
        )
        assertEquals(
            "Starlight Rupees & Wonder Relics",
            com.example.data.DisneyImagineeringTranslator.STARLIGHT_RUPEES_RELICS_LABEL
        )

        // 2. 4-Part Friendly Guide Cards replacing ZT-6 technical error banners
        val guide = com.example.data.DisneyImagineeringTranslator.toFriendlyGuideCard(
            rawNoticeOrError = "ZT-6 network offline fallback for aegisEnvelopeV1",
            preferredActionLabel = "Keep Exploring Together"
        )
        assertEquals("Friendly Guide Cards", guide.title)
        assertTrue(guide.whatHappened.isNotBlank())
        assertTrue(guide.whyItHappened.isNotBlank())
        assertTrue(guide.yourProgressIsSafe.contains("Time-Machine Memory Vault"))
        assertEquals("Keep Exploring Together", guide.oneClickNextStepLabel)

        // 3. Default zt9_comfort_overrides Family-Friendly Arcade Mode
        val envelope = com.example.data.CommonplaceTrilogySyncEnvelopeV1()
        assertTrue(envelope.zt9ComfortOverrides.familyFriendlyArcadeMode)
        assertEquals("zt9_comfort_overrides", envelope.zt9ComfortOverrides.schemaKey)
        assertEquals("Cardboard Critters / Neon Arcade Bots", envelope.zt9ComfortOverrides.enemyFramingLabel)
        assertEquals("Confetti KOs", envelope.zt9ComfortOverrides.eliminationFramingLabel)
        assertEquals("Tag-Team Rescue", envelope.zt9ComfortOverrides.coOpReviveFramingLabel)

        // 4. 7-Destination Switch Wing / Return to TrashArcade Hall selector & 16-Channel Rmblyn Chat
        val hallLabels = com.example.data.OasisHallDestination.entries.map { it.displayLabel }
        assertEquals(
            listOf(
                "Commonplace Wing I",
                "Scriptorium Wing II",
                "Chapterhouse Wing III",
                "3D Vigil Wing IV",
                "Code Runner",
                "Boxhead Fort",
                "BoxBoy Defense"
            ),
            hallLabels
        )
        assertEquals(16, com.example.data.CommonplaceTrilogySyncEnvelopeV1.RMBLYN_16_CHANNELS.size)
        val seededHandles = envelope.rmblynMessages.map { it.senderHandle }.toSet()
        assertTrue(seededHandles.containsAll(listOf("@willow", "@cinder", "@mote")))

        // 5. AudioBookshelf & OpenAudible Sanctuary Storyteller (cb64f9e7)
        val storyteller = envelope.storyteller
        assertEquals("cb64f9e7", storyteller.serviceId)
        assertEquals("/api/lfs/openaudible/library", storyteller.libraryEndpoint)
        assertEquals("/api/lfs/openaudible/stream/[key]", storyteller.streamEndpointTemplate)
        assertEquals(listOf(15, 30, 45), com.example.data.SanctuaryStorytellerState.SLEEP_TIMER_PRESETS_MINUTES)
        val client = OasisCommonplaceServerClient()
        assertEquals(
            "https://lfs-prod.tail420c4d.ts.net/api/lfs/openaudible/stream/ch_01_lantern_porch",
            client.buildOpenAudibleStreamUrl(
                baseUrl = LfsServerPreset.TAILSCALE_ANCHOR_PROD.baseUrl,
                chapterKey = "ch_01_lantern_porch"
            )
        )
    }

    @Test
    fun computeIdf_isNonNegative_withOkapiBm25() {
        val idfNormal = com.example.data.QuoteSearchEngine.computeIdf(10, 1)
        assertTrue(idfNormal > 0.0)

        val idfAll = com.example.data.QuoteSearchEngine.computeIdf(10, 10)
        assertTrue(idfAll >= 0.0)

        val idfMost = com.example.data.QuoteSearchEngine.computeIdf(10, 9)
        assertTrue(idfMost >= 0.0)
    }

    @Test
    fun rankBm25_scoresQuotesCorrectly() {
        val quotes = listOf(
            com.example.data.QuoteItem("1", "To be or not to be", "Shakespeare", "Hamlet", "Literature", emptyList(), 100, ""),
            com.example.data.QuoteItem("2", "I think therefore I am", "Descartes", "Philosophy", "Philosophy", emptyList(), 100, ""),
            com.example.data.QuoteItem("3", "To think is to exist", "Unknown", "Unknown", "Philosophy", emptyList(), 100, "")
        )

        val results = com.example.data.QuoteSearchEngine.rankBm25("think", quotes)
        val thinkQuotes = results.filter { it.second > 0.0 }
        assertEquals(2, thinkQuotes.size)
        assertTrue(thinkQuotes.any { it.first.id == "2" })
        assertTrue(thinkQuotes.any { it.first.id == "3" })
        assertTrue(results[0].second >= results[1].second)
    }

    @Test
    fun rankRrf_fusesRankingsCorrectly() {
        val q1 = com.example.data.QuoteItem("1", "A", "A", "A", "A", emptyList(), 100, "")
        val q2 = com.example.data.QuoteItem("2", "B", "B", "B", "B", emptyList(), 100, "")
        val q3 = com.example.data.QuoteItem("3", "C", "C", "C", "C", emptyList(), 100, "")

        val ranking1 = listOf(q1, q2, q3)
        val ranking2 = listOf(q3, q1, q2)

        val results = com.example.data.QuoteSearchEngine.rankRrf(listOf(ranking1, ranking2), k = 60)

        assertEquals(3, results.size)
        val scoreQ1 = 1.0/61.0 + 1.0/62.0
        val scoreQ3 = 1.0/63.0 + 1.0/61.0
        val scoreQ2 = 1.0/62.0 + 1.0/63.0

        assertEquals(q1, results[0].first)
        assertEquals(q3, results[1].first)
        assertEquals(q2, results[2].first)

        assertTrue(kotlin.math.abs(results[0].second - scoreQ1) < 0.0001)
        assertTrue(kotlin.math.abs(results[1].second - scoreQ3) < 0.0001)
        assertTrue(kotlin.math.abs(results[2].second - scoreQ2) < 0.0001)
    }
}
