package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Track
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(viewModel: MusicPlayerViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State bindings
    val allTracks by viewModel.allTracks.collectAsState()
    val filteredTracks by viewModel.filteredTracks.collectAsState()
    val availableFolders by viewModel.availableFolders.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progressMs by viewModel.playbackProgressMs.collectAsState()
    val isShuffle by viewModel.isShuffleEnabled.collectAsState()
    val isRepeat by viewModel.isRepeatEnabled.collectAsState()
    val sleepTimerMins by viewModel.sleepTimerMinutes.collectAsState()
    val selectedFolder by viewModel.currentFolder.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val hiddenCount by viewModel.hiddenTracksCount.collectAsState()

    // Screen sub-navigation tab (0: Player, 1: Library/Folders, 2: Smart Sleep Timer / Toolkit)
    var activeTab by remember { mutableStateOf(0) }

    // Dialog state for adding track
    var showAddTrackDialog by remember { mutableStateOf(false) }

    // Force RTL interface for Arabic Localization
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = MidnightBg,
            bottomBar = {
                // Custom Bottom Navigation designed exactly as requested in specifications
                Column(modifier = Modifier.background(MidnightBg)) {
                    HorizontalDivider(color = MetallicGrey, thickness = 1.dp)
                    NavigationBar(
                        containerColor = MidnightBg,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("app_bottom_navigator")
                    ) {
                        NavigationBarItem(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonViolet,
                                selectedTextColor = CyberTeal,
                                indicatorColor = CyberTeal,
                                unselectedIconColor = SoftLilac,
                                unselectedTextColor = SoftLilac
                            ),
                            icon = {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircle,
                                    contentDescription = "المشغل"
                                )
                            },
                            label = { Text("المشغل", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            modifier = Modifier.testTag("tab_player")
                        )

                        NavigationBarItem(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonViolet,
                                selectedTextColor = CyberTeal,
                                indicatorColor = CyberTeal,
                                unselectedIconColor = SoftLilac,
                                unselectedTextColor = SoftLilac
                            ),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.LibraryMusic,
                                    contentDescription = "المكتبة"
                                )
                            },
                            label = { Text("المكتبة", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            modifier = Modifier.testTag("tab_library")
                        )

                        NavigationBarItem(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonViolet,
                                selectedTextColor = CyberTeal,
                                indicatorColor = CyberTeal,
                                unselectedIconColor = SoftLilac,
                                unselectedTextColor = SoftLilac
                            ),
                            icon = {
                                Icon(
                                    imageVector = if (sleepTimerMins != null) Icons.Default.Bedtime else Icons.Outlined.Bedtime,
                                    contentDescription = "المؤقت"
                                )
                            },
                            label = {
                                Text(
                                    text = if (sleepTimerMins != null) "$sleepTimerMins د" else "المؤقت",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            modifier = Modifier.testTag("tab_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeTab) {
                    0 -> PlayerTabContent(
                        viewModel = viewModel,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        progressMs = progressMs,
                        isShuffle = isShuffle,
                        isRepeat = isRepeat,
                        sleepTimerMins = sleepTimerMins,
                        onOpenLibrary = { activeTab = 1 },
                        onOpenSettings = { activeTab = 2 }
                    )
                    1 -> LibraryTabContent(
                        viewModel = viewModel,
                        tracks = filteredTracks,
                        folders = availableFolders,
                        selectedFolder = selectedFolder,
                        query = query,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        hiddenCount = hiddenCount,
                        onAddTrackClick = { showAddTrackDialog = true }
                    )
                    2 -> ToolkitTabContent(
                        viewModel = viewModel,
                        sleepTimerMins = sleepTimerMins,
                        onAddTrackClick = { showAddTrackDialog = true }
                    )
                }

                // Add Track Dialog (Enables full testing by injecting custom user files into Room!)
                if (showAddTrackDialog) {
                    AddTrackDialog(
                        onDismiss = { showAddTrackDialog = false },
                        onSubmit = { title, artist, folder ->
                            viewModel.createCustomTrack(title, artist, folder)
                            showAddTrackDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerTabContent(
    viewModel: MusicPlayerViewModel,
    currentTrack: Track?,
    isPlaying: Boolean,
    progressMs: Long,
    isShuffle: Boolean,
    isRepeat: Boolean,
    sleepTimerMins: Int?,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MetallicGrey)
                        .clickable { onOpenLibrary() }
                        .testTag("btn_top_library"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "المجلدات",
                        tint = CyberTeal
                    )
                }
                Column {
                    Text(
                        text = "الموسيقى المحلية",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (currentTrack != null) currentTrack.folder else "البرنامج جاهز للتجربة",
                        color = SoftLilac,
                        fontSize = 10.sp
                    )
                }
            }

            // Quick Actions (Sleep timer status + settings/toolkit)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sleepTimerMins != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(HighlightGrey)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable { onOpenSettings() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Bedtime, contentDescription = null, tint = CyberTeal, modifier = Modifier.size(12.dp))
                            Text("$sleepTimerMins د", color = CyberTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "خيارات إضافية",
                        tint = SoftLilac
                    )
                }
            }
        }

        // Album Art section with premium radial gradient
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(CyberTeal, NeonViolet),
                        radius = 480f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Animated rotating CD inside or giant musical note Icon
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(110.dp)
            )
            // Small badge showing folder
            if (currentTrack != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightBg.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = currentTrack.album,
                        color = CyberTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Track Details (Title & Artist)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = currentTrack?.title ?: "اختر لحنًا للبدء",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("track_title_label")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentTrack?.artist ?: "انقر لتفعيل مكتبة الملفات المضمنة",
                color = SoftLilac,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.testTag("track_artist_label")
            )
        }

        // Scrubber / Progress Bar Slider
        Column(modifier = Modifier.fillMaxWidth()) {
            val duration = currentTrack?.durationMs ?: 240000L
            val sliderValue = progressMs.toFloat().coerceIn(0f, duration.toFloat())

            Slider(
                value = sliderValue,
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange = 0f..duration.toFloat(),
                colors = SliderDefaults.colors(
                    activeTrackColor = CyberTeal,
                    inactiveTrackColor = MetallicGrey,
                    thumbColor = CyberTeal
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playback_slider")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(progressMs),
                    color = SoftLilac,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "-" + formatTime((duration - progressMs).coerceAtLeast(0L)),
                    color = SoftLilac,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Playback Interface Commands (Shuffling, Skips, PlayToggle, Repeat, Rewind)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Main Command Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.toggleShuffle() },
                    modifier = Modifier.testTag("btn_shuffle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "تبديل عشوائي",
                        tint = if (isShuffle) CyberTeal else SoftLilac,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.onSkipPrevious() },
                        modifier = Modifier.testTag("btn_previous")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "السابق",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(CyberTeal)
                            .clickable { viewModel.togglePlayPause() }
                            .testTag("btn_play_pause"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "تشغيل أو إيقاف مؤقت",
                            tint = NeonViolet,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.onSkipNext() },
                        modifier = Modifier.testTag("btn_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "التالي",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.toggleRepeat() },
                    modifier = Modifier.testTag("btn_repeat")
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "تكرار",
                        tint = if (isRepeat) CyberTeal else SoftLilac,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Secondary Controls (Rewind 10s & Forward 10s with custom labels)
            Row(
                horizontalArrangement = Arrangement.spacedBy(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .clickable { viewModel.rewind10Seconds() }
                        .testTag("btn_rewind"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "رجوع 10 ثواني",
                        tint = SoftLilac,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "رجوع",
                        fontSize = 9.sp,
                        color = SoftLilac,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Column(
                    modifier = Modifier
                        .clickable { viewModel.forward10Seconds() }
                        .testTag("btn_forward"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "تقديم 10 ثواني",
                        tint = SoftLilac,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "تقديم",
                        fontSize = 9.sp,
                        color = SoftLilac,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryTabContent(
    viewModel: MusicPlayerViewModel,
    tracks: List<Track>,
    folders: List<String>,
    selectedFolder: String?,
    query: String,
    currentTrack: Track?,
    isPlaying: Boolean,
    hiddenCount: Int,
    onAddTrackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Search & Directory header bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "مكتبتي الموسيقية",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "إجمالي الملفات: ${tracks.size} مقاطع",
                    color = SoftLilac,
                    fontSize = 11.sp
                )
            }

            // Quick Scan Folders and Add New Tracks button
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { viewModel.scanFolders() },
                    colors = ButtonDefaults.buttonColors(containerColor = HighlightGrey),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_scan_folders")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = CyberTeal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مسح مجلد", color = CyberTeal, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { onAddTrackClick() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_add_track")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة لحن", color = NeonViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Search Input Bar
        TextField(
            value = query,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("ابحث عن أغنية، فنان أو ألبوم...", color = SoftLilac, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SoftLilac) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = SoftLilac)
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CardDarkBg,
                unfocusedContainerColor = CardDarkBg,
                focusedIndicatorColor = CyberTeal,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_text_input"),
            singleLine = true
        )

        // Folders list selector row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(folders) { folder ->
                val isSelected = selectedFolder == folder
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) CyberTeal else CardDarkBg)
                        .clickable { viewModel.currentFolder.value = folder }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("folder_pill_$folder")
                ) {
                    Text(
                        text = folder,
                        color = if (isSelected) NeonViolet else SoftLilac,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // List Header showing details or empty states
        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicOff,
                        contentDescription = null,
                        tint = SoftLilac,
                        modifier = Modifier.size(60.dp)
                    )
                    Text("لا توجد موسيقى مطابقة لبحثك", color = SoftLilac, fontSize = 14.sp)
                    Text(
                        text = "انقر على 'إضافة لحن' لصنع مسار مخصص فوراً!",
                        color = CyberTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Lazy load the files using specialized elegant cards
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(tracks) { track ->
                    val isPlayingThis = currentTrack?.id == track.id
                    TrackLibraryItem(
                        track = track,
                        isPlayingThis = isPlayingThis && isPlaying,
                        isSelectedThis = isPlayingThis,
                        onPlayClick = { viewModel.playTrack(track) },
                        onFavoriteClick = { viewModel.toggleTrackFavorite(track) },
                        onHideClick = { viewModel.hideTrack(track) },
                        onDeleteClick = { viewModel.deleteTrack(track) }
                    )
                }
            }
        }
    }
}

@Composable
fun TrackLibraryItem(
    track: Track,
    isPlayingThis: Boolean,
    isSelectedThis: Boolean,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onHideClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelectedThis) HighlightGrey else CardDarkBg
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick() }
            .testTag("track_item_${track.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Track Graphic avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (isSelectedThis) listOf(CyberTeal, NeonViolet) else listOf(MetallicGrey, HighlightGrey)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlayingThis) Icons.Default.VolumeUp else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isSelectedThis) Color.White else SoftLilac,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Info details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isSelectedThis) CyberTeal else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = track.artist,
                        color = SoftLilac,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("•", color = SoftLilac, fontSize = 11.sp)
                    Text(
                        text = formatTime(track.durationMs),
                        color = SoftLilac,
                        fontSize = 11.sp
                    )
                }
            }

            // Controllers for lists (Favorite switch + action settings dropdown)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "المفضلة",
                        tint = if (track.isFavorite) WarmRose else SoftLilac,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(onClick = { expandedMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "خيارات إضافية",
                            tint = SoftLilac
                        )
                    }

                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false },
                        modifier = Modifier.background(CardDarkBg)
                    ) {
                        DropdownMenuItem(
                            text = { Text("إخفاء الأغنية", color = Color.White) },
                            onClick = {
                                onHideClick()
                                expandedMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = SoftLilac) }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف نهائي", color = WarmRose) },
                            onClick = {
                                onDeleteClick()
                                expandedMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = WarmRose) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolkitTabContent(
    viewModel: MusicPlayerViewModel,
    sleepTimerMins: Int?,
    onAddTrackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "مؤقت النوم الذكي والأدوات",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "تحكم بميزات التشغيل المتقدمة ومؤقت النوم التلقائي",
                color = SoftLilac,
                fontSize = 11.sp
            )
        }

        // Sleep Timer Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDarkBg),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(HighlightGrey),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bedtime, contentDescription = null, tint = CyberTeal)
                        }
                        Column {
                            Text("مؤقت إيقاف التشغيل", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                text = if (sleepTimerMins != null) "سينتهي التشغيل بعد $sleepTimerMins دقيقة" else "المؤقت غير نشط حاليًا",
                                color = SoftLilac,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (sleepTimerMins != null) {
                        Button(
                            onClick = { viewModel.cancelSleepTimer() },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmRose.copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("إلغاء", color = WarmRose, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = MetallicGrey, thickness = 0.5.dp)

                // Timer preset interval options
                Text("اختر الوقت المناسب للإيقاف التلقائي:", color = SoftLilac, fontSize = 11.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(5, 15, 30, 45, 60).forEach { mins ->
                        val isCurrent = sleepTimerMins == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) CyberTeal else HighlightGrey)
                                .clickable { viewModel.setSleepTimer(mins) }
                                .padding(vertical = 10.dp)
                                .testTag("btn_timer_preset_$mins"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$mins د",
                                color = if (isCurrent) NeonViolet else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Feature tools card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDarkBg),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "إدارة سريعة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                // Row action 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddTrackClick() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HighlightGrey),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = CyberTeal)
                    }
                    Column {
                        Text("إضافة أغنية مخصصة للرقم الأساسي", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("إدخال فنان ومجلد مخصص ليتم حفظه في قاعدة البيانات فوراً", color = SoftLilac, fontSize = 10.sp)
                    }
                }

                HorizontalDivider(color = MetallicGrey, thickness = 0.5.dp)

                // Row action 2
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.scanFolders() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HighlightGrey),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Explore, contentDescription = null, tint = CyberTeal)
                    }
                    Column {
                        Text("مسح الملفات والمجلدات المخفية", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("استيراد تلقائي لملفات جديدة من ذاكرة النظام وتخزينها", color = SoftLilac, fontSize = 10.sp)
                    }
                }
            }
        }

        // Description Note about the product
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(HighlightGrey.copy(alpha = 0.6f))
                .padding(16.dp)
        ) {
            Text(
                text = "جميع تصاميم ألحان مخصصة للعمل بالكامل دون إنترنت، مع دمج ذكي لقاعدة بيانات SQLite (Room) المحلية لتخزين تفضيلاتك وسجلات تشغيلك بأمان تام.",
                color = SoftLilac,
                fontSize = 11.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun AddTrackDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf("الموسيقى المحلية") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إضافة لحن جديد للمكتبة",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم اللحن / الأغنية") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberTeal,
                        unfocusedBorderColor = MetallicGrey,
                        focusedLabelColor = CyberTeal,
                        unfocusedLabelColor = SoftLilac,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_input_title")
                )

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("اسم الفنان") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberTeal,
                        unfocusedBorderColor = MetallicGrey,
                        focusedLabelColor = CyberTeal,
                        unfocusedLabelColor = SoftLilac,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_input_artist")
                )

                OutlinedTextField(
                    value = folder,
                    onValueChange = { folder = it },
                    label = { Text("المجلد / التصنيف") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberTeal,
                        unfocusedBorderColor = MetallicGrey,
                        focusedLabelColor = CyberTeal,
                        unfocusedLabelColor = SoftLilac,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_input_folder")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(title, artist, folder) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                modifier = Modifier.testTag("dialog_btn_confirm")
            ) {
                Text("إضافة وحفظ", color = NeonViolet, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_btn_dismiss")
            ) {
                Text("إلغاء", color = SoftLilac)
            }
        },
        containerColor = CardDarkBg,
        textContentColor = WhiteText
    )
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
