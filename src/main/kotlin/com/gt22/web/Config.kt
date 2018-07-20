package com.gt22.web

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gt22.uadam.utils.obj
import java.io.InputStreamReader

val config: JsonObject = JsonParser().parse(InputStreamReader(Unit.javaClass.getResourceAsStream("/conf/config.json"))).obj