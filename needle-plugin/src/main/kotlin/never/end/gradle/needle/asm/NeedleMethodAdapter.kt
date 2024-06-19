package never.end.gradle.needle.asm

import never.end.gradle.needle.NeedleMapping
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class NeedleMethodAdapter(api: Int, methodVisitor: MethodVisitor?, access: Int, name: String?, descriptor: String?,
                          private var skipOtherChild:Boolean) :
    AdviceAdapter(api, methodVisitor, access, name, descriptor) {

    private var methodEnter = false

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        var targetOwner = owner

        if (opcode == Opcodes.INVOKESPECIAL
            && owner != null && NeedleMapping.isTargetClass(owner)
            && name != null && "<init>" == name) {

            if (methodEnter || !skipOtherChild) {
                println("NeedleMethodAdapter found the target`s construction call, target: $owner, name : $name, des:$descriptor")
                targetOwner = NeedleMapping.getSourceClass(owner)
            }

        }

        super.visitMethodInsn(opcode, targetOwner, name, descriptor, isInterface)

    }

    override fun onMethodEnter() {
        super.onMethodEnter()
        methodEnter = true
    }

    override fun visitTypeInsn(opcode: Int, type: String?) {
        if (opcode == Opcodes.NEW && type != null ) {
            if (NeedleMapping.isTargetClass(type)) {
                println("NeedleMethodAdapter found the target`s new call, target: $type")
                super.visitTypeInsn(opcode, NeedleMapping.getSourceClass(type))
                return
            }

        }
        super.visitTypeInsn(opcode, type)

    }
}