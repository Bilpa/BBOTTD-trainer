package org.example.bbottd_trainer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform