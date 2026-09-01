package net.edwin.mmcecomplement.catalyst;

/** Implemented by every catalyst requirement so RecipePrimer can propagate its limit. */
public interface CatalystLimitAware {
    void setMaxCatalyst(int max);
    int getMaxCatalyst();
}
