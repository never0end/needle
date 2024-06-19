package never.end.gradle.needle

import com.android.build.api.transform.Status
import com.android.utils.FileUtils
import com.google.common.hash.Hashing
import never.end.gradle.Log
import never.end.gradle.PluginClassLoader
import never.end.gradle.utils.IOUtil
import org.gradle.api.Project
import java.io.File
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class NeedleTaskManager(private val project: Project) {
    companion object {
        private const val TAG: String = "Needle.Task"

        @Suppress("DEPRECATION")
        fun getUniqueJarName(jarFile: File): String {
            val origJarName = jarFile.name
            val hashing = Hashing.sha1().hashString(jarFile.path, Charsets.UTF_16LE).toString()
            val dotPos = origJarName.lastIndexOf('.')
            return if (dotPos < 0) {
                String.format("%s_%s", origJarName, hashing)
            } else {
                val nameWithoutDotExt = origJarName.substring(0, dotPos)
                val dotExt = origJarName.substring(dotPos)
                String.format("%s_%s%s", nameWithoutDotExt, hashing, dotExt)
            }
        }

        fun appendSuffix(jarFile: File, suffix: String): String {
            val origJarName = jarFile.name
            val dotPos = origJarName.lastIndexOf('.')
            return if (dotPos < 0) {
                String.format("%s_%s", origJarName, suffix)
            } else {
                val nameWithoutDotExt = origJarName.substring(0, dotPos)
                val dotExt = origJarName.substring(dotPos)
                String.format("%s_%s%s", nameWithoutDotExt, suffix, dotExt)
            }
        }
    }

    fun doTransform(classInputs: Collection<File>,
                    changedFiles: Map<File, Status>,
                    inputToOutput: Map<File, File>,
                    isIncremental: Boolean,
                    skipCheckClass: Boolean,
                    classDirectoryOutput: File,
                    legacyReplaceChangedFile: ((File, Map<File, Status>) -> Object)?,
                    legacyReplaceFile: ((File, File) -> (Object))?,
                    uniqueOutputName: Boolean
    ) {

        val executor: ExecutorService = Executors.newFixedThreadPool(16)

        /**
         * step 1
         */
        var start = System.currentTimeMillis()

        val futures = LinkedList<Future<*>>()

        val dirInputOutMap = ConcurrentHashMap<File, File>()
        val jarInputOutMap = ConcurrentHashMap<File, File>()

        for (file in classInputs) {
            if (file.isDirectory) {
                futures.add(executor.submit(
                    CollectDirectoryInputTask(
                        directoryInput = file,
                        mapOfChangedFiles = changedFiles,
                        mapOfInputToOutput = inputToOutput,
                        isIncremental = isIncremental,
                        traceClassDirectoryOutput = classDirectoryOutput,
                        legacyReplaceChangedFile = legacyReplaceChangedFile,
                        legacyReplaceFile = legacyReplaceFile,
                        // result
                        resultOfDirInputToOut = dirInputOutMap
                    )
                ))
            } else {
                val status = Status.CHANGED
                futures.add(executor.submit(
                    CollectJarInputTask(
                        inputJar = file,
                        inputJarStatus = status,
                        inputToOutput = inputToOutput,
                        isIncremental = isIncremental,
                        traceClassFileOutput = classDirectoryOutput,
                        legacyReplaceFile = legacyReplaceFile,
                        uniqueOutputName = uniqueOutputName,
                        // result
                        resultOfDirInputToOut = dirInputOutMap,
                        resultOfJarInputToOut = jarInputOutMap
                    )
                ))
            }
        }

        for (future in futures) {
            future.get()
        }
        futures.clear()

        Log.i(TAG, "[doTransform] Step(1)[Parse]... cost:%sms", System.currentTimeMillis() - start)

        /**
         * step 2
         */
        start = System.currentTimeMillis()
        val methodTracer = NeedleClassWriteTask(executor)
        val allInputs = ArrayList<File>().also {
            it.addAll(dirInputOutMap.keys)
            it.addAll(jarInputOutMap.keys)
        }
        val traceClassLoader = PluginClassLoader.getClassLoader(project, allInputs)
        methodTracer.visitAllFile(dirInputOutMap, jarInputOutMap, traceClassLoader, skipCheckClass)

        Log.i(TAG, "[doTransform] Step(2)[Trace]... cost:%sms", System.currentTimeMillis() - start)
    }

    class CollectDirectoryInputTask(
        private val directoryInput: File,
        private val mapOfChangedFiles: Map<File, Status>,
        private val mapOfInputToOutput: Map<File, File>,
        private val isIncremental: Boolean,
        private val traceClassDirectoryOutput: File,
        private val legacyReplaceChangedFile: ((File, Map<File, Status>) -> (Object))?,     // Will be removed in the future
        private val legacyReplaceFile: ((File, File) -> (Object))?,                         // Will be removed in the future

        private val resultOfDirInputToOut: MutableMap<File, File>
    ) : Runnable {

        override fun run() {
            try {
                handle()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "%s", e.toString())
            }
        }

        private fun handle() {
            val dirInput = directoryInput
            val dirOutput = if (mapOfInputToOutput.containsKey(dirInput)) {
                mapOfInputToOutput[dirInput]!!
            } else {
                File(traceClassDirectoryOutput, dirInput.name)
            }
            val inputFullPath = dirInput.absolutePath
            val outputFullPath = dirOutput.absolutePath

            if (!dirOutput.exists()) {
                dirOutput.mkdirs()
            }

            if (!dirInput.exists() && dirOutput.exists()) {
                if (dirOutput.isDirectory) {
                    FileUtils.deletePath(dirOutput)
                } else {
                    FileUtils.delete(dirOutput)
                }
            }

            if (isIncremental) {
                val outChangedFiles = HashMap<File, Status>()

                for ((changedFileInput, status) in mapOfChangedFiles) {
                    val changedFileInputFullPath = changedFileInput.absolutePath

                    // mapOfChangedFiles is contains all. each collectDirectoryInputTask should handle itself, should not handle other file
                    if (!changedFileInputFullPath.contains(inputFullPath)) {
                        continue
                    }

                    val changedFileOutput = File(changedFileInputFullPath.replace(inputFullPath, outputFullPath))

                    if (status == Status.ADDED || status == Status.CHANGED) {
                        resultOfDirInputToOut[changedFileInput] = changedFileOutput
                    } else if (status == Status.REMOVED) {
                        changedFileOutput.delete()
                    }
                    outChangedFiles[changedFileOutput] = status
                }

                legacyReplaceChangedFile?.invoke(dirInput, outChangedFiles)
            } else {
                resultOfDirInputToOut[dirInput] = dirOutput
            }

            legacyReplaceFile?.invoke(dirInput, dirOutput)
        }
    }

    class CollectJarInputTask(
        private val inputJar: File,
        private val inputJarStatus: Status,
        private val inputToOutput: Map<File, File>,
        private val isIncremental: Boolean,
        private val traceClassFileOutput: File,
        private val legacyReplaceFile: ((File, File) -> (Object))?,             // Will be removed in the future
        private val uniqueOutputName: Boolean,
        private val resultOfDirInputToOut: MutableMap<File, File>,
        private val resultOfJarInputToOut: MutableMap<File, File>
    ) : Runnable {

        override fun run() {
            try {
                handle()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "%s", e.toString())
            }
        }

        private fun handle() {

            val jarInput = inputJar
            val jarOutput = if (inputToOutput.containsKey(jarInput)) {
                inputToOutput[jarInput]!!
            } else {
                val outputJarName = if (uniqueOutputName)
                    getUniqueJarName(jarInput)
                else
                    appendSuffix(jarInput, "needle")
                File(traceClassFileOutput, outputJarName)
            }

            Log.d(TAG, "CollectJarInputTask input %s -> output %s", jarInput, jarOutput)

            if (!isIncremental && jarOutput.exists()) {
                jarOutput.delete()
            }
            if (!jarOutput.parentFile.exists()) {
                jarOutput.parentFile.mkdirs()
            }

            if (IOUtil.isRealZipOrJar(jarInput)) {
                if (isIncremental) {
                    if (inputJarStatus == Status.ADDED || inputJarStatus == Status.CHANGED) {
                        resultOfJarInputToOut[jarInput] = jarOutput
                    } else if (inputJarStatus == Status.REMOVED) {
                        jarOutput.delete()
                    }

                } else {
                    resultOfJarInputToOut[jarInput] = jarOutput
                }

            } else {

                // TODO for wechat
                Log.i(TAG, "Special case for WeChat AutoDex. Its rootInput jar file is actually a txt file contains path list.")
                // Special case for WeChat AutoDex. Its rootInput jar file is actually
                // a txt file contains path list.
                jarInput.inputStream().bufferedReader().useLines { lines ->
                    lines.forEach { realJarInputFullPath ->
                        val realJarInput = File(realJarInputFullPath)
                        // dest jar, moved to extra guard intermediate output dir.
                        val realJarOutput = File(traceClassFileOutput, getUniqueJarName(realJarInput))

                        if (realJarInput.exists() && IOUtil.isRealZipOrJar(realJarInput)) {
                            resultOfJarInputToOut[realJarInput] = realJarOutput
                        } else {
                            realJarOutput.delete()
                            if (realJarInput.exists() && realJarInput.isDirectory) {
                                val realJarOutputDir = File(traceClassFileOutput, realJarInput.name)
                                if (!realJarOutput.exists()) {
                                    realJarOutput.mkdirs()
                                }
                                resultOfDirInputToOut[realJarInput] = realJarOutputDir
                            }

                        }
                        // write real output full path to the fake jar at rootOutput.
                        jarOutput.outputStream().bufferedWriter().use { bw ->
                            bw.write(realJarOutput.absolutePath)
                            bw.newLine()
                        }
                    }
                }

                jarInput.delete() // delete raw inputList
            }

            legacyReplaceFile?.invoke(jarInput, jarOutput)
        }
    }
}