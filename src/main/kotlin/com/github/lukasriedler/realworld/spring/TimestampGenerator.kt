package com.github.lukasriedler.realworld.spring

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.format.DateTimeFormatterBuilder

@Component
class TimestampGenerator {
    private val timeFormatter = DateTimeFormatterBuilder().appendInstant(3).toFormatter()
    private val clock = Clock.systemUTC()

    fun createTimestamp(): String = timeFormatter.format(clock.instant())
}
