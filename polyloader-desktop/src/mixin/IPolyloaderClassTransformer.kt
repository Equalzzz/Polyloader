package org.im.polyloader.mixin

interface IPolyloaderClassTransformer {
    fun transformClassBytes(classBytes: ByteArray, className: String) : ByteArray
    val transformPriority : Int
        get() = 0
}