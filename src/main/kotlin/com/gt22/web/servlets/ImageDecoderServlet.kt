package com.gt22.web.servlets

import com.gt22.web.DatabaseConnector
import com.gt22.web.utlis.*
import sun.misc.BASE64Decoder
import java.io.ByteArrayInputStream
import java.net.URLDecoder
import java.sql.PreparedStatement
import java.sql.SQLException
import javax.imageio.ImageIO
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet("/imagedecoder", "/imagedecoder.php")
class ImageDecoderServlet : HttpServlet() {

    private val base = DatabaseConnector.connect()
    private val addStm: PreparedStatement
    private val removeStm: PreparedStatement
    private val getStm: PreparedStatement
    private val checkStm: PreparedStatement
    init {
        ImageIO.setUseCache(false)
        addStm = base.prepareStatement("INSERT INTO `pictures`(`name`, `base64`) VALUES (?, ?)")
        removeStm = base.prepareStatement("DELETE FROM `pictures` WHERE `name`= ?")
        getStm = base.prepareStatement("SELECT `base64` FROM `pictures` WHERE `name` = ?")
        checkStm = base.prepareStatement("SELECT COUNT(*) AS `count` FROM `pictures` WHERE `name` = ?")
    }

    override fun doPost(req: HttpServletRequest, res: HttpServletResponse) {
        val params = req.params()
        val rep = try {
            checkIsSet(params, "name", "mode")
            val mode = params["mode"]!!
            if (mode != "get") {
                res.prepare() //Only forbid cache and set encoding if do not wont to send an image
            }
            when (params["mode"]!!) {
                "add" -> add(params)
                "remove" -> remove(params)
                "check" -> check(params)
                "get" -> {
                    get(params, res)
                    null
                }
                else -> "INVALID_MODE"
            }
        } catch (e: SQLException) {
            rep("Something went wrong ${e.localizedMessage}")
        } catch (e: ParamNotSetException) {
            e.message
        }
        rep?.let { res.writer.print(it) }
    }


    private fun add(params: Map<String, String>): String {
        checkIsSet(params, "img")
        val img = URLDecoder.decode(params["img"]!!, "UTF-8").replace(' ', '+')
        addStm.setString(1, params["name"]!!)
        addStm.setString(2, img)
        addStm.executeUpdate()
        return "Success"
    }

    private fun remove(params: Map<String, String>): String {
        removeStm.setString(1, params["name"]!!)
        removeStm.executeUpdate()
        return "Success"
    }

    private fun get(params: Map<String, String>, res: HttpServletResponse) {
        getStm.setString(1, params["name"]!!)
        val result = getStm.executeQuery()
        if(!result.next()) {
            res.writer.print("Not found")
            return
        }
        val b64 = result.getString(1)
        val dec = BASE64Decoder()
        val img = dec.decodeBuffer(b64)
        val bufImg = ImageIO.read(ByteArrayInputStream(img))
        res.contentType = "image/png"
        ImageIO.write(bufImg, "png", res.outputStream)
    }

    private fun check(params: Map<String, String>): String {
        checkStm.setString(1, params["name"]!!)
        val res = checkStm.executeQuery()
        res.next()
        return if(res.getInt(1) == 0) "false" else "true"
    }
}