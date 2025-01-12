package io.github.marco4413.pemu.compiler.parser;

import io.github.marco4413.pemu.instructions.Instruction;
import org.jetbrains.annotations.NotNull;

public final class InstructionNode extends ValueNode {
    private final Instruction INSTRUCTION;

    protected InstructionNode(int opcode, @NotNull Instruction instruction) {
        super(opcode);
        INSTRUCTION = instruction;
    }

    public @NotNull Instruction getInstruction() {
        return INSTRUCTION;
    }

    @Override
    public @NotNull NodeType getType() {
        return NodeType.INSTRUCTION;
    }
}
