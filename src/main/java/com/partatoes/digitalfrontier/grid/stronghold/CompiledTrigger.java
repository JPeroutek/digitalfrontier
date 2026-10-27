package com.partatoes.digitalfrontier.grid.stronghold;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A respawn trigger tree with its leaves numbered, so a stronghold's progress is just a counter per leaf, and with
 * each leaf's combined {@code chance}. Leaves are told apart by identity: two identical triggers in one tree count
 * separately.
 */
public final class CompiledTrigger {
    private final Trigger root;
    private final List<Trigger> leaves = new ArrayList<>();
    private final Map<Trigger, Integer> indices = new IdentityHashMap<>();
    private final Map<Trigger, Double> chances = new IdentityHashMap<>();

    public CompiledTrigger(Trigger root) {
        this.root = root;
        root.leaves(leaves);
        for (int i = 0; i < leaves.size(); i++) {
            indices.put(leaves.get(i), i);
        }
        collectChances(root, 1.0);
    }

    private void collectChances(Trigger trigger, double chance) {
        switch (trigger) {
            case Triggers.Chance c -> collectChances(c.trigger(), chance * c.chance());
            case Triggers.AnyOf anyOf -> anyOf.triggers().forEach(child -> collectChances(child, chance));
            case Triggers.AllOf allOf -> allOf.triggers().forEach(child -> collectChances(child, chance));
            case Triggers.Not not -> collectChances(not.trigger(), chance);
            case Triggers.Count count -> collectChances(count.trigger(), chance);
            default -> chances.put(trigger, chance);
        }
    }

    public Trigger root() {
        return root;
    }

    public List<Trigger> leaves() {
        return leaves;
    }

    public int index(Trigger leaf) {
        Integer index = indices.get(leaf);
        if (index == null) {
            throw new IllegalArgumentException("Not a leaf of this trigger: " + leaf);
        }
        return index;
    }

    public double chance(Trigger leaf) {
        return chances.getOrDefault(leaf, 1.0);
    }
}
