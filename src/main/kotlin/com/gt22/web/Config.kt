package com.gt22.web

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gt22.randomutils.Instances
import com.gt22.uadam.utils.obj
import java.io.InputStreamReader

val config: JsonObject = Instances.getParser().parse(InputStreamReader(Unit.javaClass.getResourceAsStream("/conf/config.json"))).obj