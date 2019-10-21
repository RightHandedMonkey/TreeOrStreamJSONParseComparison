package com.example.android.json

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.BufferedReader
import java.io.InputStream

class SmallJsonTree: ParseInterface {
    override fun getNetTimeAndParseTime(): String {
        return timing.logText
    }

    var timing: TimingInterface = TimingInterface()

    override fun parse(inputStream: InputStream): List<String> {
        timing.updateTimeMS()
        val bufferedReader = BufferedReader(inputStream.reader())
        val json = bufferedReader.readText()
        var diffTime = timing.diffTimeMS()
        timing.logText += "Time to read entire network call in MS: "+diffTime+"\r\n"
        timing.updateTimeMS()
        val mapper = jacksonObjectMapper()
        val data: SmallData = mapper.readValue(json)
        diffTime = timing.diffTimeMS()
        timing.logText += "Time to parse JSON in MS: "+diffTime+"\r\n"
        return data.dataset.fields.map { it.name+" - "+it.label }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    data class SmallData(var dataset: SmallDataset)

    @JsonIgnoreProperties(ignoreUnknown=true)
    data class SmallDataset(var fields: List<SmallDataField>)

    @JsonIgnoreProperties(ignoreUnknown=true)
    data class SmallDataField(
        var description: String?=null,
        var label: String="",
        var type: String="",
        var name: String=""
    )
}

