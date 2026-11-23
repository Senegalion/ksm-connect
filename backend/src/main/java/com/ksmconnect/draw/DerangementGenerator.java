package com.ksmconnect.draw;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

public class DerangementGenerator {

    static final int MAX_ATTEMPTS = 10_000;

    private final RandomGenerator random;

    public DerangementGenerator() {
        this(new SecureRandom());
    }

    public DerangementGenerator(RandomGenerator random) {
        this.random = random;
    }

    public <T> List<Pairing<T>> generate(List<T> participants) {
        List<T> snapshot = List.copyOf(participants);
        if (snapshot.size() < 2) {
            throw new IllegalArgumentException("At least two participants are required");
        }
        int[] permutation = randomDerangement(snapshot.size());
        List<Pairing<T>> pairings = new ArrayList<>(snapshot.size());
        for (int i = 0; i < snapshot.size(); i++) {
            pairings.add(new Pairing<>(snapshot.get(i), snapshot.get(permutation[i])));
        }
        DrawInvariants.verify(snapshot, pairings);
        return List.copyOf(pairings);
    }

    int[] randomDerangement(int size) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int[] permutation = shuffledIdentity(size);
            if (!hasFixedPoint(permutation)) {
                return permutation;
            }
        }
        throw new IllegalStateException("Unable to generate a derangement");
    }

    private int[] shuffledIdentity(int size) {
        int[] values = new int[size];
        for (int i = 0; i < size; i++) {
            values[i] = i;
        }
        for (int i = size - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = values[i];
            values[i] = values[j];
            values[j] = swap;
        }
        return values;
    }

    private static boolean hasFixedPoint(int[] permutation) {
        for (int i = 0; i < permutation.length; i++) {
            if (permutation[i] == i) {
                return true;
            }
        }
        return false;
    }
}
