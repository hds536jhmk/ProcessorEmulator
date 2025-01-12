package io.github.marco4413.pemu.compiler.parser;

import io.github.marco4413.pemu.utils.IPIntSupplier;
import org.jetbrains.annotations.NotNull;

public class ValueNode implements INode {

    private final IPIntSupplier VALUE_SUPPLIER;

    protected ValueNode(int value) {
        this(data -> value);
    }

    protected ValueNode(@NotNull IPIntSupplier valueSupplier) {
        VALUE_SUPPLIER = valueSupplier;
    }

    public int getValue() {
        return VALUE_SUPPLIER.get();
    }

    @Override
    public @NotNull NodeType getType() {
        return NodeType.VALUE;
    }
}
