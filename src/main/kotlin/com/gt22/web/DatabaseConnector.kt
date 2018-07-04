package com.gt22.web

import com.google.gson.JsonElement
import com.gt22.web.utlis.get
import com.gt22.web.utlis.str
import com.mysql.jdbc.NonRegisteringDriver
import java.sql.Connection
import java.sql.DriverManager

object DatabaseConnector {

    private val cfg: JsonElement = config["database"]
    private val connectUrl = "jdbc:mysql://${cfg["host"].str}:3306/${cfg["db_name"].str}?useUnicode=yes&characterEncoding=UTF-8"
    private val connection: Connection
    init {
        DriverManager.registerDriver(NonRegisteringDriver())
        connection = DriverManager.getConnection(connectUrl, cfg["login"].str, cfg["password"].str)
    }


    fun connect(): Connection {
        return connection
    }
}