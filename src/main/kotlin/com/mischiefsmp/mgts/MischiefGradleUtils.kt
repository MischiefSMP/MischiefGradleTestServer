package com.mischiefsmp.mgts


import org.json.JSONObject
import java.io.File
import java.lang.StringBuilder
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Scanner

class MischiefGradleUtils {
    companion object {
        fun downloadPaper(serverVersion: String, serverFolder: File) {
            val paperURL = MischiefGradleUtils.getLatestPaperURL(serverVersion)
            Files.copy(URL(paperURL).openStream(), File(serverFolder, "paper.jar").toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        fun getLatestPaperURL(version: String): String {
            JSONObject(getTextFromURL(Constants.PAPER_BUILDS(version))).getJSONArray("builds").also { json ->
                json.getJSONObject(json.length() - 1).also { build ->
                    return Constants.PAPER_DL(version, build.getInt("build").toString())
                }
            }
        }

        private fun getTextFromURL(url: String): String {
            Scanner(URL(url).openStream()).also {
                StringBuilder().run {
                    while(it.hasNext()) append(it.next())
                    return toString()
                }
            }
        }
    }
}

class Constants {
    companion object {
        val PAPER_MAVEM_REPO = "https://repo.papermc.io/repository/maven-public/"
        fun PAPER_API_ID(version: String) = "io.papermc.paper:paper-api:$version"

        fun PAPER_BUILDS(version: String) = "https://papermc.io/api/v2/projects/paper/versions/$version/builds"
        fun PAPER_DL(version: String, build: String) = "https://api.papermc.io/v2/projects/paper/versions/$version/builds/$build/downloads/paper-$version-$build.jar"
    }
}