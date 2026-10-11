package org.im.polyloader.mixin

import java.net.URL
import java.net.URLClassLoader

class PolyloaderClassLoader : URLClassLoader {
    private val classTransformers: MutableList<IPolyloaderClassTransformer> = mutableListOf()

    constructor(urls: Array<URL>, parentClassLoader: ClassLoader):
            super(urls, parentClassLoader)

    override fun findClass(name: String): Class<*> {
        val classPath = name.replace('.', '/') + ".class"

        val url = getResource(classPath) ?: throw ClassNotFoundException(name)
        var rawBytes = url.readBytes()
        classTransformers.forEach { transformer ->
            rawBytes = transformer.transformClassBytes(rawBytes, name)
        }
        return defineClass(name, rawBytes, 0, rawBytes.size)
    }

    fun addTransformer(classTransformer: IPolyloaderClassTransformer) {
        classTransformers.add(classTransformer)
        classTransformers.sortWith(Comparator.comparingInt(IPolyloaderClassTransformer::transformPriority))
    }
}