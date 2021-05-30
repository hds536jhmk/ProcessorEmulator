package io.github.hds.pemu.compiler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RegisterData extends HashMap<Integer, String> {
    protected RegisterData(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    protected RegisterData(int initialCapacity) {
        super(initialCapacity);
    }

    protected RegisterData() {
        super();
    }

    protected RegisterData(Map<? extends Integer, ? extends String> m) {
        super(m);
    }

    @Nullable String getRegisterAtAddress(int line) {
        return this.get(line);
    }

    boolean hasRegisterAtAddress(int line) {
        return this.containsKey(line);
    }

    @NotNull Integer[] getRegisterAddresses(@NotNull String registerName) {
        ArrayList<Integer> addresses = new ArrayList<>();

        this.forEach((line, name) -> {
            if (name.equals(registerName))
                addresses.add(line);
        });

        return addresses.toArray(new Integer[0]);
    }

    boolean hasRegister(@NotNull String registerName) {
        return this.containsValue(registerName);
    }
}
