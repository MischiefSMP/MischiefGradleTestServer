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
    var pluginDirs = ArrayList<File>()
    fun pluginDir(path: File) = pluginDirs.add(path)
}

class GradlePlugin: Plugin<Project> {
    override fun apply(project: Project) {
        val gradleGroup = "Mischief Test Server"
        val config = project.extensions.create("TestServerConfig", ServerConfig::class.java)
        val logger = project.logger

        val pluginsDir = File(config.folder, "plugins")
        val paperJar = File(config.folder, "paper.jar")
        val eulaTxt = File(config.folder, "eula.txt")

        val taskDownload = project.task("testServerDownload") { task ->
            task.group = gradleGroup
            task.description = "Refreshes the server jar"
            task.doLast {
                if(paperJar.exists()) return@doLast

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
            val acceptedEulaAlready = eulaTxt.exists() && eulaTxt.readText().contains("eula=true")
            if(!acceptedEulaAlready) {
                logger.lifecycle("You currently have not accepted the eula.")
                logger.lifecycle("Please read it at https://www.minecraft.net/eula")
                logger.lifecycle("Accept?: y/n")
                when(readLine()) {
                    "y" -> {
                        eulaTxt.delete()
                        eulaTxt.writeText("eula=true")
                    }
                    else -> throw GradleException("You have not accepted the eula. Server cannot start.")
                }
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
                    logger.lifecycle("Copying $file -> ${config.folder}")
                    file.copyRecursively(pluginsDir, true)
                }
            }
        }

        project.task("testServerCleanPlugins").run {
            group = gradleGroup
            doLast {
                pluginsDir.deleteRecursively()
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

                //Heap size as configured
                minHeapSize = config.xms
                maxHeapSize = config.xmx

                //nogui flag and port, online mode cant be set via the command line. Has to be in server.properties
                ArrayList<String>().also { mcArgs ->
                    if(!config.gui) mcArgs.add("-nogui")
                    mcArgs.add("-p${config.port}")
                    args = mcArgs
                }
            }
        }
    }
}