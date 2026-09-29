package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Isometric::class,
        Spool::class,
        WeldJoint::class
    ],
    version = 4,
    exportSchema = false
)
abstract class WeldDatabase : RoomDatabase() {

    abstract fun weldJointDao(): WeldJointDao
    abstract fun isometricDao(): IsometricDao
    abstract fun spoolDao(): SpoolDao

    companion object {
        @Volatile
        private var INSTANCE: WeldDatabase? = null

        fun getDatabase(context: Context): WeldDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeldDatabase::class.java,
                    "welding_database.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
