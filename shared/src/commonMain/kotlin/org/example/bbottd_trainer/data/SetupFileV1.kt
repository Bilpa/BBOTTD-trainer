package org.example.bbottd_trainer.data

import kotlinx.serialization.Serializable

// Version 1 of the setup file format. These classes describe the JSON exactly and are kept apart
// from the domain classes, so the domain can change without silently changing saved files.
//
// Rules for this format:
// - Never change these classes in a way that alters the JSON. Any change to the format needs a new
//   version with its own classes and a conversion from the older versions.
// - Enum values are written as fixed strings (see SetupCodec), not taken from Kotlin enum names.
// - Every field is required, and unknown fields are rejected, so a damaged file is not silently
//   loaded with missing data.

/** The top-level object of a version 1 setup file. */
@Serializable
internal data class SetupFileV1(
    val format: String,
    val formatVersion: Int,
    val setup: SetupV1,
)

@Serializable
internal data class SetupV1(
    val id: String,
    val name: String,
    val notes: String,
    val home: TeamV1,
    val away: TeamV1,
    val ball: BallV1,
)

/** A team. Whether it is home or away is given by the field it is stored in. */
@Serializable
internal data class TeamV1(
    val name: String,
    val color: String,
    val players: List<PlayerV1>,
)

@Serializable
internal data class PlayerV1(
    val id: String,
    val name: String,
    val number: Int?,
    val status: String,
    val position: PositionV1?,
)

@Serializable
internal data class BallV1(
    val position: PositionV1?,
)

@Serializable
internal data class PositionV1(
    val x: Int,
    val y: Int,
)
