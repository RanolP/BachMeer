package io.github.ranolp.bachmeer.compile

class CompilerOption(val useMagicLookup: Boolean = false, val resultExtension: String? = null) {
    companion object {
        val DEFAULT = CompilerOption()
    }
}
