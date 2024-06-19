
package never.end.gradle

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.api.ReadOnlyBuildType
import com.android.builder.model.SigningConfig
import org.objectweb.asm.Opcodes

class AgpCompat {

    companion object {
        @JvmField
        val getIntermediatesSymbolDirName = {
            when {
                VersionsCompat.lessThan(AGPVersion.AGP_3_6_0) -> "symbols"
                VersionsCompat.greatThanOrEqual(AGPVersion.AGP_3_6_0) -> "runtime_symbol_list"
                else -> "symbols"
            }
        }

        fun getSigningConfig(variant: BaseVariant): SigningConfig? {
            return (variant.buildType as ReadOnlyBuildType).signingConfig
        }

        @JvmStatic
        val asmApi: Int
            get() = when {
                VersionsCompat.greatThanOrEqual(AGPVersion.AGP_7_0_0) -> Opcodes.ASM6
                else -> Opcodes.ASM5
            }
    }

}