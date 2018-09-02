package com.gt22.web

import com.google.gson.JsonElement
import com.gt22.uadam.utils.get
import com.gt22.uadam.utils.str
import org.jetbrains.exposed.sql.Database

object DatabaseConnector {

    private val cfg: JsonElement = config["database"]
    private val connectUrl = "jdbc:mysql://${cfg["host"]!!.str}:3306/${cfg["db_name"]!!.str}?useUnicode=yes&characterEncoding=UTF-8&autoReconnect=true"
    init {
        Database.connect(connectUrl, "com.mysql.jdbc.Driver", cfg["login"]!!.str, cfg["password"]!!.str)
    }

    fun init() {
        //Used to load object
    }
}