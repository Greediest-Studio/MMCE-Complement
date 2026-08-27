package net.edwin.mmcecomplement.redstoneinterface;

import java.util.Objects;

/** Immutable definition of one named redstone value exposed by a machine. */
public final class RedstoneValueDefinition {

    public static final int OPERATOR_MAX = 0;
    public static final int OPERATOR_MIN = 1;
    public static final int OPERATOR_SUM = 2;

    private final String name;
    private final int operator;

    public RedstoneValueDefinition(String name, int operator) {
        this.name = Objects.requireNonNull(name, "name");
        if (!isValidOperator(operator)) {
            throw new IllegalArgumentException("Unknown redstone operator: " + operator);
        }
        this.operator = operator;
    }

    public String getName() {
        return name;
    }

    public int getOperator() {
        return operator;
    }

    public static boolean isValidOperator(int operator) {
        return operator >= OPERATOR_MAX && operator <= OPERATOR_SUM;
    }
}
