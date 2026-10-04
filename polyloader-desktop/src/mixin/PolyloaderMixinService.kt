package org.im.polyloader.mixin

import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual
import org.spongepowered.asm.launch.platform.container.IContainerHandle
import org.spongepowered.asm.service.IClassBytecodeProvider
import org.spongepowered.asm.service.IClassProvider
import org.spongepowered.asm.service.IClassTracker
import org.spongepowered.asm.service.IMixinAuditTrail
import org.spongepowered.asm.service.ITransformerProvider
import org.spongepowered.asm.service.MixinServiceAbstract
import java.io.InputStream
import java.util.Collections

class PolyloaderMixinService : MixinServiceAbstract() {
    override fun getName() = "Polyloader-Mixin"

    override fun isValid() = true

    override fun getClassProvider(): IClassProvider? {
        TODO("Not yet implemented")
    }

    override fun getBytecodeProvider(): IClassBytecodeProvider? {
        TODO("Not yet implemented")
    }

    override fun getTransformerProvider(): ITransformerProvider? = null

    override fun getClassTracker(): IClassTracker? = null

    override fun getAuditTrail(): IMixinAuditTrail? = null

    // todo: understand what it does
    override fun getPlatformAgents(): Collection<String?>? = Collections.emptyList()

    // todo: understand what it does
    override fun getPrimaryContainer(): IContainerHandle = ContainerHandleVirtual(name)

    override fun getResourceAsStream(name: String?): InputStream? = null
}