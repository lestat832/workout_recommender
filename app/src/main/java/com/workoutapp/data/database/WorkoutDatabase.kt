package com.workoutapp.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import com.workoutapp.data.database.converters.DateConverter
import com.workoutapp.data.database.converters.MuscleGroupListConverter
import com.workoutapp.data.database.converters.SetListConverter
import com.workoutapp.data.database.converters.StringListConverter
import com.workoutapp.data.database.dao.ExerciseDao
import com.workoutapp.data.database.dao.WorkoutDao
import com.workoutapp.data.database.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ExerciseEntity::class,
        UserExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(DateConverter::class, SetListConverter::class, StringListConverter::class, MuscleGroupListConverter::class)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    
    companion object {
        @Volatile
        private var INSTANCE: WorkoutDatabase? = null
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add isUserCreated and createdAt columns to exercises table
                database.execSQL("ALTER TABLE exercises ADD COLUMN isUserCreated INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE exercises ADD COLUMN createdAt INTEGER")
            }
        }
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migrate from single muscleGroup to multiple muscleGroups and remove difficulty
                database.execSQL("CREATE TABLE exercises_new (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, muscleGroups TEXT NOT NULL, equipment TEXT NOT NULL, category TEXT NOT NULL, imageUrl TEXT, instructions TEXT NOT NULL, isUserCreated INTEGER NOT NULL DEFAULT 0, createdAt INTEGER)")
                
                // Copy data from old table to new, converting single muscleGroup to list
                database.execSQL("INSERT INTO exercises_new (id, name, muscleGroups, equipment, category, imageUrl, instructions, isUserCreated, createdAt) SELECT id, name, muscleGroup, equipment, category, imageUrl, instructions, isUserCreated, createdAt FROM exercises")
                
                // Drop old table and rename new table
                database.execSQL("DROP TABLE exercises")
                database.execSQL("ALTER TABLE exercises_new RENAME TO exercises")
            }
        }
        
        fun getDatabase(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}