package gg.moonflower.pollen.core.mixin;

import gg.moonflower.pollen.api.crafting.brewing.PollenBrewingRecipe;
import gg.moonflower.pollen.api.platform.Platform;
import gg.moonflower.pollen.core.Pollen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;

@Mixin(PotionBrewing.class)
public class PotionBrewingMixin {

    @Unique
    private static List<PollenBrewingRecipe> getRecipes() {
        return Platform.getRunningServer().map(server -> server.getRecipeManager().getAllRecipesFor(Pollen.BREWING)).orElse(Collections.emptyList());
    }

    @Inject(method = "isPotionIngredient", at = @At("HEAD"), cancellable = true)
    private static void isPotionIngredient(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        List<PollenBrewingRecipe> recipes = getRecipes();
        for (PollenBrewingRecipe recipe : recipes) {
            if (recipe.getIngredient().test(itemStack)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = "isBrewablePotion", at = @At("HEAD"), cancellable = true)
    private static void isBrewablePotion(Potion potion, CallbackInfoReturnable<Boolean> cir) {
        List<PollenBrewingRecipe> list = getRecipes();
        for (PollenBrewingRecipe recipes : list) {
            if (recipes.getResult() == potion) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = "hasPotionMix", at = @At("HEAD"), cancellable = true)
    private static void hasPotionMix(ItemStack potionStack, ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
        Potion potion = PotionUtils.getPotion(potionStack);

        List<PollenBrewingRecipe> recipes = getRecipes();
        for (PollenBrewingRecipe recipe : recipes) {
            if (recipe.getFrom() == potion && recipe.getIngredient().test(ingredient)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = "mix", at = @At("HEAD"), cancellable = true)
    private static void mix(ItemStack ingredient, ItemStack fromPotion, CallbackInfoReturnable<ItemStack> cir) {
        if (ingredient.isEmpty())
            return;

        Potion potion = PotionUtils.getPotion(fromPotion);
        List<PollenBrewingRecipe> recipes = getRecipes();
        for (PollenBrewingRecipe recipe : recipes) {
            if (recipe.getFrom() == potion && recipe.getIngredient().test(ingredient)) {
                cir.setReturnValue(PotionUtils.setPotion(new ItemStack(fromPotion.getItem()), recipe.getResult()));
                return;
            }
        }
    }
}
