package net.earthcomputer.multiconnect.impl;

import com.google.common.collect.BiMap;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.Int2ObjectBiMap;
import net.minecraft.util.registry.SimpleRegistry;

public interface ISimpleRegistry<T> {

    int getNextId();

    void setNextId(int nextId);

    Int2ObjectBiMap<T> getIndexedEntries();

    BiMap<Identifier, T> getEntries();

    default void register(T t, int id, Identifier name) {
        register(t, id, name, true);
    }

    void register(T t, int id, Identifier name, boolean sideEffects);

    default void registerInPlace(T t, int id, Identifier name) {
        registerInPlace(t, id, name, true);
    }

    void registerInPlace(T t, int id, Identifier name, boolean sideEffects);

    default void unregister(T t) {
        unregister(t, true);
    }

    void unregister(T t, boolean sideEffects);

    default void purge(T t) {
        purge(t, true);
    }

    void purge(T t, boolean sideEffects);

    void clear(boolean sideEffects);

    void addRegisterListener(IRegistryUpdateListener<T> listener);

    void addUnregisterListener(IRegistryUpdateListener<T> listener);

    SimpleRegistry<T> copy();

    void dump();
}
