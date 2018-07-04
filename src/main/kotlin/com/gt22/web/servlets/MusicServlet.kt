package com.gt22.web.servlets

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.gt22.web.utlis.*
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet("/music")
class MusicServlet : HttpServlet() {

    private val musicDir = Paths.get(com.gt22.web.config["music", "root"].str)
    private val str = JsonElement::str

    override fun doGet(req: HttpServletRequest, res: HttpServletResponse) {
        val rep = process(req, res)
        val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
        res.writer.print(gson.toJson(rep).replace("\n", "<br>").replace("  ", "&nbsp;&nbsp;"))
    }


    override fun doPost(req: HttpServletRequest, res: HttpServletResponse) {
        val process = process(req, res)
        res.writer.print(process)
    }

    private fun process(req: HttpServletRequest, res: HttpServletResponse): JsonElement {
        res.prepare()
        val rep = try {
            val params = req.params()
            val useFullPath = "useFullPath" in params
            if ("author" in params) {
                if ("album" in params) {
                    getSongs(params["author"]!!, params["album"]!!, useFullPath)
                } else {
                    getAuthorSongs(params["author"]!!, useFullPath)
                }

            } else {
                if ("get" in params) {
                    when (params["get"]!!) {
                        "authors" -> getAuthors()
                        "albums" -> getAllAlbums()
                        "songs" -> getAllSongs(useFullPath)
                        else -> rep("Invalid get request")
                    }
                } else {
                    getAll(useFullPath)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            rep("${e.message!!.replace(musicDir.toString(), "")} not found")
        }
        return formatResponse(rep)
    }

    private fun getAuthors(): JsonArray {
        return listDirToArray(musicDir, false)
    }

    private fun getAlbums(author: String): JsonArray {
        return listDirToArray(musicDir.resolve(author), false) { Files.isDirectory(it) }
    }

    private fun getAllAlbums(): JsonArray {
        val ret = JsonArray()
        getAuthors().forEach(str) { author ->
            getAlbums(author).forEach(str) { album ->
                ret.add("$author/$album")
            }
        }
        return ret
    }

    private fun getSongs(author: String, album: String, useFullPath: Boolean): JsonArray {
        return listDirToArray(musicDir.resolve(if (album == "") author else "$author/$album"), useFullPath) { !Files.isDirectory(it) }
    }

    private fun getAuthorSongs(author: String, useFullPath: Boolean): JsonObject {
        val ret = JsonObject()
        val albumsObj = JsonObject()
        getAlbums(author).forEach(str) { albumsObj[it] = getSongs(author, it, useFullPath) }
        ret["albums"] = albumsObj
        ret["root"] = getSongs(author, "", useFullPath)
        return ret
    }

    private fun getAllSongs(useFullPath: Boolean): JsonArray {
        val ret = JsonArray()
        getAuthors().forEach(str) { author ->
            getAuthorSongs(author, useFullPath).entrySet().forEach { (album, songs) ->
                songs.arr.map { song ->
                    if (album != "root") {
                        "$author/$album/${song.str}"
                    } else {
                        "$author/${song.str}"
                    }
                }.forEach(ret::add)
            }
        }
        return ret
    }

    private fun getAll(useFullPath: Boolean): JsonObject {
        val ret = JsonObject()
        getAuthors().forEach(str) { author ->
            ret[author] = getAuthorSongs(author, useFullPath)
        }
        return ret
    }

    private fun listDirToArray(dir: Path, useFullPath: Boolean, filter: (Path) -> Boolean = { true }): JsonArray {
        return Files.list(dir).use {
            it.filter(filter)
                    .map { if (useFullPath) musicDir.relativize(it) else it.fileName }
                    .map(Path::toString)
                    .sorted()
                    .collect(jsonArrayCollector)
        }
    }

}