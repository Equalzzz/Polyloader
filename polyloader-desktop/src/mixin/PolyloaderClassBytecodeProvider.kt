package org.im.polyloader.mixin

import org.im.polyloader.Polyloader
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.service.IClassBytecodeProvider
import org.spongepowered.asm.transformers.MixinClassReader

class PolyloaderClassBytecodeProvider : IClassBytecodeProvider {
    override fun getClassNode(name: String): ClassNode {
        Polyloader.classLoader.getResourceAsStream(name.replace('.', '/') + ".class")?.let { stream ->
            val bytes = stream.readBytes()
            val classNode = ClassNode()
            MixinClassReader(bytes, name)
                .accept(classNode, ClassReader.EXPAND_FRAMES)
            return classNode
        }
        throw ClassNotFoundException(name)
    }

    override fun getClassNode(name: String, runTransformers: Boolean): ClassNode =
        getClassNode(name)
}