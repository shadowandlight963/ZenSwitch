package com.example.ourmajor.data.mindfulness

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [GratitudeEntry::class],
    version = 1,
    exportSchema = false
)
abstract class MindfulnessDatabase : RoomDatabase() {
    abstract fun gratitudeDao(): GratitudeDao

    companion object {
        @Volatile private var INSTANCE: MindfulnessDatabase? = null

        fun get(context: Context): MindfulnessDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    MindfulnessDatabase::class.java,
                    "mindfulness.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                INSTANCE = db
                db
            }
        }
    }
}
