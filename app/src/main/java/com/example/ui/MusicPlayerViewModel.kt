package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MusicRepository
import com.example.data.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicPlayerViewModel(
    application: Application,
    private val repository: MusicRepository
) : AndroidViewModel(application) {

    // Tracks and Favorites state mapping
    val allTracks: StateFlow<List<Track>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<Track>> = repository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Player controls state
    val currentTrack = MutableStateFlow<Track?>(null)
    val isPlaying = MutableStateFlow(false)
    val playbackProgressMs = MutableStateFlow(0L)
    val isShuffleEnabled = MutableStateFlow(false)
    val isRepeatEnabled = MutableStateFlow(false)

    // Filter, Search and Folders State
    val searchQuery = MutableStateFlow("")
    val currentFolder = MutableStateFlow<String?>("الكل") // "الكل", "المفضلة" or specific folder name

    // Smart Sleep Timer State
    val sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val isSleepTimerActive = MutableStateFlow(false)

    // Dynamic calculated playlist based on search, selection and hidden items
    val filteredTracks: StateFlow<List<Track>> = combine(
        allTracks,
        searchQuery,
        currentFolder,
        favoriteTracks
    ) { tracks, query, folder, favorites ->
        var list = when (folder) {
            "الكل" -> tracks
            "المفضلة" -> favorites
            null -> tracks
            else -> tracks.filter { it.folder == folder }
        }

        // Hide files marked with isHidden (Unless inspecting hidden files)
        list = list.filter { !it.isHidden }

        if (query.isNotEmpty()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.artist.contains(query, ignoreCase = true) ||
                        it.album.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated unique folders list
    val availableFolders: StateFlow<List<String>> = allTracks.combine(favoriteTracks) { tracks, favorites ->
        val list = mutableListOf<String>("الكل")
        if (favorites.isNotEmpty()) {
            list.add("المفضلة")
        }
        val customFolders = tracks.map { it.folder }.distinct().filter { it.isNotEmpty() && it != "المفضلة" }
        list.addAll(customFolders)
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("الكل"))

    // Hidden count
    val hiddenTracksCount: StateFlow<Int> = allTracks.combine(MutableStateFlow(true)) { tracks, _ ->
        tracks.count { it.isHidden }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Coroutine Jobs for custom ticks
    private var playbackJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        // Run database seeding if empty
        viewModelScope.launch {
            repository.checkForInitialSeeding()
        }
        // Run simulator tick
        startPlaybackProgressTicker()
    }

    private fun startPlaybackProgressTicker() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (true) {
                delay(100)
                if (isPlaying.value) {
                    val track = currentTrack.value
                    if (track != null) {
                        val currentProg = playbackProgressMs.value
                        if (currentProg >= track.durationMs) {
                            // Completed current song
                            handleTrackCompletion()
                        } else {
                            playbackProgressMs.value = currentProg + 100
                        }
                    }
                }
            }
        }
    }

    private fun handleTrackCompletion() {
        if (isRepeatEnabled.value) {
            playbackProgressMs.value = 0
        } else {
            onSkipNext()
        }
    }

    fun playTrack(track: Track) {
        currentTrack.value = track
        playbackProgressMs.value = 0
        isPlaying.value = true
    }

    fun togglePlayPause() {
        // If no track is currently selected, pick the first one from our current playlist
        if (currentTrack.value == null) {
            val list = filteredTracks.value
            if (list.isNotEmpty()) {
                currentTrack.value = list.first()
            }
        }
        isPlaying.value = !isPlaying.value
    }

    fun onSkipNext() {
        val list = filteredTracks.value
        if (list.isEmpty()) return

        val current = currentTrack.value
        val nextIndex = if (isShuffleEnabled.value) {
            list.indices.random()
        } else {
            if (current == null) 0
            else {
                val index = list.indexOfFirst { it.id == current.id }
                if (index == -1 || index == list.lastIndex) 0 else index + 1
            }
        }
        playTrack(list[nextIndex])
    }

    fun onSkipPrevious() {
        val list = filteredTracks.value
        if (list.isEmpty()) return

        val current = currentTrack.value
        val prevIndex = if (isShuffleEnabled.value) {
            list.indices.random()
        } else {
            if (current == null) 0
            else {
                val index = list.indexOfFirst { it.id == current.id }
                if (index <= 0) list.lastIndex else index - 1
            }
        }
        playTrack(list[prevIndex])
    }

    fun seekTo(progressMs: Long) {
        val duration = currentTrack.value?.durationMs ?: 0L
        playbackProgressMs.value = progressMs.coerceIn(0L, duration)
    }

    fun forward10Seconds() {
        val currentPr = playbackProgressMs.value
        val duration = currentTrack.value?.durationMs ?: 0L
        seekTo(currentPr + 10000L)
    }

    fun rewind10Seconds() {
        val currentPr = playbackProgressMs.value
        seekTo(currentPr - 10000L)
    }

    fun toggleShuffle() {
        isShuffleEnabled.value = !isShuffleEnabled.value
    }

    fun toggleRepeat() {
        isRepeatEnabled.value = !isRepeatEnabled.value
    }

    fun toggleTrackFavorite(track: Track) {
        viewModelScope.launch {
            repository.toggleFavorite(track)
            // Update current track state if it is the toggled one
            if (currentTrack.value?.id == track.id) {
                currentTrack.value = currentTrack.value?.copy(isFavorite = !track.isFavorite)
            }
        }
    }

    fun hideTrack(track: Track) {
        viewModelScope.launch {
            repository.toggleHidden(track)
            if (currentTrack.value?.id == track.id) {
                onSkipNext()
            }
        }
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            repository.deleteTrack(track)
            if (currentTrack.value?.id == track.id) {
                isPlaying.value = false
                currentTrack.value = null
                playbackProgressMs.value = 0
            }
        }
    }

    fun scanFolders() {
        viewModelScope.launch {
            repository.scanLocalTracks()
        }
    }

    // Smart Sleep Timer functions
    fun setSleepTimer(minutes: Int) {
        sleepTimerMinutes.value = minutes
        isSleepTimerActive.value = true
        startSleepTimerCountdown()
    }

    fun cancelSleepTimer() {
        sleepTimerMinutes.value = null
        isSleepTimerActive.value = false
        sleepTimerJob?.cancel()
    }

    private fun startSleepTimerCountdown() {
        sleepTimerJob?.cancel()
        sleepTimerJob = viewModelScope.launch {
            while (isSleepTimerActive.value) {
                val mins = sleepTimerMinutes.value ?: 0
                if (mins <= 1) {
                    // Tick second-level fine resolution for final minute
                    var secondsLeft = 60
                    while (secondsLeft > 0 && isSleepTimerActive.value) {
                        delay(1000)
                        secondsLeft--
                    }
                    // Pause playback, triggers action
                    isPlaying.value = false
                    sleepTimerMinutes.value = null
                    isSleepTimerActive.value = false
                    break
                } else {
                    delay(60000) // Sleep 1 minute
                    if (isSleepTimerActive.value) {
                        sleepTimerMinutes.value = mins - 1
                    }
                }
            }
        }
    }

    fun createCustomTrack(title: String, artist: String, folder: String) {
        viewModelScope.launch {
            val track = Track(
                title = title.ifBlank { "أغنية مجهولة" },
                artist = artist.ifBlank { "فنان مجهول" },
                album = "مخصص",
                durationMs = 210000,
                path = "custom_${System.currentTimeMillis()}",
                folder = folder.ifBlank { "الموسيقى المحلية" }
            )
            repository.addTrack(track)
        }
    }

    class Factory(
        private val application: Application,
        private val repository: MusicRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MusicPlayerViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MusicPlayerViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
