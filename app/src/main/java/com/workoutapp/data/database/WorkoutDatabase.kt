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
import com.workoutapp.data.database.dao.GymDao
import com.workoutapp.data.database.dao.StravaAuthDao
import com.workoutapp.data.database.dao.StravaSyncDao
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
        WorkoutExerciseEntity::class,
        StravaSyncQueueEntity::class,
        StravaAuthEntity::class,
        GymEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(DateConverter::class, SetListConverter::class, StringListConverter::class, MuscleGroupListConverter::class)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun stravaSyncDao(): StravaSyncDao
    abstract fun stravaAuthDao(): StravaAuthDao
    abstract fun gymDao(): GymDao
    
    companion object {
        @Volatile
        private var INSTANCE: WorkoutDatabase? = null
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add isUserCreated and createdAt columns to exercises table
                db.execSQL("ALTER TABLE exercises ADD COLUMN isUserCreated INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN createdAt INTEGER")
            }
        }
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migrate from single muscleGroup to multiple muscleGroups and remove difficulty
                db.execSQL("CREATE TABLE exercises_new (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, muscleGroups TEXT NOT NULL, equipment TEXT NOT NULL, category TEXT NOT NULL, imageUrl TEXT, instructions TEXT NOT NULL, isUserCreated INTEGER NOT NULL DEFAULT 0, createdAt INTEGER)")

                // Copy data from old table to new, converting single muscleGroup to list
                db.execSQL("INSERT INTO exercises_new (id, name, muscleGroups, equipment, category, imageUrl, instructions, isUserCreated, createdAt) SELECT id, name, muscleGroup, equipment, category, imageUrl, instructions, isUserCreated, createdAt FROM exercises")

                // Drop old table and rename new table
                db.execSQL("DROP TABLE exercises")
                db.execSQL("ALTER TABLE exercises_new RENAME TO exercises")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create Strava sync queue table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS strava_sync_queue (
                        id TEXT NOT NULL PRIMARY KEY,
                        workoutId TEXT NOT NULL,
                        status TEXT NOT NULL,
                        stravaActivityId INTEGER,
                        queuedAt INTEGER NOT NULL,
                        lastAttemptAt INTEGER,
                        completedAt INTEGER,
                        retryCount INTEGER NOT NULL DEFAULT 0,
                        errorMessage TEXT,
                        isUpdate INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // Create Strava auth table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS strava_auth (
                        id INTEGER NOT NULL PRIMARY KEY,
                        accessToken TEXT NOT NULL,
                        refreshToken TEXT NOT NULL,
                        expiresAt INTEGER NOT NULL,
                        athleteId INTEGER NOT NULL,
                        lastRefreshedAt INTEGER NOT NULL,
                        scope TEXT NOT NULL DEFAULT 'activity:write'
                    )
                """)

                // Create index for workoutId lookups
                db.execSQL("CREATE INDEX IF NOT EXISTS index_strava_sync_queue_workoutId ON strava_sync_queue(workoutId)")

                // Create index for status queries
                db.execSQL("CREATE INDEX IF NOT EXISTS index_strava_sync_queue_status ON strava_sync_queue(status)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create gyms table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS gyms (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        equipmentList TEXT NOT NULL,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """)

                // Create index for default gym lookups
                db.execSQL("CREATE INDEX IF NOT EXISTS index_gyms_isDefault ON gyms(isDefault)")

                // Add gymId column to workouts table
                db.execSQL("ALTER TABLE workouts ADD COLUMN gymId INTEGER")

                // Create index for gym lookups in workouts
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workouts_gymId ON workouts(gymId)")

                // Create default "Home Gym" with all equipment for existing users
                val allEquipment = "Barbell,Dumbbell,Cable,Machine,Bodyweight,Bench,Smith Machine,Kettlebell,Resistance Band,None,Other"
                val currentTime = System.currentTimeMillis()
                db.execSQL("""
                    INSERT INTO gyms (name, equipmentList, isDefault, createdAt)
                    VALUES ('Home Gym', '$allEquipment', 1, $currentTime)
                """)
            }
        }
        
        fun getDatabase(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Note: Exercise initialization is handled by InitializeExercisesUseCase
                            // in the app startup to avoid blocking database creation
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}