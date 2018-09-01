package com.gt22.web.servlets

import com.google.common.hash.Hashing
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.gt22.randomutils.Instances
import com.gt22.uadam.utils.set
import com.gt22.web.DatabaseConnector
import com.gt22.web.Quoter
import com.gt22.web.Quoter.id
import com.gt22.web.utlis.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.charset.StandardCharsets
import java.sql.SQLException
import java.util.Date
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet("/quoter", "/Quoter.php")
class QuoterServlet : HttpServlet() {

    override fun doPost(req: HttpServletRequest, res: HttpServletResponse) {
        res.prepare()
        DatabaseConnector.init()
        val params = req.params()
        val r = try {
            when (params["task"]) {
                "GET" -> get(params)
                "ADD" -> add(params)
                "EDIT" -> edit(params)
                null -> rep("TASK_NOT_SET")
                else -> rep("INVALID_TASK")
            }
        } catch(e: SQLException) {
            rep("Something went wrong! ${e.localizedMessage}")
        } catch (e: ParamNotSetException) {
            rep(e.message!!)
        }

        res.writer.println(formatResponse(r))
    }

    private fun buildQuote(row: ResultRow): JsonObject {
        return with(Quoter) {
            with(row) {
                val ret = JsonObject()
                ret["id"] = get(id)
                ret["author"] = get(author)
                ret["adder"] = get(adder)
                ret["quote"] = get(quote)
                ret["edited_by"] = get(editedBy) ?: "null"
                ret["edited_at"] = get(editedAt) ?: -1
                ret
            }
        }
    }

    fun get(params: Map<String, String>): JsonObject {
        checkIsSet(params, "mode")
        return transaction {
            when (params["mode"]) {
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
    }

    private fun getPos(pos: Int): JsonObject {
        return with(Quoter) {
            buildQuote(select { id eq pos }.firstOrNull() ?: return rep("QUOTE_NOT_FOUND"))
        }
    }

    private fun getQuoteCount(): Int {
        return Quoter.selectAll().count()
    }

    private fun getRange(from: Int, to: Int): JsonObject {
        val ret = JsonObject()
        val quotes = JsonArray()
        ret["quotes"] = quotes
        Quoter.select { (id greater from) and (id less to) }
                .asSequence().map(::buildQuote).forEach(quotes::add)
        return ret
    }

    private fun getRand(): JsonObject {
        val count = getQuoteCount()
        val pos = Instances.getRand().nextInt(count) + 1
        return getPos(pos)
    }

    private fun add(params: Map<String, String>): JsonObject {
        checkIsSet(params, "key", "adder", "author", "quote")
        return if(isKeyValid(params["key"]!!)) {
            Quoter.insert {
                it[adder] = params["adder"]!!
                it[author] = params["author"]!!
                it[quote] = params["quote"]!!
            }
            rep("Added quote", false)
        } else {
            rep("KEY_NOT_VALID")
        }
    }

    private fun edit(params: Map<String, String>): JsonObject {
        checkIsSet(params, "id", "edited_by", "new_text", "key")
        return if(isKeyValid(params["key"]!!)) {
            val idN = params["id"]!!.toIntOrNull() ?: return rep("INVALID_ID")
            transaction {
                val updated = Quoter.update({ id eq idN }) {
                    it[quote] = params["new_text"]!!
                    it[editedBy] = params["edited_by"]
                    it[editedAt] = Date().time
                }
                if (updated == 0) {
                    rep("QUOTE_NOT_FOUND")
                } else {
                    rep("Quote has been edited", false)
                }
            }
        } else {
            rep("KEY_NOT_VALID")
        }
    }

    private fun isKeyValid(key: String): Boolean {
        return Hashing.sha256().hashString(key, StandardCharsets.UTF_8).toString() ==
                "bf077926f1f26e2e3552001461c1e51ec078c7d488f1519bd570cc86f0efeb1a"
    }

}