package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Review
import com.example.data.model.Ticket
import com.example.data.model.PromoCode

@Database(entities = [Review::class, Ticket::class, PromoCode::class], version = 4, exportSchema = false)
abstract class CinemaDatabase : RoomDatabase() {
    abstract fun cinemaDao(): CinemaDao

    companion object {
        @Volatile
        private var INSTANCE: CinemaDatabase? = null

        fun getDatabase(context: Context): CinemaDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        CinemaDatabase::class.java,
                        "cinema_database"
                    )
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    val fallbackInstance = Room.inMemoryDatabaseBuilder(
                        context.applicationContext,
                        CinemaDatabase::class.java
                    ).build()
                    INSTANCE = fallbackInstance
                    fallbackInstance
                }
            }
        }
    }
}
