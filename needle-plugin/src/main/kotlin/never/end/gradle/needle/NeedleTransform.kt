package never.end.gradle.needle

import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import never.end.gradle.Log
import never.end.gradle.lib.needle.NeedleConst
import org.gradle.api.Project
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class NeedleTransform(
    private val project: Project,
    private val extension: NeedleExtension,
    private var enabled: Boolean
) : Transform() {
    companion object {
        private const val TAG = "NeedleTransform"
    }

    override fun getName(): String {
        return "NeedleTransform"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    override fun isIncremental(): Boolean {
        return true
    }

    override fun transform(invocation: TransformInvocation?) {
        super.transform(invocation)

        Log.i(TAG, "the needle plugin transform started！ ")

        if (invocation == null)
            return

        val start = System.currentTimeMillis()

        val outputProvider = invocation.outputProvider

        if (!invocation.isIncremental) {
            outputProvider.deleteAll()
        }

        enabled = checkMapping(invocation)

        if (enabled) transformClasses(invocation) else transparentCopy(invocation)

        Log.i(TAG, "the needle plugin cost: " + (System.currentTimeMillis() - start))
    }

    private fun checkMapping(invocation: TransformInvocation): Boolean {
        val buildPath = project.buildDir.absolutePath
        val mappingPath = NeedleConst.getMappingFile(buildPath, invocation.context.variantName)

        Log.i(TAG, "needle mapping file path $mappingPath")
        val mappingFile = File(mappingPath)

        if (mappingFile.isFile) {
            NeedleMapping.initMappingList(mappingFile)
        }

        if (!NeedleMapping.hasMappingFile()) {
            Log.e(TAG, "NO any needle annotation declaration found ! ")
        }
        
        return NeedleMapping.hasMappingFile()
    }

    private fun transformClasses(invocation: TransformInvocation) {
        val changedFiles = ConcurrentHashMap<File, Status>()
        val inputToOutput = ConcurrentHashMap<File, File>()
        val inputFiles = ArrayList<File>()
        var transformDirectory: File? = null

        for (input in invocation.inputs) {
            for (dir in input.directoryInputs) {
                Log.e(TAG,"needle the input dir is ${dir.file.canonicalPath}, changedFiles size is ${dir.changedFiles.size}")
                changedFiles.putAll(dir.changedFiles)
                val inputDir = dir.file
                inputFiles.add(inputDir)
                val outputDirectory = invocation.outputProvider.getContentLocation(
                    dir.name,
                    dir.contentTypes,
                    dir.scopes,
                    Format.DIRECTORY)

                inputToOutput[inputDir] = outputDirectory
                if (transformDirectory == null) transformDirectory = outputDirectory.parentFile
            }

            //TODO: add config to enable jar file rewrite
            for (jarInput in input.jarInputs) {
                transparentCopyJar(jarInput, invocation.outputProvider, invocation.isIncremental)
            }
        }

        if (inputFiles.size == 0 || transformDirectory == null) {
            Log.i(TAG, "needle plugin didn't found any file")
            return
        }

        NeedleTaskManager(project).doTransform(
            classInputs = inputFiles,
            changedFiles = changedFiles,
            inputToOutput = inputToOutput,
            isIncremental = invocation.isIncremental,
            skipCheckClass = false,
            classDirectoryOutput = transformDirectory,
            null,
            null,
            uniqueOutputName = true
        )
    }

    private fun transparentCopy(invocation: TransformInvocation) {
        for (input in invocation.inputs) {
            for (dir in input.directoryInputs) {
                val inputDir = dir.file
                val outputDir = invocation.outputProvider.getContentLocation(
                    dir.name,
                    dir.contentTypes,
                    dir.scopes,
                    Format.DIRECTORY)

                if (invocation.isIncremental) {
                    for (entry in dir.changedFiles.entries) {
                        val inputFile = entry.key
                        when (entry.value) {
                            Status.NOTCHANGED -> {
                            }
                            Status.ADDED, Status.CHANGED -> if (!inputFile.isDirectory) {
                                val outputFile = toOutputFile(outputDir, inputDir, inputFile)
                                copyFileAndMkdirsAsNeed(inputFile, outputFile)
                            }
                            Status.REMOVED -> {
                                val outputFile = toOutputFile(outputDir, inputDir, inputFile)
                                FileUtils.deleteIfExists(outputFile)
                            }
                            else -> {}
                        }
                    }
                } else {
                    for (`in` in FileUtils.getAllFiles(inputDir)) {
                        val out = toOutputFile(outputDir, inputDir, `in`)
                        copyFileAndMkdirsAsNeed(`in`, out)
                    }
                }
            }

            //TODO: add config to enable jar file rewrite
            for (jarInput in input.jarInputs) {
                transparentCopyJar(jarInput, invocation.outputProvider, invocation.isIncremental)
            }
        }
    }

    private fun transparentCopyJar(jarInput: JarInput, outputProvider: TransformOutputProvider, isIncremental: Boolean) {
        val inputJar = jarInput.file
        val outputJar = outputProvider.getContentLocation(
            jarInput.name,
            jarInput.contentTypes,
            jarInput.scopes,
            Format.JAR)

        if (isIncremental) {
            when (jarInput.status) {
                Status.NOTCHANGED -> {
                }
                Status.ADDED, Status.CHANGED -> {
                    copyFileAndMkdirsAsNeed(inputJar, outputJar)
                }
                Status.REMOVED -> FileUtils.delete(outputJar)
                else -> {}
            }
        } else {
            copyFileAndMkdirsAsNeed(inputJar, outputJar)
        }
    }

    private fun copyFileAndMkdirsAsNeed(from: File, to: File) {
        if (from.exists()) {
            to.parentFile.mkdirs()
            FileUtils.copyFile(from, to)
        }
    }

    private fun toOutputFile(outputDir: File, inputDir: File, inputFile: File): File {
        return File(outputDir, inputFile.toRelativeString(inputDir))
    }
}