package never.end.gradle.needle

import never.end.gradle.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object NeedleMapping {
    private var mappingFile:File? = null
    private val mapping =  HashMap<String,String>()

    fun isTargetClass(name:String) = mapping.containsKey(name)

    fun isSouceClass(name:String) = mapping.containsValue(name)

    fun getSourceClass(targetName: String): String? {
        return mapping[targetName]
    }

    private fun findMappingSource(folder: File):File? {
        val files = folder.listFiles()
        if (null == files) {
            Log.e("NeedleMapping",
                "[findMappingSource] files is null! %s",
                folder.absolutePath
            )
            return null
        }
        for (file in files) {
            if (file == null) {
                continue
            }
            if (file.isDirectory) {
                findMappingSource(file)
            } else {
                if (file.isFile && file.name.equals("needle.mapping")) {
                    this.mappingFile = file
                    initMappingList(file)
                    return file
                }
            }
        }
        return null
    }

    fun initMappingList(file: File) {
        mapping.clear()
        val inputStream = file.inputStream()
        val reader = BufferedReader(InputStreamReader(inputStream))
        reader.lines().forEach {
            val oneline =  it.split(":")
            val targets = oneline[1].split(",")
            println("needle mapping read one line $oneline")
            for (target in targets) {
                val targetClass = target.replace(".", "/")
                val sourceClass = oneline[0].replace(".", "/")
                mapping[targetClass] = sourceClass
            }
        }
        reader.close()
        inputStream.close()

        println("needle mapping result $mapping")
    }

    fun hasMappingFile() =  (mapping.isNotEmpty())
}