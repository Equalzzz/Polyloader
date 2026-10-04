package org.im.polyloader.mixin

import org.spongepowered.asm.service.IGlobalPropertyService
import org.spongepowered.asm.service.IPropertyKey

class PolyloaderGlobalPropertyService : IGlobalPropertyService {
    private val values : MutableMap<IPropertyKey, Any?> = HashMap()

    override fun resolveKey(name: String?): IPropertyKey = Key(name)

    @Suppress("UNCHECKED_CAST") // Surely nothing can go wrong
    override fun <T> getProperty(key: IPropertyKey?): T? = values[key] as? T

    override fun setProperty(key: IPropertyKey?, value: Any?) = values.set(key!!, value)

    @Suppress("UNCHECKED_CAST") // Surely nothing can go wrong x2
    override fun <T> getProperty(key: IPropertyKey?, defaultValue: T?): T? = values[key] as? T ?: defaultValue

    override fun getPropertyString(
        key: IPropertyKey?,
        defaultValue: String?
    ): String? = values[key] as? String ?: defaultValue
}

private data class Key(val name: String?) : IPropertyKey