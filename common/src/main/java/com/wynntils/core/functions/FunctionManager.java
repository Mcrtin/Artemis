/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.functions;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.managers.CoreManager;
import com.wynntils.functions.EnvironmentFunctions;
import com.wynntils.functions.MinecraftFunctions;
import com.wynntils.functions.WorldFunction;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;

/** Manage all built-in {@link Function}s */
public final class FunctionManager extends CoreManager {
    private static final List<Function<?>> FUNCTIONS = new ArrayList<>();
    private static final Set<ActiveFunction<?>> ENABLED_FUNCTIONS = new HashSet<>();

    private static void registerFunction(Function<?> function) {
        FUNCTIONS.add(function);
        if (function instanceof ActiveFunction<?> activeFunction) {
            activeFunction.init();
        }
    }

    public static List<Function<?>> getFunctions() {
        return FUNCTIONS;
    }

    public static boolean enableFunction(Function<?> function) {
        if (!(function instanceof ActiveFunction<?> activeFunction)) return true;

        WynntilsMod.getEventBus().register(activeFunction);

        boolean enableSucceeded = activeFunction.onEnable();

        if (!enableSucceeded) {
            WynntilsMod.getEventBus().unregister(activeFunction);
        }
        ENABLED_FUNCTIONS.add(activeFunction);
        return enableSucceeded;
    }

    public static void disableFunction(Function<?> function) {
        if (!(function instanceof ActiveFunction<?> activeFunction)) return;

        WynntilsMod.getEventBus().unregister(activeFunction);
        activeFunction.onDisable();
        ENABLED_FUNCTIONS.remove(activeFunction);
    }

    public static boolean isEnabled(Function<?> function) {
        if (!(function instanceof ActiveFunction<?>)) return true;

        return (ENABLED_FUNCTIONS.contains(function));
    }

    public static Optional<Function<?>> forName(String functionName) {
        return FunctionManager.getFunctions().stream()
                .filter(function -> hasName(function, functionName))
                .findFirst();
    }

    private static boolean hasName(Function<?> function, String name) {
        if (function.getName().equalsIgnoreCase(name)) return true;
        for (String alias : function.getAliases()) {
            if (alias.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public static Component getSimpleValueString(
            Function<?> function, String argument, ChatFormatting color, boolean includeName) {
        MutableComponent header = includeName
                ? new TextComponent(function.getTranslatedName() + ": ").withStyle(ChatFormatting.WHITE)
                : new TextComponent("");

        Object value = function.getValue(argument);
        if (value == null) {
            return header.append(new TextComponent("N/A").withStyle(ChatFormatting.RED));
        }

        String formattedValue = format(value);

        return header.append(new TextComponent(formattedValue).withStyle(color));
    }

    private static String format(Object value) {
        if (value instanceof Number number) {
            return NumberFormat.getInstance().format(number);
        }
        return value.toString();
    }

    /**
     * Return a string, based on the template, with values filled in from the referenced
     * functions.
     */
    public static Component getStringFromTemplate(String template) {
        // FIXME: implement template parser
        return new TextComponent(template);
    }

    /**
     * Return a list of all functions referenced in a template string
     */
    public static List<Function<?>> getFunctionsInTemplate(String template) {
        // FIXME: implement template parser
        return List.of();
    }

    public static void init() {
        registerFunction(new WorldFunction());

        registerFunction(new MinecraftFunctions.XFunction());
        registerFunction(new MinecraftFunctions.YFunction());
        registerFunction(new MinecraftFunctions.ZFunction());
        registerFunction(new MinecraftFunctions.DirFunction());
        registerFunction(new MinecraftFunctions.FpsFunction());

        registerFunction(new EnvironmentFunctions.ClockFunction());
        registerFunction(new EnvironmentFunctions.ClockmFunction());
        registerFunction(new EnvironmentFunctions.MemMaxFunction());
        registerFunction(new EnvironmentFunctions.MemUsedFunction());
        registerFunction(new EnvironmentFunctions.MemPctFunction());
    }
}