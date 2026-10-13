package org.im.polyloader.mixin

import java.net.URL
import java.net.URLClassLoader

// All mod mixin configs are passed in a single loader, but they just try their best at finding
// resource in any URL available, so any config with the same relative path could be loaded - which might be a problem
// as in rare cases one mod could read other mod's config or load other mod's mixins, which shouldn't be allowed
// will fix it when it becomes a problem todo: ^^^
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

    // Adding urls should be public method
    public override fun addURL(url: URL) = super.addURL(url)

    fun addTransformer(classTransformer: IPolyloaderClassTransformer) {
        classTransformers.add(classTransformer)
        classTransformers.sortWith(Comparator.comparingInt(IPolyloaderClassTransformer::transformPriority))
    }
}