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
import com.workoutapp.data.database.dao.TrainingProfileDao
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
        GymEntity::class,
        ExerciseProfileEntity::class,
        MuscleGroupProfileEntity::class,
        GlobalProfileEntity::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(DateConverter::class, SetListConverter::class, StringListConverter::class, MuscleGroupListConverter::class)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun stravaSyncDao(): StravaSyncDao
    abstract fun stravaAuthDao(): StravaAuthDao
    abstract fun gymDao(): GymDao
    abstract fun trainingProfileDao(): TrainingProfileDao
    
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

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add workout format columns (nullable except format).
                db.execSQL("ALTER TABLE workouts ADD COLUMN format TEXT NOT NULL DEFAULT 'STRENGTH'")
                db.execSQL("ALTER TABLE workouts ADD COLUMN durationMinutes INTEGER")
                db.execSQL("ALTER TABLE workouts ADD COLUMN completedRounds INTEGER")

                // Transition the Phase 1 Home Gym row to conditioning style and
                // expand its equipment list to the real home inventory. Targets
                // only the exact Phase 1 shape so users who customized their
                // Home Gym are left alone.
                val homeEquipment = "Dumbbell,Bodyweight,Suspension Trainer,Medicine Ball,Ab Wheel,Indoor Rower,Indoor Bike,Jump Rope"
                db.execSQL(
                    """
                    UPDATE gyms
                    SET equipmentList = '$homeEquipment',
                        workoutStyle = 'CONDITIONING'
                    WHERE name = 'Home Gym'
                      AND workoutStyle = 'STRENGTH'
                      AND equipmentList = 'Dumbbell,Bodyweight'
                    """
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Phase 0: taxonomy foundation.
                // Add exerciseCategory column with a placeholder default; the app runs
                // a one-time Kotlin backfill via InitializeExercisesUseCase after
                // startup to set the correct value from muscleGroups + legacy category.
                db.execSQL("ALTER TABLE exercises ADD COLUMN exerciseCategory TEXT NOT NULL DEFAULT 'STRENGTH_PUSH'")

                // Phase 1: gym mode selection.
                db.execSQL("ALTER TABLE gyms ADD COLUMN workoutStyle TEXT NOT NULL DEFAULT 'STRENGTH'")

                // Conservative rename: only touch a row that looks like the original
                // MIGRATION_6_7 seed (name unchanged, default, full commercial equipment).
                // Users who renamed their default gym or customized its equipment are
                // left alone.
                db.execSQL(
                    "UPDATE gyms SET name = 'LMU Gym' " +
                        "WHERE name = 'Home Gym' AND isDefault = 1 AND equipmentList LIKE '%Barbell%'"
                )

                // Insert a new Home Gym row with a narrow strength equipment subset.
                // Every upgraded user gets this row appended. Multi-gym users retain
                // all existing gyms; the new row is just a second option.
                val homeEquipment = "Dumbbell,Bodyweight"
                val currentTime = System.currentTimeMillis()
                db.execSQL(
                    """
                    INSERT INTO gyms (name, equipmentList, isDefault, createdAt, workoutStyle)
                    VALUES ('Home Gym', '$homeEquipment', 0, $currentTime, 'STRENGTH')
                    """
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS exercise_profiles (
                        exerciseId TEXT NOT NULL PRIMARY KEY,
                        loadingPattern TEXT NOT NULL DEFAULT 'UNKNOWN',
                        loadingPatternConfidence INTEGER NOT NULL DEFAULT 0,
                        currentWorkingWeight REAL,
                        warmupWeight REAL,
                        rampSteps INTEGER NOT NULL DEFAULT 0,
                        bodyweightOnly INTEGER NOT NULL DEFAULT 0,
                        preferredWorkingSets INTEGER NOT NULL DEFAULT 3,
                        preferredRepsMin INTEGER NOT NULL DEFAULT 8,
                        preferredRepsMax INTEGER NOT NULL DEFAULT 10,
                        lastProgressionDate INTEGER,
                        progressionRateLbPerMonth REAL,
                        estimatedOneRepMax REAL,
                        plateauFlag INTEGER NOT NULL DEFAULT 0,
                        plateauSessionCount INTEGER NOT NULL DEFAULT 0,
                        sessionCount INTEGER NOT NULL DEFAULT 0,
                        strengthSessionCount INTEGER NOT NULL DEFAULT 0,
                        lastPerformedDate INTEGER,
                        lastUpdated INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS muscle_group_profiles (
                        muscleGroup TEXT NOT NULL PRIMARY KEY,
                        weeklySetVolume REAL NOT NULL DEFAULT 0,
                        volumeTolerance INTEGER,
                        coveragePercentage REAL NOT NULL DEFAULT 0,
                        preferredExerciseIds TEXT NOT NULL DEFAULT '',
                        avoidedExerciseIds TEXT NOT NULL DEFAULT '',
                        lastTrainedDate INTEGER,
                        sessionCount INTEGER NOT NULL DEFAULT 0,
                        lastUpdated INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS global_profile (
                        id INTEGER NOT NULL PRIMARY KEY,
                        avgSessionDurationMin REAL NOT NULL DEFAULT 0,
                        avgExercisesPerSession REAL NOT NULL DEFAULT 0,
                        avgSetsPerExercise REAL NOT NULL DEFAULT 0,
                        trainingFrequencyPerWeek REAL NOT NULL DEFAULT 0,
                        pushPullRatio REAL NOT NULL DEFAULT 1,
                        preferredTrainingDays TEXT NOT NULL DEFAULT '',
                        totalCompletedSessions INTEGER NOT NULL DEFAULT 0,
                        totalStrengthSessions INTEGER NOT NULL DEFAULT 0,
                        totalConditioningSessions INTEGER NOT NULL DEFAULT 0,
                        currentStreakWeeks INTEGER NOT NULL DEFAULT 0,
                        lastWorkoutDate INTEGER,
                        profileVersion INTEGER NOT NULL DEFAULT 1,
                        lastFullRecompute INTEGER NOT NULL DEFAULT 0,
                        lastUpdated INTEGER NOT NULL
                    )
                """)

                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_profiles_lastPerformedDate ON exercise_profiles(lastPerformedDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_muscle_group_profiles_coveragePercentage ON muscle_group_profiles(coveragePercentage)")
            }
        }

        fun getDatabase(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    // Allow destructive recovery only for hypothetical pre-v3 installs
                    // (the project has no committed v1/v2 migrations and has shipped
                    // v3+ since its earliest tracked schema). Any unhandled v3+ upgrade
                    // will still fail loudly instead of wiping user data.
                    .fallbackToDestructiveMigrationFrom(1, 2)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Fresh install on v8: seed both gyms deterministically.
                            // Exercise initialization is handled by InitializeExercisesUseCase
                            // in the app startup to avoid blocking database creation.
                            val now = System.currentTimeMillis()
                            val lmuEquipment = "Barbell,Dumbbell,Cable,Machine,Bodyweight,Bench,Smith Machine,Kettlebell,Resistance Band,None,Other"
                            val homeEquipment = "Dumbbell,Bodyweight,Suspension Trainer,Medicine Ball,Ab Wheel,Indoor Rower,Indoor Bike,Jump Rope"
                            db.execSQL(
                                """
                                INSERT INTO gyms (name, equipmentList, isDefault, createdAt, workoutStyle)
                                VALUES ('LMU Gym', '$lmuEquipment', 1, $now, 'STRENGTH')
                                """
                            )
                            db.execSQL(
                                """
                                INSERT INTO gyms (name, equipmentList, isDefault, createdAt, workoutStyle)
                                VALUES ('Home Gym', '$homeEquipment', 0, $now, 'CONDITIONING')
                                """
                            )
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}