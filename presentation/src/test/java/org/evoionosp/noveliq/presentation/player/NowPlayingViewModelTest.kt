package org.evoionosp.noveliq.presentation.player

import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookDetail
import org.evoionosp.noveliq.domain.audiobook.usecase.ObserveAudiobookDetailUseCase
import org.evoionosp.noveliq.domain.session.usecase.GetValidSessionUseCase
import org.evoionosp.noveliq.domain.settings.AppSettings
import org.evoionosp.noveliq.domain.settings.AppSettingsStore
import org.evoionosp.noveliq.playback.PlaybackConnection
import org.evoionosp.noveliq.playback.PlaybackState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NowPlayingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `chapter target prefers the current chapter end`() {
        val chapters =
            listOf(
                AudiobookChapter(title = "A", startInSeconds = 0, endInSeconds = 100),
                AudiobookChapter(title = "B", startInSeconds = 100, endInSeconds = 200),
                AudiobookChapter(title = "C", startInSeconds = 200, endInSeconds = null),
            )

        assertEquals(100.0, sleepChapterTargetSeconds(chapters, 50.0, 300.0))
        assertEquals(200.0, sleepChapterTargetSeconds(chapters, 150.0, 300.0))
    }

    @Test
    fun `chapter target falls back to next start then book end`() {
        val chapters =
            listOf(
                AudiobookChapter(title = "A", startInSeconds = 0, endInSeconds = null),
                AudiobookChapter(title = "B", startInSeconds = 100, endInSeconds = null),
            )

        assertEquals(100.0, sleepChapterTargetSeconds(chapters, 50.0, 300.0))
        assertEquals(300.0, sleepChapterTargetSeconds(chapters, 150.0, 300.0))
        assertEquals(300.0, sleepChapterTargetSeconds(emptyList(), 10.0, 300.0))
    }

    @Test
    fun `chapter target skips stale ends and nulls past the end`() {
        val chapters =
            listOf(
                AudiobookChapter(title = "A", startInSeconds = 0, endInSeconds = 40),
            )

        assertEquals(300.0, sleepChapterTargetSeconds(chapters, 50.0, 300.0))
        assertNull(sleepChapterTargetSeconds(chapters, 300.0, 300.0))
        assertNull(sleepChapterTargetSeconds(emptyList(), 10.0, 0.0))
    }

    @Test
    fun `arm and cancel delegate to the connection`() =
        runTest {
            val connection = mockk<PlaybackConnection>()
            every { connection.playbackState } returns MutableStateFlow(PlaybackState())
            every { connection.startSleepTimer(any()) } just Runs
            every { connection.cancelSleepTimer() } just Runs
            val viewModel = viewModel(connection)

            viewModel.armSleepTimer(15)
            viewModel.cancelSleepTimer()

            verify { connection.startSleepTimer(15) }
            verify { connection.cancelSleepTimer() }
        }

    @Test
    fun `arm end of chapter resolves the live chapter end`() =
        runTest {
            val book =
                Audiobook(
                    id = "book-1",
                    libraryId = "lib-1",
                    title = "The Book",
                    author = "Author",
                    coverUrl = "",
                    series = null,
                    durationInSeconds = 300,
                )
            val chapters =
                listOf(
                    AudiobookChapter(title = "A", startInSeconds = 0, endInSeconds = 100),
                    AudiobookChapter(title = "B", startInSeconds = 100, endInSeconds = 200),
                )
            val playback = MutableStateFlow(PlaybackState(audiobook = book, currentBookPositionSeconds = 50.0))
            val connection = mockk<PlaybackConnection>()
            every { connection.playbackState } returns playback
            every { connection.startSleepAtChapterEnd(any()) } just Runs
            val observe = mockk<ObserveAudiobookDetailUseCase>()
            every { observe(any(), any()) } returns
                flowOf(
                    AudiobookDetail(
                        audiobook = book,
                        description = null,
                        chapters = chapters,
                        tracks = emptyList(),
                        refreshedAtMillis = 0L,
                    ),
                )
            val viewModel = viewModel(connection, observe)
            advanceUntilIdle()

            viewModel.armSleepEndOfChapter()

            verify { connection.startSleepAtChapterEnd(100.0) }
        }

    @Test
    fun `sleep duration exposes the persisted pick`() =
        runTest {
            val connection = mockk<PlaybackConnection>()
            every { connection.playbackState } returns MutableStateFlow(PlaybackState())
            val viewModel =
                viewModel(
                    connection,
                    appSettingsStore = FakeAppSettingsStore(AppSettings(sleepTimerMinutes = 45)),
                )

            viewModel.sleepTimerMinutes.test {
                // stateIn replays its initial value before the stored settings arrive.
                assertEquals(AppSettings.DEFAULT_SLEEP_TIMER_MINUTES, awaitItem())
                assertEquals(45, awaitItem())
            }
        }

    @Test
    fun `saving sleep duration persists without arming`() =
        runTest {
            val connection = mockk<PlaybackConnection>()
            every { connection.playbackState } returns MutableStateFlow(PlaybackState())
            val store = FakeAppSettingsStore()
            val viewModel = viewModel(connection, appSettingsStore = store)

            viewModel.saveSleepTimerMinutes(60)
            advanceUntilIdle()

            assertEquals(60, store.settings.first().sleepTimerMinutes)
            verify(exactly = 0) { connection.startSleepTimer(any()) }
        }

    @Test
    fun `arm end of chapter is a no-op without a boundary`() =
        runTest {
            val book =
                Audiobook(
                    id = "book-1",
                    libraryId = "lib-1",
                    title = "The Book",
                    author = "Author",
                    coverUrl = "",
                    series = null,
                    durationInSeconds = null,
                )
            val playback = MutableStateFlow(PlaybackState(audiobook = book, currentBookPositionSeconds = 50.0))
            val connection = mockk<PlaybackConnection>()
            every { connection.playbackState } returns playback
            val observe = mockk<ObserveAudiobookDetailUseCase>()
            every { observe(any(), any()) } returns
                flowOf(
                    AudiobookDetail(
                        audiobook = book,
                        description = null,
                        chapters = emptyList(),
                        tracks = emptyList(),
                        refreshedAtMillis = 0L,
                    ),
                )
            val viewModel = viewModel(connection, observe)
            advanceUntilIdle()

            viewModel.armSleepEndOfChapter()

            verify(exactly = 0) { connection.startSleepAtChapterEnd(any()) }
        }

    private fun viewModel(
        connection: PlaybackConnection,
        observe: ObserveAudiobookDetailUseCase = mockk(),
        appSettingsStore: AppSettingsStore = FakeAppSettingsStore(),
    ): NowPlayingViewModel {
        val getValidSession = mockk<GetValidSessionUseCase>()
        coEvery { getValidSession() } returns null
        return NowPlayingViewModel(
            playbackConnection = connection,
            observeAudiobookDetail = observe,
            refreshAudiobookDetail = mockk(),
            getValidSessionUseCase = getValidSession,
            appSettingsStore = appSettingsStore,
        )
    }

    private class FakeAppSettingsStore(
        initial: AppSettings = AppSettings(),
    ) : AppSettingsStore {
        private val state = MutableStateFlow(initial)

        override val settings: Flow<AppSettings> = state.asStateFlow()

        override suspend fun setThemePreference(themePreference: String) {
            state.update { it.copy(themePreference = themePreference) }
        }

        override suspend fun setDynamicColor(enabled: Boolean) {
            state.update { it.copy(useDynamicColor = enabled) }
        }

        override suspend fun setCoverTheme(enabled: Boolean) {
            state.update { it.copy(useCoverTheme = enabled) }
        }

        override suspend fun setSleepTimerMinutes(minutes: Int) {
            state.update { it.copy(sleepTimerMinutes = minutes) }
        }
    }
}
