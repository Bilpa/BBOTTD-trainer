package org.example.bbottd_trainer.domain

import kotlin.jvm.JvmInline

/**
 * Stable identifier of a player, unique within a setup across both teams.
 *
 * IDs are opaque: never derive the team, number or anything else from the value.
 */
@JvmInline
value class PlayerId(val value: String)

/** Stable identifier of a tactical setup. Opaque, like [PlayerId]. */
@JvmInline
value class SetupId(val value: String)
