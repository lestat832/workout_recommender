package com.workoutapp.data.database

import com.workoutapp.data.database.entities.ExerciseEntity
import com.workoutapp.domain.model.MuscleGroup
import com.workoutapp.domain.model.WorkoutType

object ExerciseDataV2 {
    private const val IMAGE_BASE_URL = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"
    
    val exercises = listOf(
        // CHEST EXERCISES
        ExerciseEntity(
            id = "1",
            name = "Barbell Bench Press",
            muscleGroups = listOf(MuscleGroup.CHEST),
            equipment = "Barbell",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Barbell_Bench_Press_-_Medium_Grip/0.jpg",
            instructions = listOf(
                "Lie back on a flat bench. Using a medium width grip, lift the bar from the rack and hold it straight over you with your arms locked.",
                "From the starting position, breathe in and begin coming down slowly until the bar touches your middle chest.",
                "After a brief pause, push the bar back to the starting position as you breathe out.",
                "Lock your arms and squeeze your chest in the contracted position at the top of the motion."
            )
        ),
        ExerciseEntity(
            id = "2",
            name = "Dumbbell Bench Press",
            muscleGroups = listOf(MuscleGroup.CHEST),
            equipment = "Dumbbell",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Dumbbell_Bench_Press/0.jpg",
            instructions = listOf(
                "Lie down on a flat bench with a dumbbell in each hand resting on top of your thighs.",
                "Using your thighs to help push the dumbbells up, lift the dumbbells one at a time so that you can hold them in front of you at shoulder width.",
                "Lower the weights slowly to the sides of your chest.",
                "Push the dumbbells back up to the starting position and squeeze your chest."
            )
        ),
        ExerciseEntity(
            id = "3",
            name = "Incline Dumbbell Press",
            muscleGroups = listOf(MuscleGroup.CHEST),
            equipment = "Dumbbell",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Incline_Dumbbell_Press/0.jpg",
            instructions = listOf(
                "Lie back on an incline bench with a dumbbell in each hand atop your thighs.",
                "Using your thighs to push the dumbbells up, lift the dumbbells one at a time and hold them at shoulder width.",
                "Push the dumbbells up with your chest and lock your arms at the top.",
                "Lower the weight slowly back down to the starting position."
            )
        ),
        ExerciseEntity(
            id = "4",
            name = "Cable Crossover",
            muscleGroups = listOf(MuscleGroup.CHEST),
            equipment = "Cable",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Cable_Crossover/0.jpg",
            instructions = listOf(
                "Set the pulleys at a high position, select the resistance, and hold the pulleys in each hand.",
                "Step forward in front of an imaginary straight line between both pulleys while pulling your arms together in front of you.",
                "With a slight bend at your elbows, extend your arms to the side in a wide arc until you feel a stretch in your chest.",
                "Return your arms back to the starting position as you breathe out."
            )
        ),
        ExerciseEntity(
            id = "5",
            name = "Push-Ups",
            muscleGroups = listOf(MuscleGroup.CHEST),
            equipment = "Bodyweight",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Pushups/0.jpg",
            instructions = listOf(
                "Lie on the floor face down and place your hands about 36 inches apart.",
                "Push your body up off the floor extending your arms while keeping your body straight.",
                "Lower yourself down until your chest almost touches the floor.",
                "Push your body back up to the starting position and repeat."
            )
        ),
        
        // BACK EXERCISES
        ExerciseEntity(
            id = "6",
            name = "Bent Over Barbell Row",
            muscleGroups = listOf(MuscleGroup.BACK),
            equipment = "Barbell",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Bent_Over_Barbell_Row/0.jpg",
            instructions = listOf(
                "Hold a barbell with a pronated grip, bend your knees slightly and bring your torso forward by bending at the waist.",
                "Keep your back straight and head up. The barbell should hang directly in front of you.",
                "While keeping the torso stationary, lift the barbell to your chest.",
                "Lower the barbell back to the starting position."
            )
        ),
        ExerciseEntity(
            id = "7",
            name = "Pull-ups",
            muscleGroups = listOf(MuscleGroup.BACK),
            equipment = "Bodyweight",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Pullups/0.jpg",
            instructions = listOf(
                "Grab the pull-up bar with the palms facing forward using the prescribed grip.",
                "Hang with your arms fully extended and your feet off the floor.",
                "Pull yourself up until your chin is over the bar.",
                "Lower yourself back down to the starting position."
            )
        ),
        ExerciseEntity(
            id = "8",
            name = "Lat Pulldown",
            muscleGroups = listOf(MuscleGroup.BACK),
            equipment = "Cable",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Wide-Grip_Lat_Pulldown/0.jpg",
            instructions = listOf(
                "Sit down on a pull-down machine with a wide bar attached to the top pulley.",
                "Grab the bar with the palms facing forward using the prescribed grip.",
                "Pull the bar down until it touches your upper chest by drawing the shoulders and upper arms down and back.",
                "After a second, slowly raise the bar back to the starting position."
            )
        ),
        ExerciseEntity(
            id = "9",
            name = "Seated Cable Row",
            muscleGroups = listOf(MuscleGroup.BACK),
            equipment = "Cable",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Seated_Cable_Rows/0.jpg",
            instructions = listOf(
                "Sit on the machine and place your feet on the front platform with knees slightly bent.",
                "Grab the V-bar handles and pull back until your torso is at a 90-degree angle from your legs.",
                "Keeping your back straight, pull the handles back towards your torso while keeping the arms close to it.",
                "Slowly return the handles to the starting position."
            )
        ),
        ExerciseEntity(
            id = "10",
            name = "Deadlifts",
            muscleGroups = listOf(MuscleGroup.BACK),
            equipment = "Barbell",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Barbell_Deadlift/0.jpg",
            instructions = listOf(
                "Stand with feet hip-width apart and grip the bar with hands just outside your legs.",
                "Bend at your hips and knees, keeping your back straight and chest up.",
                "Drive through your heels to lift the bar, extending your hips and knees.",
                "Lower the bar back to the ground by pushing your hips back and bending your knees."
            )
        ),
        
        // SHOULDER EXERCISES
        ExerciseEntity(
            id = "11",
            name = "Barbell Shoulder Press",
            muscleGroups = listOf(MuscleGroup.SHOULDER),
            equipment = "Barbell",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Barbell_Shoulder_Press/0.jpg",
            instructions = listOf(
                "Sit on a bench with back support in a squat rack. Position a barbell at a height just above your head.",
                "Grab the barbell with a pronated grip and lift it up over your head by locking your arms.",
                "Lower the bar down to the shoulders slowly.",
                "Lift the bar back up to the starting position."
            )
        ),
        ExerciseEntity(
            id = "12",
            name = "Dumbbell Shoulder Press",
            muscleGroups = listOf(MuscleGroup.SHOULDER),
            equipment = "Dumbbell",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Dumbbell_Shoulder_Press/0.jpg",
            instructions = listOf(
                "Sit on a bench with back support and hold two dumbbells at shoulder level.",
                "Press the dumbbells up and together until your arms are fully extended.",
                "Pause at the top and squeeze your shoulders.",
                "Lower the dumbbells back down to the starting position."
            )
        ),
        ExerciseEntity(
            id = "13",
            name = "Lateral Raises",
            muscleGroups = listOf(MuscleGroup.SHOULDER),
            equipment = "Dumbbell",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Side_Lateral_Raise/0.jpg",
            instructions = listOf(
                "Stand with a dumbbell in each hand at your sides.",
                "Keep your torso stationary and lift the dumbbells to your side with a slight bend at the elbow.",
                "Continue to raise the weights until your arms are parallel to the floor.",
                "Lower the dumbbells back down slowly to the starting position."
            )
        ),
        ExerciseEntity(
            id = "14",
            name = "Front Raises",
            muscleGroups = listOf(MuscleGroup.SHOULDER),
            equipment = "Dumbbell",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Front_Dumbbell_Raise/0.jpg",
            instructions = listOf(
                "Stand with a dumbbell in each hand in front of your thighs.",
                "Raise one dumbbell forward and up until your arm is slightly above parallel.",
                "Lower the dumbbell back down slowly.",
                "Alternate arms or raise both dumbbells together."
            )
        ),
        ExerciseEntity(
            id = "15",
            name = "Face Pulls",
            muscleGroups = listOf(MuscleGroup.SHOULDER),
            equipment = "Cable",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Face_Pull/0.jpg",
            instructions = listOf(
                "Set the cable at face height and attach a rope handle.",
                "Grab the rope with both hands and step back to create tension.",
                "Pull the rope towards your face while separating your hands.",
                "Return to the starting position with control."
            )
        ),
        
        // LEGS EXERCISES
        ExerciseEntity(
            id = "16",
            name = "Barbell Squat",
            muscleGroups = listOf(MuscleGroup.LEGS),
            equipment = "Barbell",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Barbell_Full_Squat/0.jpg",
            instructions = listOf(
                "Set the bar on a rack just below shoulder level. Step under the bar and place it on your shoulders.",
                "Hold the bar using both arms and lift it off the rack by pushing with your legs.",
                "Lower your body by bending the knees and hips as if sitting back into a chair.",
                "Drive through your heels to return to the starting position."
            )
        ),
        ExerciseEntity(
            id = "17",
            name = "Leg Press",
            muscleGroups = listOf(MuscleGroup.LEGS),
            equipment = "Machine",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Leg_Press/0.jpg",
            instructions = listOf(
                "Sit on the leg press machine and place your feet on the platform shoulder-width apart.",
                "Press the platform up to release the handles and support the weight.",
                "Lower the platform by bending your knees until they reach 90 degrees.",
                "Push through your heels to press the platform back to the starting position."
            )
        ),
        ExerciseEntity(
            id = "18",
            name = "Romanian Deadlift",
            muscleGroups = listOf(MuscleGroup.LEGS),
            equipment = "Barbell",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Romanian_Deadlift/0.jpg",
            instructions = listOf(
                "Hold a barbell at hip level with a pronated grip.",
                "Keep your knees slightly bent and lower the bar by moving your hips back.",
                "Lower the bar to just below your knees while keeping your back straight.",
                "Drive your hips forward to return to the starting position."
            )
        ),
        ExerciseEntity(
            id = "19",
            name = "Walking Lunges",
            muscleGroups = listOf(MuscleGroup.LEGS),
            equipment = "Dumbbell",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Dumbbell_Lunges/0.jpg",
            instructions = listOf(
                "Stand with dumbbells in each hand at your sides.",
                "Step forward with one leg and lower your hips until both knees are bent at 90 degrees.",
                "Push through the front heel to bring your back foot forward.",
                "Continue alternating legs as you walk forward."
            )
        ),
        ExerciseEntity(
            id = "20",
            name = "Leg Curls",
            muscleGroups = listOf(MuscleGroup.LEGS),
            equipment = "Machine",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Lying_Leg_Curls/0.jpg",
            instructions = listOf(
                "Lie face down on the leg curl machine and position your ankles under the pads.",
                "Curl your legs up as far as possible without lifting your hips off the bench.",
                "Squeeze your hamstrings at the top of the movement.",
                "Slowly lower the weight back to the starting position."
            )
        ),
        
        // BICEP EXERCISES
        ExerciseEntity(
            id = "21",
            name = "Barbell Curl",
            muscleGroups = listOf(MuscleGroup.BICEP),
            equipment = "Barbell",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Barbell_Curl/0.jpg",
            instructions = listOf(
                "Stand with your feet shoulder-width apart and hold a barbell with an underhand grip.",
                "Keep your elbows close to your torso and curl the weight up.",
                "Squeeze your biceps at the top of the movement.",
                "Lower the barbell back down with control."
            )
        ),
        ExerciseEntity(
            id = "22",
            name = "Dumbbell Curl",
            muscleGroups = listOf(MuscleGroup.BICEP),
            equipment = "Dumbbell",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Dumbbell_Bicep_Curl/0.jpg",
            instructions = listOf(
                "Stand with a dumbbell in each hand at arm's length.",
                "Keep your elbows close to your torso and rotate the palms to face forward.",
                "Curl the weights while contracting your biceps.",
                "Slowly begin to bring the dumbbells back to the starting position."
            )
        ),
        ExerciseEntity(
            id = "23",
            name = "Hammer Curl",
            muscleGroups = listOf(MuscleGroup.BICEP),
            equipment = "Dumbbell",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Hammer_Curls/0.jpg",
            instructions = listOf(
                "Stand with a dumbbell in each hand with palms facing your torso.",
                "Keep your elbows stationary and curl the weights forward.",
                "Continue to raise the weights until your biceps are fully contracted.",
                "Lower the dumbbells back down to the starting position."
            )
        ),
        ExerciseEntity(
            id = "24",
            name = "Preacher Curl",
            muscleGroups = listOf(MuscleGroup.BICEP),
            equipment = "Barbell",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Preacher_Curl/0.jpg",
            instructions = listOf(
                "Sit on the preacher bench and place the backs of your arms on the pad.",
                "Grasp the barbell with an underhand grip.",
                "Curl the bar up until your biceps are fully contracted.",
                "Lower the weight slowly back to the starting position."
            )
        ),
        ExerciseEntity(
            id = "25",
            name = "Cable Curl",
            muscleGroups = listOf(MuscleGroup.BICEP),
            equipment = "Cable",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Cable_Hammer_Curls_-_Rope_Attachment/0.jpg",
            instructions = listOf(
                "Stand in front of a low pulley with a straight bar attachment.",
                "Grab the bar with an underhand grip and keep your elbows at your sides.",
                "Curl the bar up by contracting your biceps.",
                "Slowly lower the bar back to the starting position."
            )
        ),
        
        // TRICEP EXERCISES
        ExerciseEntity(
            id = "26",
            name = "Close-Grip Bench Press",
            muscleGroups = listOf(MuscleGroup.TRICEP),
            equipment = "Barbell",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Close-Grip_Barbell_Bench_Press/0.jpg",
            instructions = listOf(
                "Lie on a flat bench and grasp the barbell with hands closer than shoulder width.",
                "Lower the bar straight down to your chest while keeping elbows close to your body.",
                "Press the bar back up by extending your arms.",
                "Focus on using your triceps to move the weight."
            )
        ),
        ExerciseEntity(
            id = "27",
            name = "Tricep Dips",
            muscleGroups = listOf(MuscleGroup.TRICEP),
            equipment = "Bodyweight",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Dips_-_Triceps_Version/0.jpg",
            instructions = listOf(
                "Position yourself on the dip bars with arms straight and body upright.",
                "Lower your body by bending your elbows while leaning slightly forward.",
                "Descend until your shoulders are below your elbows.",
                "Push yourself back up to the starting position."
            )
        ),
        ExerciseEntity(
            id = "28",
            name = "Overhead Tricep Extension",
            muscleGroups = listOf(MuscleGroup.TRICEP),
            equipment = "Dumbbell",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Dumbbell_One-Arm_Triceps_Extension/0.jpg",
            instructions = listOf(
                "Stand or sit with a dumbbell held overhead with both hands.",
                "Keep your upper arms close to your head and lower the weight behind your head.",
                "Lower until your forearms touch your biceps.",
                "Extend your arms back to the starting position."
            )
        ),
        ExerciseEntity(
            id = "29",
            name = "Cable Tricep Pushdown",
            muscleGroups = listOf(MuscleGroup.TRICEP),
            equipment = "Cable",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Triceps_Pushdown/0.jpg",
            instructions = listOf(
                "Stand in front of a high pulley with a straight bar attachment.",
                "Grab the bar with an overhand grip and bring your elbows to your sides.",
                "Push the bar down by extending your forearms.",
                "Return to the starting position with control."
            )
        ),
        ExerciseEntity(
            id = "30",
            name = "Diamond Push-ups",
            muscleGroups = listOf(MuscleGroup.TRICEP),
            equipment = "Bodyweight",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Diamond_Pushups/0.jpg",
            instructions = listOf(
                "Get in push-up position and place your hands close together forming a diamond shape.",
                "Lower your body while keeping your elbows close to your sides.",
                "Push back up to the starting position.",
                "Keep your core tight throughout the movement."
            )
        ),
        // Additional exercises to expand database
        ExerciseEntity(
            id = "31",
            name = "Cable Crossover",
            muscleGroups = listOf(MuscleGroup.CHEST),
            equipment = "Cable",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Cable_Crossover/0.jpg",
            instructions = listOf(
                "Position the pulleys above your head and select the resistance.",
                "Hold the pulleys in each hand with your arms spread and facing forward.",
                "Bring your hands together in front of you in a hugging motion.",
                "Return to the starting position with control."
            )
        ),
        ExerciseEntity(
            id = "32",
            name = "Face Pull",
            muscleGroups = listOf(MuscleGroup.SHOULDER),
            equipment = "Cable",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Face_Pull/0.jpg",
            instructions = listOf(
                "Set cable at face height with rope attachment.",
                "Pull the rope towards your face, separating hands at the end.",
                "Focus on squeezing your rear delts and upper back.",
                "Return to starting position with control."
            )
        ),
        ExerciseEntity(
            id = "33",
            name = "Hammer Curl",
            muscleGroups = listOf(MuscleGroup.BICEP),
            equipment = "Dumbbell",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Hammer_Curl/0.jpg",
            instructions = listOf(
                "Stand with dumbbells at your sides, palms facing your body.",
                "Keep your elbows stationary and curl the weights up.",
                "Maintain the neutral grip throughout the movement.",
                "Lower under control to starting position."
            )
        ),
        ExerciseEntity(
            id = "34",
            name = "Leg Extension",
            muscleGroups = listOf(MuscleGroup.LEGS),
            equipment = "Machine",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Leg_Extensions/0.jpg",
            instructions = listOf(
                "Sit on the leg extension machine with your back against the backrest.",
                "Place your ankles behind the lower pad.",
                "Extend your legs to the maximum, exhaling as you do so.",
                "Lower the weight back down slowly."
            )
        ),
        ExerciseEntity(
            id = "35",
            name = "T-Bar Row",
            muscleGroups = listOf(MuscleGroup.BACK),
            equipment = "Barbell",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}T-Bar_Row_with_Handle/0.jpg",
            instructions = listOf(
                "Stand over the bar with feet shoulder-width apart.",
                "Bend at the hips and knees, keeping your back straight.",
                "Pull the weight towards your chest, squeezing your back.",
                "Lower under control to starting position."
            )
        ),
        ExerciseEntity(
            id = "36",
            name = "Bulgarian Split Squat",
            muscleGroups = listOf(MuscleGroup.LEGS),
            equipment = "Bodyweight",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Bodyweight_Bulgarian_Split_Squat/0.jpg",
            instructions = listOf(
                "Stand 2-3 feet in front of a bench.",
                "Place your rear foot on the bench behind you.",
                "Lower your body by bending your front knee.",
                "Push through your front heel to return to start."
            )
        ),
        ExerciseEntity(
            id = "37",
            name = "Arnold Press",
            muscleGroups = listOf(MuscleGroup.SHOULDER),
            equipment = "Dumbbell",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Arnold_Dumbbell_Press/0.jpg",
            instructions = listOf(
                "Start with dumbbells at shoulder level, palms facing you.",
                "As you press up, rotate your palms to face forward.",
                "Press the weights overhead until arms are extended.",
                "Reverse the motion to return to start."
            )
        ),
        ExerciseEntity(
            id = "38",
            name = "Cable Lateral Raise",
            muscleGroups = listOf(MuscleGroup.SHOULDER),
            equipment = "Cable",
            category = WorkoutType.PUSH,
            imageUrl = "${IMAGE_BASE_URL}Cable_Lateral_Raise/0.jpg",
            instructions = listOf(
                "Stand sideways to the cable machine.",
                "Hold the handle with the far hand across your body.",
                "Raise your arm out to the side to shoulder height.",
                "Lower with control to starting position."
            )
        ),
        ExerciseEntity(
            id = "39",
            name = "Hack Squat",
            muscleGroups = listOf(MuscleGroup.LEGS),
            equipment = "Machine",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Sled_Hack_Squat/0.jpg",
            instructions = listOf(
                "Position yourself on the hack squat machine.",
                "Place feet shoulder-width apart on the platform.",
                "Lower by bending knees to 90 degrees.",
                "Push through heels to return to start."
            )
        ),
        ExerciseEntity(
            id = "40",
            name = "Preacher Curl",
            muscleGroups = listOf(MuscleGroup.BICEP),
            equipment = "Barbell",
            category = WorkoutType.PULL,
            imageUrl = "${IMAGE_BASE_URL}Preacher_Curl/0.jpg",
            instructions = listOf(
                "Sit on the preacher bench with arms over the pad.",
                "Hold the barbell with an underhand grip.",
                "Curl the weight up while keeping upper arms on the pad.",
                "Lower slowly to full extension."
            )
        )
    )
}