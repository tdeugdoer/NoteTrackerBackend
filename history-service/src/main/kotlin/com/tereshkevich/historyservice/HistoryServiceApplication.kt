package com.tereshkevich.historyservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@EnableFeignClients
@SpringBootApplication
class HistoryServiceApplication

fun main(args: Array<String>) {
    runApplication<HistoryServiceApplication>(*args)
}
