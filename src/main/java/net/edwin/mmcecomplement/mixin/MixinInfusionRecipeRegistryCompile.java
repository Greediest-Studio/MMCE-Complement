package net.edwin.mmcecomplement.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Nova Engineering's Astral Sorcery adapter can query the infusion registry
 * before Astral Sorcery's normal post-init compilation pass.  Astral's
 * getRecipe method dereferences its private compiledRecipes array directly,
 * so that ordering difference otherwise becomes a startup NPE.
 *
 * <p>This is a pseudo mixin because Astral Sorcery is an optional dependency
 * of MMCE Complement.  Reflection keeps the compatibility layer independent
 * of Astral's private implementation at compile time.</p>
 */
@Pseudo
@Mixin(targets =
    "hellfirepvp.astralsorcery.common.crafting.infusion.InfusionRecipeRegistry",
    remap = false)
public abstract class MixinInfusionRecipeRegistryCompile {

    @Inject(method = "getRecipe", at = @At("HEAD"), remap = false)
    private static void mmceComplement$compileBeforeLookup(int id,
        CallbackInfoReturnable<Object> cir) {
        try {
            Class<?> registry = Class.forName(
                "hellfirepvp.astralsorcery.common.crafting.infusion.InfusionRecipeRegistry");
            Field compiled = registry.getDeclaredField("compiledRecipes");
            compiled.setAccessible(true);
            if (compiled.get(null) != null) return;

            Method compile = registry.getDeclaredMethod("compileRecipes");
            compile.setAccessible(true);
            compile.invoke(null);
        } catch (Throwable ignored) {
            // Keep the original method's behavior when a different Astral
            // Sorcery implementation changes these private details.  The
            // mixin must never turn an optional integration into a hard
            // startup failure.
        }
    }
}
