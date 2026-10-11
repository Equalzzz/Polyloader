package mixins;

import arc.files.Fi;
import mindustry.core.Platform;
//import mindustry.mod.ModClassLoader;
import mindustry.mod.Mods;
import org.im.polyloader.Polyloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
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

    /*
    I don't know if it's necessary or not
    @Redirect(
            method = "loadMod",
            at = @At(
                    value = "INVOKE",
                    target = "mindustry/mod/ModClassLoader.addChild (Ljava/lang/ClassLoader;)V")
    )
    private void redirectLoadMod(ModClassLoader instance, ClassLoader child) {}
    */
}
