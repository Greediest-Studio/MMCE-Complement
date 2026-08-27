package net.edwin.mmcecomplement.compat.jei.mechannel;

/** Dedicated JEI value type, kept separate from other integer requirements. */
public final class MEChannelIngredient {
    private final int amount;

    public MEChannelIngredient(int amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }
}
