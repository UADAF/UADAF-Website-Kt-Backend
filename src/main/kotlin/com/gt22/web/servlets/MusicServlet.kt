package com.gt22.web.servlets

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.gt22.uadam.data.*
import com.gt22.uadam.utils.get
import com.gt22.uadam.utils.str
import com.gt22.web.utlis.json
import com.gt22.web.utlis.prepare
import java.nio.file.Paths
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet("/music")
class MusicServlet : HttpServlet() {

    private val context = MusicContext.create(Paths.get(com.gt22.web.config["music", "root"]!!.str))

    override fun doGet(req: HttpServletRequest, res: HttpServletResponse) {
        val rep = process(res)
        val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
        res.writer.print(gson.toJson(rep))
    }


    override fun doPost(req: HttpServletRequest, res: HttpServletResponse) {
        val process = process(res)
        res.writer.print(process)
    }

    private fun process(res: HttpServletResponse): JsonElement {
        res.prepare()
        return jsonify(context)
    }

    fun jsonify(data: BaseData): JsonElement = json {
        "meta" to getMeta(data)
        if(data is Album) {
            "children" to data.children.values.asSequence().map(BaseData::name).map(::JsonPrimitive)
        } else {
            "children" to {
                data.children.forEach { name, value -> name to jsonify(value) }
            }
        }
    }

    private fun type(data: BaseData): String = when (data) {
        is Song -> "song"
        is Album -> "album"
        is Author -> "author"
        is Group -> "group"
        is MusicContext -> "context"
        else -> "Unknown"
    }

    private fun getMeta(data: BaseData) = json {
        if (data.title != data.name) "title" to data.title
        if (data.format != data.parent?.format) "format" to data.format
        if (data.img != data.parent?.img) "img" to data.img
    }

}