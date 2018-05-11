package com.gt22.web.servlets

import com.google.common.hash.Hashing
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.gt22.web.DatabaseConnector
import com.gt22.web.utlis.*
import java.nio.charset.StandardCharsets
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.*
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet("/quoter", "/Quoter.php")
class QuoterServlet : HttpServlet() {
    private val base = DatabaseConnector.connect()
    private val posStm: PreparedStatement
    private val totalStm: PreparedStatement
    private val rangeStm: PreparedStatement
    private val addQuoteStm: PreparedStatement
    init {
        posStm = base.prepareStatement("SELECT * FROM `quoter` WHERE `id` = ?")
        totalStm = base.prepareStatement("SELECT COUNT(*) FROM `quoter`")
        rangeStm = base.prepareStatement("SELECT * FROM `quoter` WHERE `id` > ? AND `id` < ?")
        addQuoteStm = base.prepareStatement("INSERT INTO `quoter` (`adder`, `author`, `quote`) VALUES (?,?,?)")
    }

    override fun doPost(req: HttpServletRequest, res: HttpServletResponse) {
        res.prepare()
        val params = req.params()
        val r = try {
            when (params["task"]) {
                "GET" -> get(params)
                "ADD" -> add(params)
                null -> rep("TASK_NOT_SET")
                else -> rep("INVALID_TASK")
            }
        } catch(e: SQLException) {
            rep("Something went wrong! ${e.localizedMessage}")
        } catch (e: ParamNotSetException) {
            rep(e.message!!)
        }
        res.writer.println(r)
    }

    private fun buildQuote(id: Int, author: String, adder: String, quote: String): JsonObject {
        val ret = JsonObject()
        ret["id"] = id
        ret["author"] = author
        ret["adder"] = adder
        ret["quote"] = quote
        return ret
    }

    fun get(params: Map<String, String>): JsonObject {
        checkIsSet(params, "mode")
        return when(params["mode"]) {
            "pos" -> {
               checkIsSet(params, "pos")
                getPos(params["pos"]!!.toInt())
            }
            "fromto" -> {
                checkIsSet(params, "from", "to")
                getRange(params["from"]!!.toInt(), params["to"]!!.toInt())
            }
            "rand" -> getRand()
            "total" -> {
                val ret = JsonObject()
                ret["error"] = false
                ret["message"] = "success"
                ret["count"] = getQuoteCount()
                ret
            }
            null -> rep("MODE_NOT_SET")
            else -> rep("INVALID_MODE")
        }
    }

    private fun getPos(pos: Int): JsonObject {
        posStm.setInt(1, pos)
        val res = posStm.executeQuery()
        return with(res) {
            if (next()) {
                buildQuote(getInt("id"), getString("author"),
                        getString("adder"), getString("quote"))
            } else {
                rep("QUOTE_NOT_FOUND")
            }
        }
    }

    private fun getQuoteCount(): Int {
        val res = totalStm.executeQuery()
        res.next()
        return res.getInt(1)
    }

    private fun getRange(from: Int, to: Int): JsonObject {
        rangeStm.setInt(1, from)
        rangeStm.setInt(2, to)
        val res = rangeStm.executeQuery()
        val ret = JsonObject()
        val quotes = JsonArray()
        ret["quotes"] = quotes
        with(res) {
            while(next()) {
                quotes.add(buildQuote(getInt("id"), getString("author"),
                        getString("adder"), getString("quote")))
            }
        }
        return ret
    }

    private fun getRand(): JsonObject {
        val count = getQuoteCount()
        val pos = Random().nextInt(count) + 1
        return getPos(pos)
    }

    private fun add(params: Map<String, String>): JsonObject {
        checkIsSet(params, "key", "adder", "author", "quote")
        if(isKeyValid(params["key"]!!)) {
            addQuoteStm.setString(1, params["adder"]!!)
            addQuoteStm.setString(2, params["author"]!!)
            addQuoteStm.setString(3, params["quote"]!!)
            addQuoteStm.executeUpdate()
            return rep("Added quote", false)
        }
        return rep("KEY_NOT_VALID")
    }

    private fun isKeyValid(key: String): Boolean {
        return Hashing.sha256().hashString(key, StandardCharsets.UTF_8).toString() ==
                "bf077926f1f26e2e3552001461c1e51ec078c7d488f1519bd570cc86f0efeb1a"
    }

}