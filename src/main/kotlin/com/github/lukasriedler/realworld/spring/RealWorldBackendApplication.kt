package com.github.lukasriedler.realworld.spring

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RealWorldBackendApplication

fun main(args: Array<String>) {
    runApplication<RealWorldBackendApplication>(*args)
}
