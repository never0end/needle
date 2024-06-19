package never.end.gradle.needle.asm

import com.android.tools.build.jetifier.core.utils.Log
import never.end.gradle.AgpCompat
import never.end.gradle.needle.NeedleMapping
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class NeedleClassAdapter(api: Int,
                         writer: ClassWriter,
                         private val skipOtherChild: Boolean =true)
    : ClassVisitor(api, writer) {
    private lateinit var className:String
    private var needReplace = false
    private var isABSClass = false

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String,
        interfaces: Array<String?>?
    ) {
        this.className = name
        Log.i("NeedleClassAdapter", "visit in for class: %s", className)
        this.needReplace = !NeedleMapping.isSouceClass(name)

        if (access and Opcodes.ACC_ABSTRACT > 0 || access and Opcodes.ACC_INTERFACE > 0) {
            this.isABSClass = true
        }

        if (needReplace && !skipOtherChild && NeedleMapping.isTargetClass(superName)){
            super.visit(version, access, name, signature, NeedleMapping.getSourceClass(superName), interfaces)
        }else {
            super.visit(version, access, name, signature, superName, interfaces)
        }
    }

    override fun visitInnerClass(
        name: String?,
        outerName: String?,
        innerName: String?,
        access: Int
    ) {
        super.visitInnerClass(name, outerName, innerName, access)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        if (isABSClass || !needReplace)
            return super.visitMethod(access, name, descriptor, signature, exceptions)

        val methodVisitor = cv.visitMethod(access, name, descriptor, signature, exceptions)
        return NeedleMethodAdapter(AgpCompat.asmApi, methodVisitor, access, name, descriptor, skipOtherChild)
    }


    override fun visitEnd() {
        super.visitEnd()
    }


}