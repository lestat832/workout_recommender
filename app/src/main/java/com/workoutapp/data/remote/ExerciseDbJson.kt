package com.workoutapp.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ExerciseDB API response model
 * Maps to the Free Exercise DB JSON structure
 */
@Serializable
data class ExerciseDbJson(
    @SerialName("id")
    val id: String,

    @SerialName("name")
    val name: String,

    @SerialName("force")
    val force: String? = null,

    @SerialName("level")
    val level: String,

    @SerialName("mechanic")
    val mechanic: String? = null,

    @SerialName("equipment")
    val equipment: String? = null,

    @SerialName("primaryMuscles")
    val primaryMuscles: List<String>,

    @SerialName("secondaryMuscles")
    val secondaryMuscles: List<String>,

    @SerialName("instructions")
    val instructions: List<String>,

    @SerialName("category")
    val category: String,

    @SerialName("images")
    val images: List<String>
)
