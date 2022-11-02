package com.mischiefsmp.mgts

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import java.awt.Desktop
import java.io.File

abstract class ServerConfig {
    var folder = File(System.getProperty("user.home"), ".mischiefsmp/testserver/")
    var version = "none"
    var xms = "265m"
    var xmx = "2048m"
    var gui = false
    var port = "25565"
    var onlineMode = true
    var serverName = "MischiefGradleTestServer"
    var pluginDirs = ArrayList<File>()
    fun pluginDir(path: File) = pluginDirs.add(path)
}

class GradlePlugin: Plugin<Project> {
    override fun apply(project: Project) {
        val gradleGroup = "Mischief Test Server"
        val config = project.extensions.create("TestServerConfig", ServerConfig::class.java)
        val logger = project.logger

        val taskDownload = project.task("testServerDownload") { task ->
            task.group = gradleGroup
            task.description = "Refreshes the server jar"
            task.doLast {
                if(config.version == "none") throw GradleException("serverVersion is not set!")
                config.folder.mkdirs()
                logger.lifecycle("Grabbing Paper jar for ${config.version}")
                MischiefGradleUtils.downloadPaper(config.version, config.folder)
            }
        }

        project.task("testServerOpen") { task ->
            task.group = gradleGroup
            task.description = "Opens the test server folder"
            task.doLast {
                config.folder.also { folder ->
                    println("Server folder: $folder")
                    if(!folder.exists()) folder.mkdirs()
                    Desktop.getDesktop().open(folder)
                }
            }
        }

        fun acceptEula() {
            File(config.folder, "eula.txt").also {file ->
                file.delete()
                file.writeText("eula=true")
            }
        }

        project.task("testServerClean") { task ->
            task.group = gradleGroup
            task.description = "Deletes the server directory"
            task.doLast {
                if(!config.folder.deleteRecursively())
                    throw GradleException("Could not be cleaned! Are any servers still running?")
            }
        }

        val taskCopyPlugins = project.task("testServerCopyPlugins").run {
            group = gradleGroup
            doFirst {
                config.pluginDirs.forEach { file ->
                    logger.lifecycle("Copying $it -> ${config.folder}")
                    file.copyRecursively(File(config.folder, "plugins"))
                }
            }
        }

        project.task("testServerCleanPlugins").run {
            group = gradleGroup
            doLast {
                File(config.folder, "plugins").deleteRecursively()
            }
        }

        project.task(mapOf(Pair("type", JavaExec::class.java)), "testServerLaunch").run {
            dependsOn(taskDownload, taskCopyPlugins)
            group = gradleGroup
            doFirst {
                acceptEula()
                this as JavaExec
                workingDir = config.folder //Set work dir to server folder
                classpath(File(config.folder, "paper.jar").absolutePath) //Jar path
                standardInput = System.`in` //This allows input in our IDE

                //Add args
                if(!config.gui) args?.add("--nogui")
                args?.add("-Xms${config.xms}")
                args?.add("-Xmx${config.xmx}")
                args?.add("--port ${config.port}")
                args?.add("--online-mode ${config.onlineMode}")
                args?.add("--server-name ${config.serverName}")
            }
        }
    }
}