package com.example

import com.example.data.CommonplaceTransportMode
import com.example.data.LfsHostMode
import com.example.data.LfsLifecyclePolicy
import com.example.data.LfsServerEndpointConfig
import com.example.data.MultiplayerGameMode
import com.example.data.SeedQuoteCatalog
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
}



