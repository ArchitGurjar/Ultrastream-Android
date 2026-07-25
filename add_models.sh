#!/bin/bash
cd ~/projects/Ultrastreaming || exit

# Create directories
mkdir -p app/src/main/java/com/ultrastream/app/data/models

# 1. Addon.kt
cat > app/src/main/java/com/ultrastream/app/data/models/Addon.kt <<'EOF'
package com.ultrastream.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "addons")
data class Addon(
    @PrimaryKey val id: String,
    val url: String,
    val name: String,
    val catalogs: String,
    val enabled: Boolean = true,
    val required: Boolean = false
)

data class Catalog(
    val type: String,
    val id: String,
    val name: String,
    val extraSupported: List<String>? = null,
    val extra: List<Extra>? = null
)

data class Extra(
    val name: String,
    val isRequired: Boolean = false,
    val options: List<String>? = null
)
EOF
git add app/src/main/java/com/ultrastream/app/data/models/Addon.kt

# 2. MetaItem.kt
cat > app/src/main/java/com/ultrastream/app/data/models/MetaItem.kt <<'EOF'
package com.ultrastream.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library")
data class LibraryItem(
    @PrimaryKey val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val imdbRating: String?,
    val year: String?,
    val releaseInfo: String?,
    val released: String?,
    val description: String?,
    val genre: String?,
    val runtime: String?,
    val cast: String?,
    val imdbId: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val imdbRating: String?,
    val year: String?,
    val releaseInfo: String?,
    val released: String?,
    val description: String?,
    val genre: String?,
    val runtime: String?,
    val cast: String?,
    val imdbId: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_meta")
data class CachedMeta(
    @PrimaryKey val cacheKey: String,
    val json: String
)

data class MetaItem(
    val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val imdbRating: String?,
    val year: String?,
    val releaseInfo: String?,
    val released: String?,
    val description: String?,
    val genre: List<String>?,
    val runtime: String?,
    val cast: List<String>?,
    val imdbId: String?,
    val videos: List<Video>? = null
)

data class Video(
    val season: Int?,
    val episode: Int?,
    val name: String?,
    val title: String?,
    val description: String?,
    val thumbnail: String?,
    val url: String?
)
EOF
git add app/src/main/java/com/ultrastream/app/data/models/MetaItem.kt

# 3. StreamItem.kt
cat > app/src/main/java/com/ultrastream/app/data/models/StreamItem.kt <<'EOF'
package com.ultrastream.app.data.models

data class StreamItem(
    val url: String?,
    val streamUrl: String?,
    val externalUrl: String?,
    val title: String?,
    val name: String?,
    val description: String?,
    val infoHash: String?,
    val addonName: String?,
    val subtitles: List<Subtitle>?,
    val isLive: Boolean = false
)

data class Subtitle(
    val url: String?,
    val file: String?,
    val lang: String?,
    val name: String?
)
EOF
git add app/src/main/java/com/ultrastream/app/data/models/StreamItem.kt

# 4. SmartPlaylist.kt
cat > app/src/main/java/com/ultrastream/app/data/models/SmartPlaylist.kt <<'EOF'
package com.ultrastream.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smart_playlists")
data class SmartPlaylist(
    @PrimaryKey val id: String,
    val metaId: String,
    val metaName: String,
    val poster: String?,
    val season: Int,
    val addon: String,
    val total: Int,
    val fetched: Int,
    val status: String,
    val episodesJson: String
)

data class PlaylistEpisode(
    val epNum: Int,
    val epName: String,
    val title: String,
    val stream: StreamItem?,
    val isMissing: Boolean = false
)
EOF
git add app/src/main/java/com/ultrastream/app/data/models/SmartPlaylist.kt

# 5. WatchProgress.kt
cat > app/src/main/java/com/ultrastream/app/data/models/WatchProgress.kt <<'EOF'
package com.ultrastream.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_progress")
data class WatchProgress(
    @PrimaryKey val id: String,
    val percent: Int,
    val lastUpdate: Long = System.currentTimeMillis()
)

@Entity(tableName = "watched_episodes")
data class WatchedEpisode(
    @PrimaryKey val episodeKey: String,
    val watched: Boolean = true
)
EOF
git add app/src/main/java/com/ultrastream/app/data/models/WatchProgress.kt

# 6. Profile.kt
cat > app/src/main/java/com/ultrastream/app/data/models/Profile.kt <<'EOF'
package com.ultrastream.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val id: String,
    val name: String,
    val avatar: String
)

data class RecommendedAddon(
    val name: String,
    val description: String,
    val url: String,
    val isInstalled: Boolean = false
)
EOF
git add app/src/main/java/com/ultrastream/app/data/models/Profile.kt

echo "✅ All model files created and staged successfully!"
