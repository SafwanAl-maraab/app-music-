package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MusicRepository(
    private val context: Context,
    private val trackDao: TrackDao
) {

    val allTracks: Flow<List<Track>> = trackDao.getAllTracks()
    val favoriteTracks: Flow<List<Track>> = trackDao.getFavoriteTracks()

    suspend fun insert(track: Track): Long {
        return trackDao.insertTrack(track)
    }

    suspend fun toggleFavorite(track: Track) {
        val updated = track.copy(isFavorite = !track.isFavorite)
        trackDao.updateTrack(updated)
    }

    suspend fun toggleHidden(track: Track) {
        val updated = track.copy(isHidden = !track.isHidden)
        trackDao.updateTrack(updated)
    }

    suspend fun deleteTrack(track: Track) {
        trackDao.deleteTrack(track)
    }

    suspend fun addTrack(track: Track) {
        trackDao.insertTrack(track)
    }

    // Initial database seeding with premium pre-bundled Arabesque instrumental tracks
    suspend fun checkForInitialSeeding() {
        val count = trackDao.getTracksCount()
        if (count == 0) {
            val seedList = listOf(
                Track(
                    title = "موشح لما بدا يتثنى",
                    artist = "التراث العربي الموسيقي",
                    album = "الروائع الكلاسيكية",
                    durationMs = 210000,
                    path = "track_1",
                    folder = "موشحات وتراث",
                    isFavorite = true,
                    isHidden = false
                ),
                Track(
                    title = "تقاسيم العود الحزين",
                    artist = "الأستاذ رياض حمدي",
                    album = "أوتار الشرق",
                    durationMs = 180000,
                    path = "track_2",
                    folder = "تقاسيم آلات",
                    isFavorite = false,
                    isHidden = false
                ),
                Track(
                    title = "عزف ناي هادئ (نور الهوى)",
                    artist = "العازف سمير شاكر",
                    album = "ألحان الطبيعة",
                    durationMs = 240000,
                    path = "track_3",
                    folder = "طبيعة ناعمة",
                    isFavorite = true,
                    isHidden = false
                ),
                Track(
                    title = "سماعي بياتي قديم",
                    artist = "فرقة الأنغام الشرقية",
                    album = "عبق الماضي",
                    durationMs = 300000,
                    path = "track_4",
                    folder = "موشحات وتراث",
                    isFavorite = false,
                    isHidden = false
                ),
                Track(
                    title = "رقصة القانون الذهبي",
                    artist = "ماجد عبد اللطيف",
                    album = "سحر الليالي",
                    durationMs = 150000,
                    path = "track_5",
                    folder = "تقاسيم آلات",
                    isFavorite = false,
                    isHidden = false
                ),
                Track(
                    title = "نبضات الصحراء (إيقاع بدوي)",
                    artist = "إيقاعات الشرق الأوسط",
                    album = "طبول العز الذهبية",
                    durationMs = 260000,
                    path = "track_6",
                    folder = "إيقاعات شعبية",
                    isFavorite = false,
                    isHidden = false
                )
            )
            trackDao.insertTracks(seedList)
        }
    }

    // Scan function to dynamically insert simulated fresh tracks
    suspend fun scanLocalTracks() {
        val count = trackDao.getTracksCount()
        // Adding dynamic files that are scanned
        val freshScanned = listOf(
            Track(
                title = "لونجا رياض السنباطي",
                artist = "أوركسترا القاهرة للشرق",
                album = "موسوعة السنباطي",
                durationMs = 220000,
                path = "scanned_track_${System.currentTimeMillis()}",
                folder = "ملفات ممسوحة",
                isFavorite = false,
                isHidden = false
            ),
            Track(
                title = "مقطوعة نسيم الأندلس",
                artist = "ثنائي العود والكمان",
                album = "غرناطة المفقودة",
                durationMs = 195000,
                path = "scanned_track_2_${System.currentTimeMillis()}",
                folder = "ملفات ممسوحة",
                isFavorite = false,
                isHidden = false
            )
        )
        // Insert them
        trackDao.insertTracks(freshScanned)
    }
}
