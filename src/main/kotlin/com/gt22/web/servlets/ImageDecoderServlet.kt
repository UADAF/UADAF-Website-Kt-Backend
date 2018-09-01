package com.gt22.web.servlets

import com.gt22.web.DatabaseConnector
import com.gt22.web.Pictures
import com.gt22.web.utlis.*
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import sun.misc.BASE64Decoder
import java.io.ByteArrayInputStream
import java.net.URLDecoder
import java.sql.SQLException
import javax.imageio.ImageIO
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet("/imagedecoder", "/imagedecoder.php")
class ImageDecoderServlet : HttpServlet() {

    override fun doGet(req: HttpServletRequest, res: HttpServletResponse) {
        doPost(req, res)
    }

    override fun doPost(req: HttpServletRequest, res: HttpServletResponse) {
        val params = req.params()
        DatabaseConnector.init()
        val rep = try {
            checkIsSet(params, "name", "mode")
            val mode = params["mode"]!!
            if (mode != "get") {
                res.prepare() //Only forbid cache and set encoding if do not wont to send an image
            }
            transaction {
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
        Pictures.insert {
            it[name] = params["name"]!!
            it[base64] = img
        }
        return "Success"
    }

    private fun remove(params: Map<String, String>): String {
        with(Pictures) {
            deleteWhere { name eq params["name"]!! }
        }
        return "Success"
    }

    private fun get(params: Map<String, String>, res: HttpServletResponse) {
        with(Pictures) {
            val result = slice(base64).select { name eq params["name"]!! }.firstOrNull() ?: return res.writer.print("Not found")
            val b64 = result[base64]
            val dec = BASE64Decoder()
            val img = dec.decodeBuffer(b64)
            val bufImg = ImageIO.read(ByteArrayInputStream(img))
            res.contentType = "image/png"
            ImageIO.write(bufImg, "png", res.outputStream)
        }
    }

    private fun check(params: Map<String, String>): String {
        with(Pictures) {
            return if(select { name eq params["name"]!! }.count() == 0) "false" else "true"
        }
    }
}