package io.github.ranolp.bachmeer.util

class TimeChecker {
    companion object {
        val LABEL_DEFAULT = "TimeChecker::Default"
        val LABEL_GENERAL = "TimeChecker::General"
    }

    private data class TimeData(val start: Long, var end: Long) {
        val duration: Double
            get() = (end - start) / 1000.0
    }

    private val dataset: MutableMap<String, TimeData> = mutableMapOf()

    fun <T : Any?> does(label: String = LABEL_DEFAULT, body: () -> T): T {
        start(label)
        try {
            return body()
        } catch (th: Throwable) {
            // rethrow
            throw th
        } finally {
            end(label)
        }
    }

    fun start(label: String = LABEL_DEFAULT) {
        val data = TimeData(System.currentTimeMillis(), -1)
        if (LABEL_GENERAL !in dataset) {
            dataset[LABEL_GENERAL] = data
        }
        dataset[label] = data
    }

    fun end(label: String = LABEL_DEFAULT): Double {
        val data = dataset[label] ?: return Double.NaN
        data.end = System.currentTimeMillis()
        dataset[LABEL_GENERAL]?.end = System.currentTimeMillis()

        return data.duration
    }

    fun get(label: String = LABEL_DEFAULT): Double {
        return dataset[label]?.duration ?: Double.NaN
    }
}
