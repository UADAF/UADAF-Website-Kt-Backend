package com.gt22.web.servlets

import com.google.common.hash.Hashing
import com.google.gson.JsonObject
import com.gt22.randomutils.Instances
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
            checkIsSet(params, "task")
            when (params["task"]!!) {
                "GET" -> get(params)
                "ADD" -> add(params)
                "EDIT" -> edit(params)
                else -> rep("INVALID_TASK")
            }
        } catch(e: SQLException) {
            rep("Something went wrong! ${e.localizedMessage}")
        } catch (e: ParamNotSetException) {
            rep(e.message!!)
        }

        res.writer.println(formatResponse(r))
    }



    private fun buildQuote(row: ResultRow) = with(Quoter) {
        with(row) {
            json {
                "id" to get(id)
                "author" to get(author)
                "adder" to get(adder)
                "quote" to get(quote)
                "edited_by" to (get(editedBy) ?: "null")
                "edited_at" to (get(editedAt) ?: -1)
            }
        }
    }

    fun get(params: Map<String, String>) = transaction {
        checkIsSet(params, "mode")
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
                json {
                    "error" to false
                    "message" to "success"
                    "count" to getQuoteCount()
                }
            }
            null -> rep("MODE_NOT_SET")
            else -> rep("INVALID_MODE")
        }
    }

    private fun getPos(pos: Int) = with(Quoter) {
        buildQuote(select { id eq pos }.firstOrNull() ?: return rep("QUOTE_NOT_FOUND"))
    }

    private fun getQuoteCount() = Quoter.selectAll().count()

    private fun getRange(from: Int, to: Int) = json {
        "quotes" to Quoter.select { (id greater from) and (id less to) }.asSequence().map(::buildQuote)
    }

    private fun getRand() = getPos(Instances.getRand().nextInt(getQuoteCount()) + 1)

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

    private fun isKeyValid(key: String) =
            Hashing.sha256().hashString(key, StandardCharsets.UTF_8).toString() ==
                    "bf077926f1f26e2e3552001461c1e51ec078c7d488f1519bd570cc86f0efeb1a"

}