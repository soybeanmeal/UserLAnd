package tech.ula.utils

import android.os.Environment
import android.util.Log
import java.io.File

// TODO refactor this class with a better name
class FileUtility(private val applicationFilesDirectoryPath: String) {

    fun getSupportDirPath(): String {
        return "$applicationFilesDirectoryPath/support"
    }

    fun getFilesDirPath(): String {
        return applicationFilesDirectoryPath
    }

    fun createAndGetDirectory(directory: String): File {
        val file = File("${getFilesDirPath()}/$directory")
        if (!file.exists()) file.mkdirs()
        return file
    }

    fun statusFileExists(filesystemName: String, filename: String): Boolean {
        val file = File("${getFilesDirPath()}/$filesystemName/support/$filename")
        return file.exists()
    }

    fun distributionAssetsExist(distributionType: String): Boolean {
        val rootfsParts = listOf("rootfs.tar.gz.part00", "rootfs.tar.gz.part01", "rootfs.tar.gz.part02", "rootfs.tar.gz.part03")
        rootfsParts.map {
            File("${getFilesDirPath()}/$distributionType/$it")
        }.forEach {
            if (!it.exists()) return false
        }
        return true
    }

    // Filename takes form of UserLAnd:<directory to place in>:<filename>
    fun moveAssetsToCorrectSharedDirectory(source: File = Environment.getExternalStoragePublicDirectory((Environment.DIRECTORY_DOWNLOADS))) {
        source.walkBottomUp()
                .filter { it.name.contains("UserLAnd:") }
                .forEach {
                    val (_, directory, filename) = it.name.split(":")
                    val targetDestination = File("${getFilesDirPath()}/$directory/$filename")
                    it.copyTo(targetDestination, overwrite = true)
                    if (!it.delete())
                        Log.e("FileUtility", "Could not delete downloaded file: ${it.name}")
                }
    }

    fun copyDistributionAssetsToFilesystem(targetFilesystemName: String, distributionType: String) {
        val sharedDirectory = File("${getFilesDirPath()}/$distributionType")
        val targetDirectory = createAndGetDirectory("$targetFilesystemName/support")
        sharedDirectory.copyRecursively(targetDirectory, overwrite = true)
        targetDirectory.walkBottomUp().forEach {
            if (it.name == "support") {
                return
            }
            changePermission(it.name, "$targetFilesystemName/support")
        }
    }

    fun correctFilePermissions(distributionType: String) {
        val filePermissions = listOf(
                "proot" to "support",
                "killProcTree.sh" to "support",
                "isServerInProcTree.sh" to "support",
                "busybox" to "support",
                "libtalloc.so.2" to "support",
                "execInProot.sh" to "support",
                "startSSHServer.sh" to distributionType,
                "startVNCServer.sh" to distributionType,
                "startVNCServerStep2.sh" to distributionType,
                "busybox" to distributionType,
                "libdisableselinux.so" to distributionType)
        filePermissions.forEach { (file, subdirectory) -> changePermission(file, subdirectory) }
    }

    private fun changePermission(filename: String, subdirectory: String) {
        val executionDirectory = createAndGetDirectory(subdirectory)
        val commandToRun = arrayListOf("chmod", "0777", filename)

        val pb = ProcessBuilder(commandToRun)
        pb.directory(executionDirectory)

        val process = pb.start()
        process.waitFor()
    }
}