package net.edwin.mmcecomplement.compat.jei.mechannel;

import mezz.jei.api.ingredients.IIngredientHelper;
import net.edwin.mmcecomplement.Tags;

import javax.annotation.Nullable;

/** JEI identity and display metadata for ME channel requirements. */
public final class MEChannelIngredientHelper
    implements IIngredientHelper<MEChannelIngredient> {

    public static final MEChannelIngredientHelper INSTANCE =
        new MEChannelIngredientHelper();

    private MEChannelIngredientHelper() { }

    @Nullable
    @Override
    public MEChannelIngredient getMatch(
        Iterable<MEChannelIngredient> ingredients,
        MEChannelIngredient ingredientToMatch) {
        for (MEChannelIngredient ingredient : ingredients) {
            return ingredient;
        }
        return null;
    }

    @Override
    public String getDisplayName(MEChannelIngredient ingredient) {
        return "ME Channel";
    }

    @Override
    public String getUniqueId(MEChannelIngredient ingredient) {
        return "me_channel";
    }

    @Override
    public String getWildcardId(MEChannelIngredient ingredient) {
        return "me_channel";
    }

    @Override
    public String getModId(MEChannelIngredient ingredient) {
        return Tags.MOD_ID;
    }

    @Override
    public String getDisplayModId(MEChannelIngredient ingredient) {
        return Tags.MOD_NAME;
    }

    @Override
    public String getResourceId(MEChannelIngredient ingredient) {
        return "me_channel";
    }

    @Override
    public MEChannelIngredient copyIngredient(MEChannelIngredient ingredient) {
        return new MEChannelIngredient(ingredient.getAmount());
    }

    @Override
    public String getErrorInfo(@Nullable MEChannelIngredient ingredient) {
        return "MMCE Complement ME channel ingredient";
    }
}
