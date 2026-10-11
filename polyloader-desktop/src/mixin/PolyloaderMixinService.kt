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

    override fun getName() = "Polyloader-Mixin"

    override fun isValid() = true

    // for some reason it ISN'T used, but should have been used
    override fun getClassProvider(): IClassProvider {
        Polyloader.log.debug("getClassProvider()")
        TODO("Not yet implemented")
    }

    override fun getBytecodeProvider(): IClassBytecodeProvider {
        return bytecodeProvider
    }

    override fun getTransformerProvider(): ITransformerProvider? {
        Polyloader.log.debug("getTransformerProvider()")
        return null
    }

    override fun getClassTracker(): IClassTracker? {
        Polyloader.log.debug("getClassTracker()")
        return null
    }

    override fun getAuditTrail(): IMixinAuditTrail? {
        Polyloader.log.debug("getAuditTrail()")
        return null
    }

    // for some reason it is used
    // todo: understand what it does
    override fun getPlatformAgents(): Collection<String> = Collections.emptyList()

    // todo: understand what it does
    override fun getPrimaryContainer(): IContainerHandle = containerHandle

    override fun getResourceAsStream(name: String?): InputStream? {
        Polyloader.log.debug("Pending resource for $name")
        return Polyloader::class.java.classLoader.getResourceAsStream(name) ?: return null
    }

    lateinit var phaseConsumer : IConsumer<MixinEnvironment.Phase?>

    @Deprecated("Deprecated in Java")
    override fun wire(phase: MixinEnvironment.Phase?, phaseConsumer: IConsumer<MixinEnvironment.Phase?>?) {
        this.phaseConsumer = phaseConsumer!!
        super.wire(phase, phaseConsumer)
    }

    fun onGameStart() {
        phaseConsumer.accept(MixinEnvironment.Phase.DEFAULT)
    }
}