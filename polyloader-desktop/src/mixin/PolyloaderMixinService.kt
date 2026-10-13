package org.im.polyloader.mixin

import org.im.polyloader.Polyloader
import org.im.polyloader.utils.debug
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual
import org.spongepowered.asm.launch.platform.container.IContainerHandle
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.service.*
import org.spongepowered.asm.util.IConsumer
import java.io.InputStream
import java.util.*

class PolyloaderMixinService : MixinServiceAbstract() {
    private val containerHandle = ContainerHandleVirtual(name)
    private val bytecodeProvider = PolyloaderClassBytecodeProvider()
    private val modConfigs : MutableMap<String, MutableList<String>> = HashMap()
    override fun getName() = "Polyloader-Mixin"

    override fun isValid() = true

    // for some reason it ISN'T used, but should have been used
    override fun getClassProvider(): IClassProvider =
        TODO("getClassProvider() is not yet implemented")

    override fun getBytecodeProvider(): IClassBytecodeProvider = bytecodeProvider

    override fun getTransformerProvider(): ITransformerProvider? {
        debug("getTransformerProvider() was called")
        return null
    }

    override fun getClassTracker(): IClassTracker? {
        debug("getClassTracker() was called")
        return null
    }

    override fun getAuditTrail(): IMixinAuditTrail? = null

    // for some reason it is used
    // todo: understand what it does
    override fun getPlatformAgents(): Collection<String> = Collections.emptyList()

    // todo: understand what it does
    override fun getPrimaryContainer(): IContainerHandle = containerHandle

    // All mod mixin configs are passed in a single loader, but they just try their best at finding
    // resource in any URL available, so any config with the same relative path could be loaded - which might be a problem
    // as in rare cases one mod could read other mod's config or load other mod's mixins, which shouldn't be allowed
    // will fix it when it becomes a problem todo: ^^^
    override fun getResourceAsStream(name: String): InputStream? {
        debug("Pending resource for $name", true)
        val stream = Polyloader.classLoader.getResourceAsStream(name)
        if (stream != null) return stream
        // then it's probably a mod mixin config that looks like 'modName:path/giga-config.mixins.json'
        return Polyloader.classLoader.getResourceAsStream(name.substringAfterLast(':')) ?: return null
    }

    // Here we steal phase consumer from evil mixin ecosystem to change a phase a bit later
    private lateinit var phaseConsumer : IConsumer<MixinEnvironment.Phase?>
    @Deprecated("Deprecated in Java")
    override fun wire(phase: MixinEnvironment.Phase?, phaseConsumer: IConsumer<MixinEnvironment.Phase?>?) {
        this.phaseConsumer = phaseConsumer!!
        super.wire(phase, phaseConsumer)
    }

    // Here we USE that phase consumer to actually change the phase to DEFAULT
    fun onGameStart() {
        phaseConsumer.accept(MixinEnvironment.Phase.DEFAULT)
    }
}