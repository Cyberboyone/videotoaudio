package com.nakudin.videotoaudio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nakudin.videotoaudio.data.local.dao.ConversionHistoryDao
import com.nakudin.videotoaudio.data.local.entity.ConversionHistoryEntity

@Database(
    entities = [ConversionHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ConversionHistoryDatabase : RoomDatabase() {

    abstract fun dao(): ConversionHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: ConversionHistoryDatabase? = null

        fun getDatabase(context: Context): ConversionHistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ConversionHistoryDatabase::class.java,
                    "conversion_history.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
