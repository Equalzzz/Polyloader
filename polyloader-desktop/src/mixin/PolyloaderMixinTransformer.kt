package org.im.polyloader.mixin

import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.transformer.IMixinTransformer

// This class is used as a reflection of a private MixinTransformer class
class PolyloaderMixinTransformer : IPolyloaderClassTransformer {
    init {
        // this bastard is private - you can't access it w/o reflection
        val transformerClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer")
        val constructor = transformerClass.getDeclaredConstructor()
        constructor.isAccessible = true
        constructor.newInstance()
    }

    // I don't know what it does, yet
    override val transformPriority: Int = Int.MIN_VALUE

    // It calls a private method from the transformer class, that transforms bytes in a way, that is kinda useful for us
    override fun transformClassBytes(classBytes: ByteArray, className: String): ByteArray {
        //return method.invoke(transformer, className, className, classBytes) as ByteArray
        return (MixinEnvironment.getDefaultEnvironment().activeTransformer as IMixinTransformer).transformClassBytes(className, className, classBytes)
    }
}