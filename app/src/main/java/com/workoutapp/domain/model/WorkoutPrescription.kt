package com.workoutapp.domain.model

enum class SetType { WARMUP, RAMP, WORKING }

data class SetPrescription(
    val setType: SetType,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val recommendedWeight: Float? = null,
    val note: String? = null
)

data class WorkoutPrescription(
    val sets: List<SetPrescription>,
    val rationale: String,
    val loadingPattern: LoadingPattern,
    val bodyweightOnly: Boolean = false
) {
    val workingSets: List<SetPrescription> get() = sets.filter { it.setType == SetType.WORKING }
    val totalSets: Int get() = sets.size
}
