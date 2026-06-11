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

package appeng.core.config;

/**
 * Loader-neutral seam for the backing store of {@link appeng.core.AEConfig}. The loader-specific implementation (e.g.
 * {@code appeng.neoforge.config.NeoForgeConfigStore}) provides typed value handles, section structure, persistence and
 * reload notification.
 * <p>
 * The definition methods ({@link #comment}, {@link #push}, {@link #pop}, {@code define*}) mirror the builder semantics
 * of NeoForge's {@code ModConfigSpec.Builder}: a comment applies to the next defined value or section, and sections
 * nest via push/pop. Only the operations actually used by {@code AEConfig} are part of this interface.
 */
public interface ConfigStore {

    /**
     * Sets the comment to attach to the next defined value or pushed section.
     */
    void comment(String comment);

    /**
     * Enters a (possibly new) section with the given name.
     */
    void push(String section);

    /**
     * Leaves the current section.
     */
    void pop();

    Value<Boolean> defineBoolean(String name, boolean defaultValue);

    Value<Integer> defineInt(String name, int defaultValue, int min, int max);

    Value<Double> defineDouble(String name, double defaultValue, double min, double max);

    <T extends Enum<T>> Value<T> defineEnum(String name, T defaultValue);

    /**
     * Persists the current values to disk.
     */
    void save();

    /**
     * Registers a listener that is invoked whenever this store's values are (re-)loaded from disk.
     */
    void onLoadOrReload(Runnable listener);

    /**
     * Typed handle to a single config value.
     */
    interface Value<T> {
        T get();

        void set(T value);
    }
}
