package com.example.android.json

import java.io.InputStream

interface ParseInterface {
    fun getNetTimeAndParseTime(): String
    fun parse(inputStream: InputStream): List<String>
}

class TimingInterface(var logText: String="") {

    private var timerMS = 0L

    init {
        logText=""
        updateTimeMS()
    }

    fun updateTimeMS() {
        timerMS = System.nanoTime() / 1000000
    }

    fun diffTimeMS(): Long {
        val timeMSNew = System.nanoTime() / 1000000
        val diffMS = timeMSNew - timerMS
        updateTimeMS()
        return diffMS
    }
}