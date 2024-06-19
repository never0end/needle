package never.end.gradle.needle

import never.end.gradle.AgpCompat
import never.end.gradle.Log
import never.end.gradle.needle.asm.NeedleClassAdapter
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.util.CheckClassAdapter
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Arrays
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class NeedleClassWriteTask(
    private val executor: ExecutorService, private var traceError: Boolean = false
) {
    companion object {
        private const val TAG = "NeedleClassWriteTask"
    }

    fun visitAllFile(
        srcFolderList: Map<File, File>, dependencyJarList: Map<File, File>, classLoader: ClassLoader,
        ignoreCheckClass: Boolean
    ) {
        val futures: MutableList<Future<*>> = LinkedList()
        visitFromClassFile(srcFolderList, futures, classLoader, ignoreCheckClass)
        visitFromJarFile(dependencyJarList, futures, classLoader, ignoreCheckClass)
        for (future in futures) {
            future.get()
        }
        require(!traceError) { "something wrong with needle, please see the detail log" }
        futures.clear()
    }

    private fun visitFromClassFile(
        srcMap: Map<File, File>, futures: MutableList<Future<*>>, classLoader: ClassLoader, skipCheckClass: Boolean
    ) {
        for ((key, value) in srcMap) {
            futures.add(executor.submit {
                innerAsmWriteFromSrc(key, value, classLoader, skipCheckClass)
            })
        }
    }

    private fun visitFromJarFile(
        jarList: Map<File, File>, futures: MutableList<Future<*>>, classLoader: ClassLoader, skipCheckClass: Boolean
    ) {
        for ((key, value) in jarList) {
            futures.add(executor.submit {
                innerAsmWriteFromJar(key, value, classLoader, skipCheckClass)
            })
        }
    }

    private fun innerAsmWriteFromSrc(
        inputFile: File, output: File, classLoader: ClassLoader, skipCheckClass: Boolean
    ) {
        val classFileList = ArrayList<File>()

        if (inputFile.isDirectory) {
            listClassFiles(classFileList, inputFile)
        } else {
            classFileList.add(inputFile)
        }

        for (classFile in classFileList) {
            var `is`: InputStream? = null
            var os: FileOutputStream? = null
            try {
                val changedFileInputFullPath = classFile.absolutePath
                val changedFileOutput =
                    File(changedFileInputFullPath.replace(inputFile.absolutePath, output.absolutePath))
                Log.d(TAG, " changedFileOutput file is " + changedFileOutput.canonicalPath)
                if (changedFileOutput.canonicalPath == classFile.canonicalPath) {
                    Log.d(TAG, "output path same with input: output: " + changedFileOutput.canonicalPath)
                    throw RuntimeException(
                        "Input file(" + classFile.canonicalPath + ") should not be same with output!")
                }

                if (!changedFileOutput.exists()) {
                    val success = changedFileOutput.parentFile.mkdirs()
                    Log.d(TAG, "changedFileOutput:" + changedFileOutput.path + ", result=" + success)
                }

                val success = changedFileOutput.createNewFile()
                if (!success) Log.d(TAG,
                    "output ERROR : changedFileOutput: " + changedFileOutput.path + ", " + " create false, classFile:" + classFile)

                `is` = FileInputStream(classFile)
                val classReader = ClassReader(`is`)
                val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)
                val classVisitor = NeedleClassAdapter(AgpCompat.asmApi, classWriter, true)

                Log.d(TAG, "needle the file name is ${classFile.canonicalPath}")
                classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                `is`.close()

                val data = classWriter.toByteArray()
                if (!skipCheckClass) {
                    try {
                        val cr = ClassReader(data)
                        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
                        val check: ClassVisitor = CheckClassAdapter(cw)
                        cr.accept(check, ClassReader.EXPAND_FRAMES)
                    } catch (e: Throwable) {
                        Log.e(TAG, "class writer ERROR : " + e.message + ", " + classFile.canonicalPath)
                        traceError = true
                    }
                }

                os = if (output.isDirectory) {
                    FileOutputStream(changedFileOutput)
                } else {
                    FileOutputStream(output)
                }
                os.write(data)
                os.close()

            } catch (e: Exception) {
                Log.e(TAG, "[innerAsmWriteFromSrc] input:%s , output:%s, e:%s, callstack: %s",
                    classFile.canonicalPath, output.canonicalPath, e.javaClass.name, Arrays.toString(e.stackTrace))
                try {
                    Files.copy(inputFile.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING)
                } catch (e1: Exception) {
                    e1.printStackTrace()
                }
            } finally {
                try {
                    `is`?.close()
                    os?.close()
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    private fun listClassFiles(classFiles: ArrayList<File>, folder: File) {
        val files = folder.listFiles()
        if (null == files) {
            Log.e(TAG, "[listClassFiles] files is null! %s", folder.absolutePath)
            return
        }
        for (file in files) {
            if (file == null) {
                continue
            }
            if (file.isDirectory) {
                listClassFiles(classFiles, file)
            } else {
                if (file.isFile && file.name.endsWith(".class")) {
                    classFiles.add(file)
                }
            }
        }
    }

    private fun innerAsmWriteFromJar(
        input: File, output: File, classLoader: ClassLoader, skipCheckClass: Boolean
    ) {
        var zipOutputStream: ZipOutputStream? = null
        var zipFile: ZipFile? = null
        try {
            zipOutputStream = ZipOutputStream(FileOutputStream(output))
            zipFile = ZipFile(input)
            val enumeration = zipFile.entries()
            while (enumeration.hasMoreElements()) {
                val zipEntry = enumeration.nextElement()
                val zipEntryName = zipEntry.name
                if (preventZipSlip(output, zipEntryName)) {
                    Log.d(TAG, "Unzip entry %s failed!", zipEntryName)
                    continue
                }
                if (isFileInScope(zipEntryName)) {
                    val inputStream = zipFile.getInputStream(zipEntry)
                    val classReader = ClassReader(inputStream)
                    val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)
                    val classVisitor = NeedleClassAdapter(AgpCompat.asmApi, classWriter, true)
                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                    inputStream.close()
                    val data = classWriter.toByteArray()
                    //
                    if (!skipCheckClass) {
                        try {
                            val r = ClassReader(data)
                            val w = ClassWriter(ClassWriter.COMPUTE_FRAMES)
                            val v: ClassVisitor = CheckClassAdapter(w)
                            r.accept(v, ClassReader.EXPAND_FRAMES)
                        } catch (e: Throwable) {
                            Log.e(TAG, "jar output ERROR: " + e.message + ", " + zipEntryName)
                            traceError = true
                        }
                    }
                    val byteArrayInputStream: InputStream = ByteArrayInputStream(data)
                    val newZipEntry = ZipEntry(zipEntryName)
                    addZipEntry(zipOutputStream, newZipEntry, byteArrayInputStream)
                } else {
                    val inputStream = zipFile.getInputStream(zipEntry)
                    val newZipEntry = ZipEntry(zipEntryName)
                    addZipEntry(zipOutputStream, newZipEntry, inputStream)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[innerTraceMethodFromJar] input:%s output:%s e:%s", input, output, e.message)
            (e as? ZipException)?.printStackTrace()
            try {
                if (input.length() > 0) {
                    Files.copy(input.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING)
                } else {
                    Log.e(TAG, "[innerTraceMethodFromJar] input:%s is empty", input)
                }
            } catch (e1: Exception) {
                e1.printStackTrace()
            }
        } finally {
            try {
                if (zipOutputStream != null) {
                    zipOutputStream.finish()
                    zipOutputStream.flush()
                    zipOutputStream.close()
                }
                zipFile?.close()
            } catch (e: Exception) {
                Log.e(TAG, "close stream err!")
            }
        }
    }

    fun preventZipSlip(output: File, zipEntryName: String): Boolean {
        try {
            if (zipEntryName.contains("..") && File(output, zipEntryName).canonicalPath.startsWith(
                    output.canonicalPath + File.separator)
            ) {
                return true
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return true
        }
        return false
    }

    private fun isFileInScope(fileName: String): Boolean {
        if (!fileName.endsWith(".class")) {
            return false
        }
        return true
    }

    fun addZipEntry(zipOutputStream: ZipOutputStream, zipEntry: ZipEntry?, inputStream: InputStream) {
        try {
            zipOutputStream.putNextEntry(zipEntry)
            val buffer = ByteArray(16384)
            var length = -1
            while (inputStream.read(buffer, 0, buffer.size).also { length = it } != -1) {
                zipOutputStream.write(buffer, 0, length)
                zipOutputStream.flush()
            }
        } catch (e: ZipException) {
            Log.e(TAG, "addZipEntry err!")
        } finally {
            closeQuietly(inputStream)
            zipOutputStream.closeEntry()
        }
    }
    fun closeQuietly(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (e: IOException) {
            Log.w(TAG, "Failed to close resource", e)
        }
    }
}