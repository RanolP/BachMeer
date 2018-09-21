package io.github.ranolp.bachmeer.platform

enum class Platform(val displayName: String, val extension: String) {
    BATCH("Batch", "bat"),
    POWER_SHELL("PowerShell", "ps1"),
    BASH("Bash", "sh")
}
