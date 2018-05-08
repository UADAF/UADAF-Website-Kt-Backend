package com.gt22.web.servlets

import com.google.gson.JsonObject
import com.gt22.web.DatabaseConnector
import com.gt22.web.utlis.*
import java.sql.PreparedStatement
import java.sql.SQLException
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet("/ith", "/ITH.php")
class ITHServlet : HttpServlet() {
    private val base = DatabaseConnector.connect()
    private val initUserSmt: PreparedStatement
    private val getUserStorySmt: PreparedStatement
    private val setUserStorySmt: PreparedStatement

    init {
        initUserSmt = base.prepareStatement("INSERT INTO `users` (`user`, `story`) VALUES (?, 1)")
        getUserStorySmt = base.prepareStatement("SELECT `story` FROM `users` WHERE `user` = ?")
        setUserStorySmt = base.prepareStatement("UPDATE `users` SET `story`= ? WHERE `user` = ?")
    }

    override fun doPost(req: HttpServletRequest, res: HttpServletResponse) {
        res.prepare()
        val params = req.params()
        checkIsSet(params, "task")
        val reply = try {
            when(params["task"]!!) {
                "login" -> login(params)
                "setStory" -> setStory(params)
                else -> rep("INVALID_TASK")
            }
        } catch (e: SQLException) {
            rep("Something went wrong ${e.localizedMessage}")
        } catch (e: ParamNotSetException) {
            e.message
        }

        res.writer.print(reply)
    }

    private fun login(params: Map<String, String>): JsonObject {
        checkIsSet(params, "name")
        val user = params["name"] ?: return rep("NAME_NOT_SET")
        getUserStorySmt.setString(1, user)
        val storyRes = getUserStorySmt.executeQuery()
        val stroy = if(storyRes.next()) {
            storyRes.getInt("story")
        } else {
            initUserSmt.setString(1, user)
            initUserSmt.executeUpdate()
            1
        }
        val ret = JsonObject()
        ret["isLogged"] = true
        ret["user"] = user
        ret["story"] = stroy
        ret["storyName"] = "Please wait"
        ret["storyContent"] = "Please wait"
        return ret
    }

    private fun setStory(params: Map<String, String>): JsonObject {
        checkIsSet(params, "name", "story")
        val user = params["name"]!!
        val story = params["story"]!!.toInt()
        setUserStorySmt.setInt(1, story)
        setUserStorySmt.setString(2, user)
        setUserStorySmt.executeUpdate()
        return rep("Success $user:$story", false)
    }

}