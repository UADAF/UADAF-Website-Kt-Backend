package com.gt22.web.utlis

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.util.function.BiConsumer
import java.util.function.BinaryOperator
import java.util.function.Supplier
import java.util.stream.Collector

inline val JsonElement.str: String
    get() = asString

inline val JsonElement.bln: Boolean
    get() = asBoolean

inline val JsonElement.int: Int
    get() = asInt

inline val JsonElement.obj: JsonObject
    get() = asJsonObject

inline val JsonElement.arr: JsonArray
    get() = asJsonArray

inline val JsonElement.flt: Float
    get() = asFloat

inline val JsonElement.dbl: Double
    get() = asDouble


operator fun JsonObject.set(key: String, value: String) = addProperty(key, value)

operator fun JsonObject.set(key: String, value: Char) = addProperty(key, value)

operator fun JsonObject.set(key: String, value: Boolean) = addProperty(key, value)

operator fun JsonObject.set(key: String, value: Number) = addProperty(key, value)

operator fun JsonObject.set(key: String, value: JsonElement) = add(key, value)

operator fun JsonElement.get(vararg keys: String): JsonElement {
    var o = this
    keys.forEach { o = o.obj[it] }
    return o
}

operator fun JsonObject.contains(key: String): Boolean = has(key)

val jsonArrayCollector: Collector<String, JsonArray, JsonArray> = Collector.of<String, JsonArray>(
        Supplier(::JsonArray),
        BiConsumer(JsonArray::add), BinaryOperator { l, r ->
    l.addAll(r)
    l
})