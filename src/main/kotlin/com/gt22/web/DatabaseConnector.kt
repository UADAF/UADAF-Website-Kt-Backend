package com.gt22.web

import com.google.gson.JsonElement
import com.gt22.uadam.utils.get
import com.gt22.uadam.utils.str
import com.mysql.jdbc.NonRegisteringDriver
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.sql.DriverManager

object DatabaseConnector {

    private val cfg: JsonElement = config["database"]
    private val connectUrl = "jdbc:mysql://${cfg["host"]!!.str}:3306/${cfg["db_name"]!!.str}?useUnicode=yes&characterEncoding=UTF-8"

    init {
        Database.connect(connectUrl, "com.mysql.jdbc.Driver", cfg["login"]!!.str, cfg["password"]!!.str)
    }

    fun init() {
        //Used to load object
    }
}