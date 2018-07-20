package com.gt22.web.utlis

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.gt22.uadam.utils.contains
import com.gt22.uadam.utils.set
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

fun rep(msg: String, isError: Boolean = true): JsonObject {
    val reply = JsonObject()
    reply["error"] = isError
    reply["message"] = msg
    return reply
}

fun HttpServletRequest.params(): Map<String, String> {
    return parameterMap.mapValues { it.value[0] }
}

fun HttpServletResponse.prepare(encoding: String = "UTF-8", forbidCache: Boolean = true, mimeType: String = "application/json") {
    contentType = "$mimeType; charset=$encoding"
    characterEncoding = encoding
    if(forbidCache) {
        setHeader("Cache-Control", "no-cache, no-store, must-revalidate")
        setHeader("Pragma", "no-cache")
        setHeader("Expires", "0")
    }
}

fun formatResponse(rep: JsonElement): JsonObject {
    val obj = JsonObject()
    if(rep is JsonObject) {
        if("error" in rep) {
            obj["error"] = rep["error"]
            rep.remove("error")
        } else {
            obj["error"] = false
        }
        if("message" in rep) {
            obj["message"] = rep["message"]
            rep.remove("message")
        } else {
            obj["message"] = "Success"
        }
        if(rep.size() > 0) {
            obj["data"] = rep
        }
    } else {
        obj["message"] = "Success"
        obj["error"] = false
        obj["data"] = rep
    }
    return obj
}

class ParamNotSetException(msg: String) : Exception(msg)

fun checkIsSet(params: Map<String, String>, vararg fields: String) {
    for(field in fields) {
        if(field !in params) {
            throw ParamNotSetException("$field not set")
        }
    }
}