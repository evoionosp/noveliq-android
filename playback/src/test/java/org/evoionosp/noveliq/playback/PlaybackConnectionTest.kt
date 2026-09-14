package org.evoionosp.noveliq.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookDetail
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookTrack
import org.evoionosp.noveliq.domain.audiobook.model.PlaybackProgress
import org.evoionosp.noveliq.domain.audiobook.playback.PlaybackPositionCalculator
import org.evoionosp.noveliq.domain.audiobook.usecase.FetchPlaybackProgressUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.PreparePlaybackUseCase
import org.evoionosp.noveliq.domain.audiobook.usecase.SavePlaybackProgressUseCase
import org.evoionosp.noveliq.domain.library.model.DomainResult
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.usecase.GetValidSessionUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackConnectionTest {
    @Test
    fun `publishes the initial controller state on connect`() =
        runTest {
            val graph = graph()
            runCurrent()

            graph.connection.playbackState.test {
                val state = awaitItem()
                assertFalse(state.isPlaying)
                assertEquals(300_000L, state.durationMs)
                assertEquals(0L, state.currentPositionMs)
                assertEquals(1.0f, state.playbackSpeed)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `play delegates to the controller`() =
        runTest {
            val graph = graph()

            graph.connection.play()

            verify { graph.controller.play() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `pause delegates and saves progress after a book is loaded`() =
        runTest {
            val graph = graph()
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null }

            graph.connection.pause()
            runCurrent()

            verify { graph.controller.pause() }
            coVerify { graph.saveProgress("https://example.com", "access-1", "book-1", 0.0, 300.0) }
        }

    @Test
    fun `playAudiobook does nothing without a session`() =
        runTest {
            val graph = graph()
            coEvery { graph.getValidSession() } returns null

            graph.connection.playAudiobook(testAudiobook())
            runCurrent()

            verify(exactly = 0) { graph.controller.setMediaItems(any(), any(), any()) }
        }

    @Test
    fun `playAudiobook does nothing when no playable detail exists`() =
        runTest {
            val graph = graph()
            coEvery { graph.prepare(any(), any(), any(), any()) } returns null

            graph.connection.playAudiobook(testAudiobook())
            runCurrent()

            verify(exactly = 0) { graph.controller.setMediaItems(any(), any(), any()) }
        }

    @Test
    fun `playAudiobook loads tracks and resumes from server progress`() =
        runTest {
            val graph = graph()
            coEvery { graph.fetchProgress(any(), any(), any()) } returns
                PlaybackProgress(currentTimeSeconds = 120.0, durationSeconds = 300.0, isFinished = false)

            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null }

            val items = slot<List<MediaItem>>()
            val index = slot<Int>()
            val offset = slot<Long>()
            verify { graph.controller.setMediaItems(capture(items), capture(index), capture(offset)) }
            assertEquals(listOf("0", "1", "2"), items.captured.map { it.mediaId })
            assertEquals(1, index.captured)
            assertEquals(20_000L, offset.captured)
            verify { graph.controller.prepare() }
            verify { graph.controller.play() }
            assertEquals(
                "The Book",
                items.captured[1]
                    .mediaMetadata.title
                    ?.toString(),
            )
            assertEquals(
                "Chapter Two",
                items.captured[1]
                    .mediaMetadata.artist
                    ?.toString(),
            )
        }

    @Test
    fun `explicit start position wins over saved progress and saves immediately`() =
        runTest {
            val graph = graph()
            coEvery { graph.fetchProgress(any(), any(), any()) } returns
                PlaybackProgress(currentTimeSeconds = 120.0, durationSeconds = 300.0, isFinished = false)

            graph.connection.playAudiobook(testAudiobook(), startPositionSeconds = 250.0)
            graph.connection.playbackState.first { it.audiobook != null }

            verify { graph.controller.setMediaItems(any(), 2, 50_000L) }
            // The immediate save reads the controller position (still at the stubbed start).
            coVerify { graph.saveProgress("https://example.com", "access-1", "book-1", 0.0, 300.0) }
        }

    @Test
    fun `artwork uri is attached when the book has a cover`() =
        runTest {
            mockkStatic(Uri::class)
            val artwork = mockk<Uri>()
            every { Uri.parse(any()) } returns artwork
            try {
                val graph = graph()

                graph.connection.playAudiobook(testAudiobook().copy(coverUrl = "https://example.com/cover.jpg"))
                graph.connection.playbackState.first { it.audiobook != null }

                val items = slot<List<MediaItem>>()
                verify { graph.controller.setMediaItems(capture(items), any(), any()) }
                assertEquals(artwork, items.captured[0].mediaMetadata.artworkUri)
            } finally {
                unmockkAll()
            }
        }

    @Test
    fun `seekToBookSeconds resolves through tracks and saves`() =
        runTest {
            val graph = graph()
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null }

            graph.connection.seekToBookSeconds(250.0)
            runCurrent()

            verify { graph.controller.seekTo(2, 50_000L) }
            coVerify { graph.saveProgress("https://example.com", "access-1", "book-1", 0.0, 300.0) }
        }

    @Test
    fun `seekToBookSeconds without tracks seeks raw milliseconds`() =
        runTest {
            val graph = graph()

            graph.connection.seekToBookSeconds(12.5)

            verify { graph.controller.seekTo(12_500L) }
            coVerify(exactly = 0) { graph.saveProgress(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `chapter skips resolve through the domain calculator`() =
        runTest {
            val graph = graph()
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null }
            every { graph.controller.currentMediaItemIndex } returns 1
            every { graph.controller.currentPosition } returns 50_000L

            // 150s is 50s into Chapter Two (> 20s): next goes to Chapter Three…
            graph.connection.skipToNextChapter(testChapters())
            verify { graph.controller.seekTo(2, 0L) }

            // …while previous restarts Chapter Two.
            graph.connection.skipToPreviousChapter(testChapters())
            verify { graph.controller.seekTo(1, 0L) }
        }

    @Test
    fun `next chapter past the end does nothing`() =
        runTest {
            val graph = graph()
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null }
            every { graph.controller.currentMediaItemIndex } returns 2
            every { graph.controller.currentPosition } returns 90_000L

            graph.connection.skipToNextChapter(testChapters())

            verify(exactly = 0) { graph.controller.seekTo(any(), any()) }
        }

    @Test
    fun `seek forward and backward apply the default steps with a zero clamp`() =
        runTest {
            val graph = graph()
            every { graph.controller.currentPosition } returns 50_000L

            graph.connection.seekForward()
            verify { graph.controller.seekTo(80_000L) }

            graph.connection.seekBackward()
            verify { graph.controller.seekTo(35_000L) }

            every { graph.controller.currentPosition } returns 5_000L
            graph.connection.seekBackward()
            verify { graph.controller.seekTo(0L) }
        }

    @Test
    fun `setPlaybackSpeed updates the controller and the state`() =
        runTest {
            val graph = graph()

            graph.connection.setPlaybackSpeed(1.5f)

            verify { graph.controller.setPlaybackSpeed(1.5f) }
            graph.connection.playbackState.test {
                assertEquals(1.5f, awaitItem().playbackSpeed)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `progress loop saves every fifteen seconds while playing`() =
        runTest {
            val graph = graph()
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null }
            every { graph.controller.isPlaying } returns true

            advanceTimeBy(15_000)
            runCurrent()
            coVerify(exactly = 1) { graph.saveProgress(any(), any(), any(), 0.0, 300.0) }

            advanceTimeBy(15_000)
            runCurrent()
            coVerify(exactly = 2) { graph.saveProgress(any(), any(), any(), 0.0, 300.0) }
        }

    @Test
    fun `progress loop stays silent while paused`() =
        runTest {
            val graph = graph()
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null }

            advanceTimeBy(60_000)

            coVerify(exactly = 0) { graph.saveProgress(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `save is skipped when the controller reports no current item`() =
        runTest {
            val graph = graph()
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null }
            every { graph.controller.currentMediaItemIndex } returns C.INDEX_UNSET

            graph.connection.pause()

            coVerify(exactly = 0) { graph.saveProgress(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `save is skipped when the session went blank mid-playback`() =
        runTest {
            val graph = graph()
            coEvery { graph.getValidSession() } returns testSession() andThen testSession().copy(accessToken = "")
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null }

            graph.connection.pause()

            coVerify(exactly = 0) { graph.saveProgress(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `save is skipped when the session is gone mid-playback`() =
        runTest {
            val graph = graph()
            coEvery { graph.getValidSession() } returns testSession() andThen null
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null }

            graph.connection.pause()

            coVerify(exactly = 0) { graph.saveProgress(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `disconnected controller degrades gracefully`() =
        runTest {
            val graph = graph(connected = false)

            graph.connection.play()
            graph.connection.pause()
            graph.connection.seekToBookSeconds(10.0)
            graph.connection.setPlaybackSpeed(2.0f)
            graph.connection.playAudiobook(testAudiobook())
            runCurrent()

            coVerify(exactly = 0) { graph.saveProgress(any(), any(), any(), any(), any()) }
            graph.connection.playbackState.test {
                assertNull(awaitItem().audiobook)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `player listener playing changes update the state`() =
        runTest {
            val graph = graph()

            graph.connection.playbackState.test {
                assertFalse(awaitItem().isPlaying)
                graph.connection.PlayerListener().onIsPlayingChanged(true)
                assertTrue(awaitItem().isPlaying)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `progress updates reflect the controller position`() =
        runTest {
            val graph = graph()
            graph.connection.playAudiobook(testAudiobook())
            graph.connection.playbackState.first { it.audiobook != null }
            every { graph.controller.currentMediaItemIndex } returns 1
            every { graph.controller.currentPosition } returns 50_000L

            graph.connection.playbackState.test {
                awaitItem()
                advanceTimeBy(1_000)
                val state = awaitItem()
                assertEquals(50_000L, state.currentPositionMs)
                assertEquals(150.0, state.currentBookPositionSeconds, 0.001)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private data class Graph(
        val connection: PlaybackConnection,
        val controller: Player,
        val getValidSession: GetValidSessionUseCase,
        val prepare: PreparePlaybackUseCase,
        val fetchProgress: FetchPlaybackProgressUseCase,
        val saveProgress: SavePlaybackProgressUseCase,
    )

    private fun TestScope.graph(connected: Boolean = true): Graph {
        val controller = mockk<Player>()
        every { controller.addListener(any()) } just Runs
        every { controller.isPlaying } returns false
        every { controller.duration } returns 300_000L
        every { controller.currentPosition } returns 0L
        every { controller.currentMediaItemIndex } returns 0
        every { controller.playbackParameters } returns PlaybackParameters(1.0f)
        every { controller.play() } just Runs
        every { controller.pause() } just Runs
        every { controller.prepare() } just Runs
        every { controller.seekTo(any<Long>()) } just Runs
        every { controller.seekTo(any<Int>(), any<Long>()) } just Runs
        every { controller.setMediaItems(any<List<MediaItem>>(), any<Int>(), any<Long>()) } just Runs
        every { controller.setPlaybackSpeed(any()) } just Runs

        val getValidSession = mockk<GetValidSessionUseCase>()
        coEvery { getValidSession() } returns testSession()
        val prepare = mockk<PreparePlaybackUseCase>()
        coEvery { prepare(any(), any(), any(), any()) } returns testDetail()
        val fetchProgress = mockk<FetchPlaybackProgressUseCase>()
        coEvery { fetchProgress(any(), any(), any()) } returns null
        val saveProgress = mockk<SavePlaybackProgressUseCase>()
        coEvery { saveProgress(any(), any(), any(), any(), any()) } returns DomainResult.Success(Unit)

        // Deterministic scope: connection work (including the infinite progress loop)
        // runs on the shared test scheduler and dies with the test. Standard (not
        // Unconfined) delivery means nothing runs until pumped, so settle the init
        // launches (connect + first loop tick) here: every test then starts from a
        // connected, initialized connection.
        val scope = CoroutineScope(backgroundScope.coroutineContext + StandardTestDispatcher(testScheduler))
        val connection =
            PlaybackConnection(
                getValidSessionUseCase = getValidSession,
                calculator = PlaybackPositionCalculator(),
                preparePlayback = prepare,
                fetchPlaybackProgress = fetchProgress,
                savePlaybackProgress = saveProgress,
                controllerConnector = FakeConnector(if (connected) controller else null),
                connectionScope = scope,
            )
        runCurrent()
        return Graph(connection, controller, getValidSession, prepare, fetchProgress, saveProgress)
    }

    private class FakeConnector(
        private val controller: Player?,
    ) : MediaControllerConnector {
        override suspend fun connect(): Player? = controller
    }

    private fun testSession() =
        LoginSession(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            userId = "user-1",
            username = "demo",
            baseUrl = "https://example.com",
        )

    private fun testAudiobook() =
        Audiobook(
            id = "book-1",
            libraryId = "lib-1",
            title = "The Book",
            author = "Author",
            coverUrl = "",
            series = null,
            durationInSeconds = 300,
        )

    private fun testTrack(
        index: Int,
        start: Long,
    ) = AudiobookTrack(
        index = index,
        startOffsetInSeconds = start,
        durationInSeconds = 100,
        title = "Track $index",
        remoteUrl = "https://example.com/$index.mp3",
        mimeType = "audio/mpeg",
    )

    private fun testChapters() =
        listOf(
            AudiobookChapter(title = "Chapter One", startInSeconds = 0, endInSeconds = 100),
            AudiobookChapter(title = "Chapter Two", startInSeconds = 100, endInSeconds = 200),
            AudiobookChapter(title = "Chapter Three", startInSeconds = 200, endInSeconds = null),
        )

    private fun testDetail() =
        AudiobookDetail(
            audiobook = testAudiobook(),
            description = null,
            chapters = testChapters(),
            tracks = listOf(testTrack(0, 0), testTrack(1, 100), testTrack(2, 200)),
            refreshedAtMillis = 0L,
        )
}
