package com.example.ourmajor.data.history

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SessionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app.db"
                )
                    // TODO: Remove before production release. Add proper migrations and remove
                    // fallbackToDestructiveMigration() to prevent data loss on schema updates.
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                INSTANCE = db
                db
            }
        }
    }
}
