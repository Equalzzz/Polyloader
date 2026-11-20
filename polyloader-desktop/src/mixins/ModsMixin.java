package mixins;

import arc.files.Fi;
import arc.struct.Seq;
import mindustry.core.Platform;
//import mindustry.mod.ModClassLoader;
import mindustry.mod.Mods;
import org.im.polyloader.Polyloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Mods.class)
public class ModsMixin {

    /**
     * @author Isaac Mercer
     * @reason Game should use polyloader class loader (used in ContentParser)
     */
    @Overwrite
    public ClassLoader mainLoader(){
        return Polyloader.INSTANCE.getClassLoader();
    }

    @Redirect(
            method = "loadMod",
            at = @At(
                    value = "INVOKE",
                    target = "mindustry/core/Platform.loadJar(Larc/files/Fi;Ljava/lang/ClassLoader;)Ljava/lang/ClassLoader;")
    )
    private ClassLoader redirectLoadJar(Platform instance, Fi jar, ClassLoader parent){
        return Polyloader.INSTANCE.getClassLoader();
    }

    @ModifyVariable(method = "load()V", at = @At("STORE"), name = "candidates")
    private Seq<Fi> injectCandidates(Seq<Fi> candidates) {
        return new Seq<>(new Fi[] { new Fi(Polyloader.INSTANCE.getPolyloaderCore()) });
    }
}
