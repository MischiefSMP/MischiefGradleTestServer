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
        private fun getPaperBuildsJSON(version: String) = "https://papermc.io/api/v2/projects/paper/versions/$version/builds"
        private fun getPaperJarURL(version: String, build: String) = "https://api.papermc.io/v2/projects/paper/versions/$version/builds/$build/downloads/paper-$version-$build.jar"


        fun downloadPaper(serverVersion: String, serverFolder: File) {
            Files.copy(URL(getLatestPaperURL(serverVersion)).openStream(), File(serverFolder, "paper.jar").toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        private fun getLatestPaperURL(version: String): String {
            JSONObject(getTextFromURL(getPaperBuildsJSON(version))).getJSONArray("builds").also { json ->
                json.getJSONObject(json.length() - 1).also { build ->
                    return getPaperJarURL(version, build.getInt("build").toString())
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