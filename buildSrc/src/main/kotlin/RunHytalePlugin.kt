import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.io.InputStream
import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Custom Gradle plugin for automated Hytale server testing.
 * 
 * Usage:
 *   runHytale {
 *       jarUrl = "https://example.com/hytale-server.jar"
 *       assetsPath = "Assets.zip"
 *   }
 *   
 *   ./gradlew runServer
 */
open class RunHytalePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("runHytale", RunHytaleExtension::class.java)

        val runTask = project.tasks.register("runServer", RunServerTask::class.java) {
            jarUrl.set(extension.jarUrl)
            extension.assetsPath?.let { assetsPath.set(it) }
            group = "hytale"
            description = "Downloads and runs the Hytale server with your plugin"
        }

        val prepareTask = project.tasks.register("prepareServer", PrepareServerTask::class.java) {
            jarUrl.set(extension.jarUrl)
            extension.assetsPath?.let { assetsPath.set(it) }
            group = "hytale"
            description = "Prepares the Hytale server files and mods without starting"
        }

        project.tasks.findByName("shadowJar")?.let {
            runTask.configure { dependsOn(it) }
            prepareTask.configure { dependsOn(it) }
        }
    }
}

/**
 * Extension for configuring the RunHytale plugin.
 */
open class RunHytaleExtension {
    var jarUrl: String = "https://example.com/hytale-server.jar"
    var assetsPath: String? = null
}

/**
 * Base task that prepares server files and mods.
 */
open class BaseServerTask : DefaultTask() {

    @Input
    val jarUrl = project.objects.property(String::class.java)
    
    @Input
    val assetsPath = project.objects.property(String::class.java)

    protected fun execute(startServer: Boolean) {
        // Create directories
        val runDir = File(project.projectDir, "run").apply { mkdirs() }
        val jarFile = File(runDir, "server.jar")
        val pidFile = File(runDir, "server.pid")

        // Cache directory for downloaded server JARs
        val cacheDir = File(
            project.layout.buildDirectory.asFile.get(), 
            "hytale-cache"
        ).apply { mkdirs() }

        // If HYTALE_SERVER_DOWNLOAD_URL is set, download the zip into .hytale/server and use it as the cached server package.
        ensureServerZipFromDownloadUrl()

        // Prefer a local server package zip if it exists (e.g. in .hytale/server).
        val localServerZip = findLatestServerZip()
        val serverJarSource: File
        val assetsSourceFromZip: File?
        val workingDir: File
        val modsDir: File
        if (localServerZip != null) {
            val extractedDir = File(cacheDir, "server-zip-${localServerZip.nameWithoutExtension}")
            val markerFile = File(extractedDir, ".extracted")
            if (!markerFile.exists() || markerFile.lastModified() < localServerZip.lastModified()) {
                if (extractedDir.exists()) {
                    extractedDir.deleteRecursively()
                }
                extractedDir.mkdirs()
                extractZip(localServerZip, extractedDir)
                markerFile.writeText("ok")
            }
            val jarFromZip = findFileByPath(extractedDir, "Server/HytaleServer.jar")
                ?: findFileByName(extractedDir, "HytaleServer.jar")
            if (jarFromZip == null) {
                println("ERROR: HytaleServer.jar not found in ${localServerZip.absolutePath}")
                return
            }
            serverJarSource = jarFromZip
            assetsSourceFromZip = findFileByPath(extractedDir, "Assets.zip")
                ?: findFileByName(extractedDir, "Assets.zip")
            workingDir = extractedDir
            modsDir = File(extractedDir, "mods").apply { mkdirs() }
            println("Using local server package: ${localServerZip.absolutePath}")
        } else {
            // Normalize jarUrl to URI
            val jarUrlStr = jarUrl.get()
            val jarUri = when {
                jarUrlStr.startsWith("file://") -> URI.create(jarUrlStr)
                jarUrlStr.startsWith("http://") || jarUrlStr.startsWith("https://") -> URI.create(jarUrlStr)
                File(jarUrlStr).isAbsolute -> File(jarUrlStr).toURI()
                else -> File(project.projectDir, jarUrlStr).toURI()
            }

            // Compute hash of URI for caching
            val urlHash = MessageDigest.getInstance("SHA-256")
                .digest(jarUri.toString().toByteArray())
                .joinToString("") { "%02x".format(it) }
            val cachedJar = File(cacheDir, "$urlHash.jar")

            // Download server JAR if not cached
            if (!cachedJar.exists()) {
                println("Downloading Hytale server from ${jarUri}")
                try {
                    jarUri.toURL().openStream().use { input ->
                        cachedJar.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    println("Server JAR downloaded and cached")
                } catch (e: Exception) {
                    println("ERROR: Failed to download server JAR")
                    println("Make sure the jarUrl in build.gradle is correct")
                    println("Error: ${e.message}")
                    return
                }
            } else {
                println("Using cached server JAR")
            }
            serverJarSource = cachedJar
            assetsSourceFromZip = null
            workingDir = runDir
            modsDir = File(runDir, "mods").apply { mkdirs() }
        }

        // Copy server JAR to run directory only when needed.
        if (workingDir == runDir) {
            if (jarFile.exists()) {
                try {
                    jarFile.delete()
                } catch (_: Exception) {
                }
            }
            if (!jarFile.exists()) {
                serverJarSource.copyTo(jarFile, overwrite = true)
            } else {
                println("WARNING: server.jar is locked; using existing file.")
            }
        }

        // Copy plugin JAR and test mods.
        copyMods(project, modsDir)

        // Copy assets file (mandatory)
        val assetsPathStr = assetsPath.orNull
        val sourceAssets = when {
            !assetsPathStr.isNullOrBlank() && assetsPathStr.startsWith("file://") -> File(URI.create(assetsPathStr))
            !assetsPathStr.isNullOrBlank() && File(assetsPathStr).isAbsolute -> File(assetsPathStr)
            !assetsPathStr.isNullOrBlank() -> File(project.projectDir, assetsPathStr)
            else -> assetsSourceFromZip
        }
        if (sourceAssets == null || !sourceAssets.exists()) {
            throw IllegalStateException(
                "Assets file not found. Set runHytale.assetsPath or provide a server zip with Assets.zip."
            )
        }

        val assetsFile = File(workingDir, sourceAssets.name)
        if (!assetsFile.exists() || assetsFile.absolutePath != sourceAssets.absolutePath) {
            if (assetsFile.exists()) {
                try {
                    assetsFile.delete()
                } catch (_: Exception) {
                }
            }
            if (!assetsFile.exists()) {
                sourceAssets.copyTo(assetsFile, overwrite = true)
                println("Assets copied to: ${assetsFile.absolutePath}")
            } else {
                println("WARNING: Assets.zip is locked; using existing file at ${assetsFile.absolutePath}")
            }
        }

        if (!startServer) {
            println("Server prepared at ${workingDir.absolutePath}")
            return
        }

        println("Starting Hytale server...")
        println("Press Ctrl+C to stop the server")

        // Check if debug mode is enabled
        val debugMode = project.hasProperty("debug")
        val javaArgs = mutableListOf<String>()
        
        val startScript = findStartScript(workingDir)
        val serverArgs = listOf("--allow-op")
        val process = if (startScript != null) {
            if (debugMode) {
                println("Debug mode ignored when using start script.")
            }
            startScriptProcess(startScript, workingDir, serverArgs)
        } else {
            if (debugMode) {
                javaArgs.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005")
                println("Debug mode enabled. Connect debugger to port 5005")
            }
            val javaExe = resolveJavaExecutable()
            val serverJarArg = if (workingDir == runDir) {
                jarFile.name
            } else {
                serverJarSource.relativeTo(workingDir).invariantSeparatorsPath
            }
            val assetsArg = assetsFile.relativeTo(workingDir).invariantSeparatorsPath
            javaArgs.addAll(listOf("-jar", serverJarArg))
            javaArgs.add("--assets")
            javaArgs.add(assetsArg)
            // Allow operator login in test server runs.
            javaArgs.addAll(serverArgs)
            ProcessBuilder(javaExe, *javaArgs.toTypedArray())
                .directory(workingDir)
                .start()
        }
        try {
            pidFile.writeText(process.pid().toString())
        } catch (_: Exception) {
        }

        // Handle graceful shutdown
        project.gradle.buildFinished {
            if (process.isAlive) {
                println("\nStopping server...")
                process.destroy()
            }
            if (pidFile.exists()) {
                pidFile.delete()
            }
        }

        // Forward process streams to console
        forwardStream(process.inputStream) { println(it) }
        forwardStream(process.errorStream) { System.err.println(it) }
        
        // Forward stdin to server (for commands)
        Thread {
            System.`in`.bufferedReader().useLines { lines ->
                lines.forEach {
                    process.outputStream.write((it + "\n").toByteArray())
                    process.outputStream.flush()
                }
            }
        }.start()

        // Wait for server to exit
        val exitCode = process.waitFor()
        println("Server exited with code $exitCode")
        if (pidFile.exists()) {
            pidFile.delete()
        }
    }

    /**
     * Forwards an input stream to a consumer in a background thread.
     */
    private fun forwardStream(inputStream: InputStream, consumer: (String) -> Unit) {
        Thread {
            inputStream.bufferedReader().useLines { lines ->
                lines.forEach(consumer)
            }
        }.start()
    }

    /**
     * Copy the plugin jar and any test mods into a mods folder.
     */
    private fun copyMods(project: Project, modsDir: File) {
        project.tasks.findByName("shadowJar")?.outputs?.files?.firstOrNull()?.let { shadowJar ->
            deleteOldPluginJars(modsDir, project.rootProject.name)
            val targetFile = File(modsDir, shadowJar.name)
            if (targetFile.exists() && !targetFile.delete()) {
                throw IllegalStateException(
                    "Failed to delete existing plugin jar at ${targetFile.absolutePath}. Stop the server and retry."
                )
            }
            shadowJar.copyTo(targetFile, overwrite = true)
            println("Plugin copied to: ${targetFile.absolutePath}")
        } ?: run {
            println("WARNING: Could not find shadowJar output")
        }
        val testModsDir = File(project.projectDir, "tests/mods")
        if (testModsDir.exists()) {
            testModsDir.listFiles()?.forEach { modFile ->
                if (modFile.isFile) {
                    val targetFile = File(modsDir, modFile.name)
                    if (targetFile.exists()) {
                        try {
                            targetFile.delete()
                        } catch (_: Exception) {
                        }
                    }
                    if (!targetFile.exists()) {
                        modFile.copyTo(targetFile, overwrite = true)
                        println("Test mod copied to: ${targetFile.absolutePath}")
                    } else {
                        println("WARNING: Test mod is locked; using existing file at ${targetFile.absolutePath}")
                    }
                }
            }
        }
    }

    private fun deleteOldPluginJars(modsDir: File, prefix: String) {
        if (!modsDir.exists()) {
            return
        }
        val deleted = mutableListOf<String>()
        modsDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith(prefix) && file.name.endsWith(".jar")) {
                if (!file.delete()) {
                    throw IllegalStateException(
                        "Failed to delete old ${prefix} jar at ${file.absolutePath}. Stop the server and retry."
                    )
                }
                deleted.add(file.name)
            }
        }
        if (deleted.isNotEmpty()) {
            println("Deleted old ${prefix} jars: ${deleted.joinToString(", ")}")
        }
    }

    /**
     * Find the best start script in the server folder.
     */
    private fun findStartScript(root: File): File? {
        val candidates = listOf("start.bat", "start.ps1", "start.sh")
        return candidates.asSequence()
            .map { File(root, it) }
            .firstOrNull { it.exists() && it.isFile }
    }

    /**
     * Start a platform-appropriate script.
     */
    /**
     * Start a platform-appropriate script with server args.
     */
    private fun startScriptProcess(scriptFile: File, workingDir: File, serverArgs: List<String>): Process {
        val scriptName = scriptFile.name
        val command = when {
            scriptName.endsWith(".bat", ignoreCase = true) -> listOf("cmd", "/c", scriptName)
            scriptName.endsWith(".ps1", ignoreCase = true) -> listOf("powershell", "-ExecutionPolicy", "Bypass", "-File", scriptName)
            scriptName.endsWith(".sh", ignoreCase = true) -> listOf("bash", scriptName)
            else -> listOf(scriptName)
        } + serverArgs
        return ProcessBuilder(command)
            .directory(workingDir)
            .start()
    }

    /**
     * Resolve a Java 25+ executable for running the server.
     */
    private fun resolveJavaExecutable(): String {
        val projectJavaHome = File(project.projectDir, ".jdk")
        if (projectJavaHome.exists()) {
            val javaExe = File(projectJavaHome, "bin/java.exe")
            if (javaExe.exists()) {
                return javaExe.absolutePath
            }
            val javaBin = File(projectJavaHome, "bin/java")
            if (javaBin.exists()) {
                return javaBin.absolutePath
            }
        }
        val envJavaHome = System.getenv("JAVA_HOME")
        if (!envJavaHome.isNullOrBlank()) {
            val javaExe = File(envJavaHome, "bin/java.exe")
            if (javaExe.exists()) {
                return javaExe.absolutePath
            }
            val javaBin = File(envJavaHome, "bin/java")
            if (javaBin.exists()) {
                return javaBin.absolutePath
            }
        }
        val gradleJavaHome = System.getProperty("java.home")
        if (!gradleJavaHome.isNullOrBlank()) {
            val javaExe = File(gradleJavaHome, "bin/java.exe")
            if (javaExe.exists()) {
                return javaExe.absolutePath
            }
            val javaBin = File(gradleJavaHome, "bin/java")
            if (javaBin.exists()) {
                return javaBin.absolutePath
            }
        }
        return "java"
    }

    /**
     * If HYTALE_SERVER_DOWNLOAD_URL is set, ensures the server zip is present in .hytale/server
     * by downloading from the URL when the cache file is missing.
     * The zip is stored in .hytale/server and used as the cached server package.
     */
    private fun ensureServerZipFromDownloadUrl() {
        val downloadUrl = System.getenv("HYTALE_SERVER_DOWNLOAD_URL") ?: return
        if (downloadUrl.isBlank()) {
            return
        }
        val userHome = System.getProperty("user.home") ?: return
        val serverDir = File(userHome, ".hytale/server").apply { mkdirs() }
        val cacheZipName = "server.zip"
        val cachedZip = File(serverDir, cacheZipName)
        if (cachedZip.exists()) {
            println("Using cached server zip at ${cachedZip.absolutePath}")
            return
        }
        println("Downloading server zip from $downloadUrl into .hytale/server ...")
        try {
            URI.create(downloadUrl).toURL().openStream().use { input ->
                cachedZip.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            println("Server zip downloaded and cached at ${cachedZip.absolutePath}")
        } catch (e: Exception) {
            println("ERROR: Failed to download server zip from HYTALE_SERVER_DOWNLOAD_URL")
            println("Error: ${e.message}")
        }
    }

    /**
     * Find the latest server package zip in the user's .hytale/server folder.
     */
    private fun findLatestServerZip(): File? {
        val userHome = System.getProperty("user.home") ?: return null
        val serverDir = File(userHome, ".hytale/server")
        if (!serverDir.exists()) {
            return null
        }
        val zipFiles = serverDir.listFiles { file ->
            file.isFile && file.name.endsWith(".zip")
        } ?: return null
        if (zipFiles.isEmpty()) {
            return null
        }
        return zipFiles.maxByOrNull { it.lastModified() }
    }

    /**
     * Extract a zip file into a directory.
     */
    private fun extractZip(zipFile: File, targetDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zipStream ->
            var entry = zipStream.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { output ->
                        zipStream.copyTo(output)
                    }
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        }
    }

    /**
     * Find the first file with the given name under a directory.
     */
    private fun findFileByName(root: File, fileName: String): File? {
        return root.walkTopDown()
            .firstOrNull { it.isFile && it.name.equals(fileName, ignoreCase = true) }
    }

    /**
     * Find a file by relative path under a directory.
     */
    private fun findFileByPath(root: File, relativePath: String): File? {
        val normalized = relativePath.replace("\\", "/")
        return root.walkTopDown()
            .firstOrNull { it.isFile && it.relativeTo(root).invariantSeparatorsPath.equals(normalized, ignoreCase = true) }
    }
}

/**
 * Task that downloads, sets up, and runs a Hytale server with the plugin.
 */
open class RunServerTask : BaseServerTask() {
    @TaskAction
    fun run() {
        execute(startServer = true)
    }
}

/**
 * Task that prepares the server without starting it.
 */
open class PrepareServerTask : BaseServerTask() {
    @TaskAction
    fun run() {
        execute(startServer = false)
    }
}
