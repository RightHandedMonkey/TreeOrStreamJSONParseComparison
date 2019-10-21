package com.example.android.json

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import java.io.InputStream

class MediumJsonStream: ParseInterface {
    override fun getNetTimeAndParseTime(): String {
        return timing.logText
    }

    var timing: TimingInterface = TimingInterface()

    override fun parse(inputStream: InputStream): List<String> {
        timing.updateTimeMS()
        val list: ArrayList<String> = arrayListOf()
        val jFactory = JsonFactory()
        val parser = jFactory.createParser(inputStream)
        var jsonToken: JsonToken? = null
        while (!parser.isClosed) {
            jsonToken = parser.nextToken()
            if (jsonToken == null) {
                break
            }
            if (JsonToken.FIELD_NAME.equals(jsonToken) && "data".equals(parser.currentName)) {
                // start of array
                jsonToken = parser.nextToken()
                if (!JsonToken.START_ARRAY.equals(jsonToken)) {
                    // bail out
                    break
                }

                while (!parser.isClosed) {
                    // each element of the array is an array so the next token
                    // should be [
                    jsonToken = parser.nextToken()
                    if (!JsonToken.START_ARRAY.equals(jsonToken)) {
                        break
                    }
                    // we are now looking for the last two fields. We
                    // continue looking till we find all such fields. This is
                    // probably not a best way to parse this json, but this will
                    // suffice for this example.
                    val tempList: ArrayList<String> = arrayListOf()
                    while (true) {
                        jsonToken = parser.nextToken()
                        if (jsonToken == null) {
                            break
                        }
                        // start of array
                        if (JsonToken.END_ARRAY.equals(jsonToken)) {
                            // bail out
                            break
                        }
                        tempList.add(parser.text)
                    }
                    if (tempList.size > 10) {
                        list.add(tempList[9] + " - " + tempList[10])
                    }
                }
            }
        }
        var diffTime = timing.diffTimeMS()
        timing.logText += "Time to read/parse entire network call in MS: "+diffTime+"\r\n"
        return list
    }
}

