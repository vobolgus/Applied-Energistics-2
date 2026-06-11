/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2025, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.neoforge.config;

import java.util.ArrayList;
import java.util.List;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import appeng.core.AEConfig;
import appeng.core.config.ConfigStore;

/**
 * {@link ConfigStore} backed by NeoForge's {@link ModConfigSpec} machinery. Produces the exact same TOML files (names,
 * sections, comments, ranges) as the previous direct use of {@link ModConfigSpec.Builder} in {@link AEConfig}.
 */
public final class NeoForgeConfigStore implements ConfigStore {
    private final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
    private final List<Runnable> loadListeners = new ArrayList<>();
    private ModConfigSpec spec;

    /**
     * Creates the client/common stores, lets {@link AEConfig} build its structure into them and registers the resulting
     * specs with the given mod container.
     */
    public static void initConfigs(ModContainer container) {
        var clientStore = new NeoForgeConfigStore();
        var commonStore = new NeoForgeConfigStore();
        AEConfig.register(clientStore, commonStore);
        clientStore.register(container, ModConfig.Type.CLIENT);
        commonStore.register(container, ModConfig.Type.COMMON);
    }

    private void register(ModContainer container, ModConfig.Type type) {
        this.spec = builder.build();
        container.registerConfig(type, spec);
        container.getEventBus().addListener((ModConfigEvent.Loading evt) -> {
            if (evt.getConfig().getSpec() == spec) {
                notifyLoadListeners();
            }
        });
        container.getEventBus().addListener((ModConfigEvent.Reloading evt) -> {
            if (evt.getConfig().getSpec() == spec) {
                notifyLoadListeners();
            }
        });
    }

    private void notifyLoadListeners() {
        for (var listener : loadListeners) {
            listener.run();
        }
    }

    @Override
    public void comment(String comment) {
        builder.comment(comment);
    }

    @Override
    public void push(String section) {
        builder.push(section);
    }

    @Override
    public void pop() {
        builder.pop();
    }

    @Override
    public Value<Boolean> defineBoolean(String name, boolean defaultValue) {
        return new ValueWrapper<>(builder.define(name, defaultValue));
    }

    @Override
    public Value<Integer> defineInt(String name, int defaultValue, int min, int max) {
        return new ValueWrapper<>(builder.defineInRange(name, defaultValue, min, max));
    }

    @Override
    public Value<Double> defineDouble(String name, double defaultValue, double min, double max) {
        return new ValueWrapper<>(builder.defineInRange(name, defaultValue, min, max));
    }

    @Override
    public <T extends Enum<T>> Value<T> defineEnum(String name, T defaultValue) {
        return new ValueWrapper<>(builder.defineEnum(name, defaultValue));
    }

    @Override
    public void save() {
        spec.save();
    }

    @Override
    public void onLoadOrReload(Runnable listener) {
        loadListeners.add(listener);
    }

    private record ValueWrapper<T>(ModConfigSpec.ConfigValue<T> value) implements Value<T> {
        @Override
        public T get() {
            return value.get();
        }

        @Override
        public void set(T newValue) {
            value.set(newValue);
        }
    }
}
