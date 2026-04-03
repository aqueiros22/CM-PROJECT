package com.example.fieldsense.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.fieldsense.data.model.Note
import com.example.fieldsense.data.model.Visit
import com.example.fieldsense.data.model.Attachment
import com.example.fieldsense.data.local.AttachmentDao

@Database(entities = [Visit::class, Note::class, Attachment::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun visitDao(): VisitDao
    abstract fun noteDao(): NoteDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fieldsense_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}