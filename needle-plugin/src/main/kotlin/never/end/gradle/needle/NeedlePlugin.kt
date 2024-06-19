package never.end.gradle.needle

import com.android.build.gradle.BaseExtension
import com.android.tools.build.jetifier.core.utils.Log
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException

class NeedlePlugin: Plugin<Project> {
    companion object {
        const val TAG = "NeedlePlugin.Plugin"
    }

    override fun apply(p0: Project) {
        println("NeedlePlugin apply caling!!")
        getNeedleMappingFile(p0)
    }

    private fun getNeedleMappingFile(p:Project) {
        if (p.plugins.findPlugin("com.android.application") == null
            && p.plugins.findPlugin("com.android.library") == null
        ) {
            throw ProjectConfigurationException(
                "Need android application/library plugin to be applied first",
                null as Throwable?
            )
        }

        Log.i(TAG, "getNeedleMappingFile")
        val baseExtension = p.extensions.getByName("android") as BaseExtension
        val needleExtension = p.extensions.create("needle", NeedleExtension::class.java)
        baseExtension.registerTransform(NeedleTransform(p, needleExtension, true))


        Log.i(TAG, "the needle plugin project path:${p.path}, build dir: ${p.buildDir}")
        p.afterEvaluate {
            Log.e(TAG,"the needle plugin afterEvaluate project is :${it.name}")
        }
    }
}