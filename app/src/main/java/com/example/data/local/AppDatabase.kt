package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [DownloadedVideoEntity::class, BookmarkEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "streamfetch_database.db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default popular safe video bookmarks
                        CoroutineScope(Dispatchers.IO).launch {
                            val bookmarkDao = getDatabase(context).bookmarkDao()
                            bookmarkDao.insertBookmark(
                                BookmarkEntity(
                                    title = "Pexels Free Stock Videos",
                                    url = "https://www.pexels.com/videos/",
                                    category = "Stock Media"
                                )
                            )
                            bookmarkDao.insertBookmark(
                                BookmarkEntity(
                                    title = "Pixabay Royalty Free Videos",
                                    url = "https://pixabay.com/videos/",
                                    category = "Stock Media"
                                )
                            )
                            bookmarkDao.insertBookmark(
                                BookmarkEntity(
                                    title = "Wikimedia Commons Video Portal",
                                    url = "https://commons.wikimedia.org/wiki/Category:Videos",
                                    category = "Open Media"
                                )
                            )
                            bookmarkDao.insertBookmark(
                                BookmarkEntity(
                                    title = "Archive.org Moving Images",
                                    url = "https://archive.org/details/movies",
                                    category = "Public Domain"
                                )
                            )
                            bookmarkDao.insertBookmark(
                                BookmarkEntity(
                                    title = "Blender Open Movies (Big Buck Bunny)",
                                    url = "https://peach.blender.org/",
                                    category = "Animation"
                                )
                            )
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
