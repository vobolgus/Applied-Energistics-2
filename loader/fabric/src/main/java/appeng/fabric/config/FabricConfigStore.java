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

package appeng.fabric.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loader.api.FabricLoader;

import appeng.core.AEConfig;
import appeng.core.config.ConfigStore;

/**
 * {@link ConfigStore} backed by night-config TOML files, reading and writing the same {@code config/ae2-client.toml} /
 * {@code config/ae2-common.toml} files (same sections, value paths and types) that NeoForge's {@code ModConfigSpec}
 * produces.
 * <p>
 * <strong>Behavior note (vs. NeoForge):</strong> there is no file watcher; config changes on disk are only picked up at
 * startup (the {@code onLoadOrReload} listeners fire once after the initial load). In-game changes via {@code save()}
 * work as on NeoForge.
 */
public final class FabricConfigStore implements ConfigStore {
    private static final Logger LOG = LoggerFactory.getLogger(FabricConfigStore.class);

    private final List<String> currentSection = new ArrayList<>();
    private final List<Definition<?>> definitions = new ArrayList<>();
    private final List<Runnable> loadListeners = new ArrayList<>();
    private final List<SectionComment> sectionComments = new ArrayList<>();
    @Nullable
    private String pendingComment;
    @Nullable
    private CommentedFileConfig config;

    /**
     * Creates the client/common stores, lets {@link AEConfig} build its structure into them and loads the backing
     * files.
     */
    public static void initConfigs() {
        var clientStore = new FabricConfigStore();
        var commonStore = new FabricConfigStore();
        AEConfig.register(clientStore, commonStore);
        var configDir = FabricLoader.getInstance().getConfigDir();
        clientStore.load(configDir.resolve("ae2-client.toml"));
        commonStore.load(configDir.resolve("ae2-common.toml"));
    }

    private void load(Path file) {
        var config = CommentedFileConfig.builder(file).sync().build();
        config.load();

        for (var definition : definitions) {
            definition.correct(config);
            config.setComment(definition.path, definition.buildComment());
        }
        for (var sectionComment : sectionComments) {
            config.setComment(sectionComment.path, sectionComment.comment);
        }

        this.config = config;
        // Always write back once so defaults/comments materialize in fresh files
        config.save();

        for (var listener : loadListeners) {
            listener.run();
        }
    }

    @Override
    public void comment(String comment) {
        this.pendingComment = comment;
    }

    @Override
    public void push(String section) {
        currentSection.add(section);
        if (pendingComment != null) {
            sectionComments.add(new SectionComment(String.join(".", currentSection), pendingComment));
            pendingComment = null;
        }
    }

    @Override
    public void pop() {
        currentSection.remove(currentSection.size() - 1);
    }

    @Override
    public Value<Boolean> defineBoolean(String name, boolean defaultValue) {
        return define(new Definition<>(path(name), defaultValue, null,
                raw -> raw instanceof Boolean b ? b : null,
                value -> value));
    }

    @Override
    public Value<Integer> defineInt(String name, int defaultValue, int min, int max) {
        return define(new Definition<>(path(name), defaultValue, rangeComment(min, max),
                raw -> {
                    if (raw instanceof Number number) {
                        var value = number.intValue();
                        return value >= min && value <= max ? value : null;
                    }
                    return null;
                },
                value -> value));
    }

    @Override
    public Value<Double> defineDouble(String name, double defaultValue, double min, double max) {
        return define(new Definition<>(path(name), defaultValue, rangeComment(min, max),
                raw -> {
                    if (raw instanceof Number number) {
                        var value = number.doubleValue();
                        return value >= min && value <= max ? value : null;
                    }
                    return null;
                },
                value -> value));
    }

    @Override
    public <T extends Enum<T>> Value<T> defineEnum(String name, T defaultValue) {
        var enumClass = defaultValue.getDeclaringClass();
        return define(new Definition<>(path(name), defaultValue,
                "Allowed Values: " + String.join(", ",
                        java.util.Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).toList()),
                raw -> {
                    if (raw instanceof String s) {
                        try {
                            return Enum.valueOf(enumClass, s.toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException ignored) {
                            return null;
                        }
                    }
                    return null;
                },
                Enum::name));
    }

    @Override
    public void save() {
        if (config != null) {
            config.save();
        }
    }

    @Override
    public void onLoadOrReload(Runnable listener) {
        loadListeners.add(listener);
    }

    private String path(String name) {
        if (currentSection.isEmpty()) {
            return name;
        }
        return String.join(".", currentSection) + "." + name;
    }

    private static String rangeComment(Number min, Number max) {
        return "Range: " + min + " ~ " + max;
    }

    private <T> Value<T> define(Definition<T> definition) {
        definition.comment = pendingComment;
        pendingComment = null;
        definitions.add(definition);
        return new ValueImpl<>(definition);
    }

    private record SectionComment(String path, String comment) {
    }

    private static final class Definition<T> {
        final String path;
        final T defaultValue;
        @Nullable
        final String typeComment;
        final Function<@Nullable Object, @Nullable T> deserializer;
        final Function<T, Object> serializer;
        @Nullable
        String comment;

        Definition(String path, T defaultValue, @Nullable String typeComment,
                Function<@Nullable Object, @Nullable T> deserializer, Function<T, Object> serializer) {
            this.path = path;
            this.defaultValue = defaultValue;
            this.typeComment = typeComment;
            this.deserializer = deserializer;
            this.serializer = serializer;
        }

        /**
         * Ensures the config contains a valid value at this path, replacing invalid/missing entries with the default
         * (mirroring ModConfigSpec's correction behavior).
         *
         * @return true if the config was changed.
         */
        boolean correct(CommentedConfig config) {
            var raw = config.get(path);
            if (deserializer.apply(raw) == null) {
                if (raw != null) {
                    LOG.warn("Correcting invalid config value {} for {} back to default {}", raw, path, defaultValue);
                }
                config.set(path, serializer.apply(defaultValue));
                return true;
            }
            return false;
        }

        @Nullable
        String buildComment() {
            if (comment != null && typeComment != null) {
                return comment + "\n" + typeComment;
            } else if (comment != null) {
                return comment;
            } else {
                return typeComment;
            }
        }
    }

    private final class ValueImpl<T> implements Value<T> {
        private final Definition<T> definition;

        ValueImpl(Definition<T> definition) {
            this.definition = definition;
        }

        @Override
        public T get() {
            var config = FabricConfigStore.this.config;
            if (config == null) {
                return definition.defaultValue;
            }
            var value = definition.deserializer.apply(config.get(definition.path));
            return value != null ? value : definition.defaultValue;
        }

        @Override
        public void set(T value) {
            var config = FabricConfigStore.this.config;
            if (config != null) {
                config.set(definition.path, definition.serializer.apply(value));
            }
        }
    }
}
