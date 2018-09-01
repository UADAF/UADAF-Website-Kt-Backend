package com.gt22.web.servlets

import com.google.gson.JsonObject
import com.gt22.uadam.utils.set
import com.gt22.web.DatabaseConnector
import com.gt22.web.Users
import com.gt22.web.utlis.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.sql.PreparedStatement
import java.sql.SQLException
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet("/ith", "/ITH.php")
class ITHServlet : HttpServlet() {

    override fun doGet(req: HttpServletRequest, res: HttpServletResponse) {
        doPost(req, res)
    }

    override fun doPost(req: HttpServletRequest, res: HttpServletResponse) {
        res.prepare()
        DatabaseConnector.init()
        val params = req.params()
        val r = try {
            checkIsSet(params, "task")
            transaction {
                when (params["task"]!!) {
                    "login" -> login(params)
                    "setStory" -> setStory(params)
                    else -> rep("INVALID_TASK")
                }
            }
        } catch (e: SQLException) {
            rep("Something went wrong ${e.localizedMessage}")
        } catch (e: ParamNotSetException) {
            rep(e.localizedMessage)
        }

        res.writer.print(formatResponse(r))
    }

    private fun getStory(name: String): Int? {
        return with(Users) { slice(story).select { user like name }.firstOrNull()?.get(story) }
    }

    private fun login(params: Map<String, String>): JsonObject {
        checkIsSet(params, "name")
        val username = params["name"] ?: return rep("NAME_NOT_SET")
        var storyId = getStory(username)
        if (storyId == null) {
            storyId = 1
            Users.insert {
                it[user] = username
                it[story] = 1
            }
        }
        val ret = JsonObject()
        ret["isLogged"] = true
        ret["user"] = username
        ret["story"] = storyId
        ret["storyName"] = "Please wait"
        ret["storyContent"] = "Please wait"
        return ret
    }

    private fun setStory(params: Map<String, String>): JsonObject {
        checkIsSet(params, "name", "story")
        val username = params["name"]!!
        val storyId = params["story"]!!.toInt()
        with(Users) {
            update({ user eq username }) {
                it[story] = storyId
            }
        }
        return rep("Success $username:$storyId", false)
    }

}