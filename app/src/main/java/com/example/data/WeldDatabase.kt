package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WeldJoint::class], version = 3, exportSchema = false)
abstract class WeldDatabase : RoomDatabase() {

    abstract fun weldJointDao(): WeldJointDao

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
