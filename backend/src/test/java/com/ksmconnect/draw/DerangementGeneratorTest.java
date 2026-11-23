package com.ksmconnect.draw;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DerangementGeneratorTest {

    private final DerangementGenerator generator = new DerangementGenerator(new SecureRandom());

    @ParameterizedTest(name = "{0} participants x {1} draws")
    @CsvSource({"2, 2000", "3, 5000", "5, 5000", "10, 3000", "30, 3000", "100, 500", "1000, 50"})
    void everyGeneratedDrawSatisfiesAllInvariants(int participantCount, int repetitions) {
        List<UUID> participants = participants(participantCount);
        for (int i = 0; i < repetitions; i++) {
            List<Pairing<UUID>> pairings = generator.generate(participants);
            assertValidDerangement(participants, pairings);
        }
    }

    @Test
    void twoParticipantsAlwaysGiftEachOther() {
        List<String> participants = List.of("anna", "piotr");
        List<Pairing<String>> pairings = generator.generate(participants);
        assertThat(pairings).containsExactlyInAnyOrder(new Pairing<>("anna", "piotr"), new Pairing<>("piotr", "anna"));
    }

    @Test
    void threeParticipantsProduceBothCyclesWithRoughlyEqualFrequency() {
        List<String> participants = List.of("a", "b", "c");
        Map<String, Integer> occurrences = new HashMap<>();
        int runs = 20_000;
        for (int i = 0; i < runs; i++) {
            String recipientOfA = generator.generate(participants).stream()
                    .filter(pairing -> pairing.giver().equals("a")).findFirst().orElseThrow().recipient();
            occurrences.merge(recipientOfA, 1, Integer::sum);
        }
        assertThat(occurrences).containsOnlyKeys("b", "c");
        assertThat(occurrences.get("b")).isBetween((int) (runs * 0.46), (int) (runs * 0.54));
    }

    @Test
    void fourParticipantsCoverAllNineDerangementsRoughlyUniformly() {
        List<Integer> participants = List.of(0, 1, 2, 3);
        Map<List<Integer>, Integer> counts = new HashMap<>();
        int runs = 45_000;
        for (int i = 0; i < runs; i++) {
            List<Integer> mapping = new ArrayList<>(List.of(-1, -1, -1, -1));
            generator.generate(participants).forEach(pairing -> mapping.set(pairing.giver(), pairing.recipient()));
            counts.merge(mapping, 1, Integer::sum);
        }
        assertThat(counts).hasSize(9);
        double expected = runs / 9.0;
        double chiSquare = counts.values().stream().mapToDouble(observed -> Math.pow(observed - expected, 2) / expected).sum();
        assertThat(chiSquare).isLessThan(35.0);
    }

    @Test
    void seededGeneratorIsDeterministic() {
        List<UUID> participants = participants(30);
        List<Pairing<UUID>> first = new DerangementGenerator(new Random(42)).generate(participants);
        List<Pairing<UUID>> second = new DerangementGenerator(new Random(42)).generate(participants);
        assertThat(first).isEqualTo(second);
    }

    @Test
    void rejectsFewerThanTwoParticipants() {
        assertThatThrownBy(() -> generator.generate(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generator.generate(List.of("solo"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDuplicateParticipants() {
        assertThatThrownBy(() -> generator.generate(List.of("a", "b", "a")))
                .isInstanceOf(DrawInvariants.DrawInvariantViolation.class);
    }

    @Test
    void doesNotDependOnCallerMutatingInput() {
        List<String> participants = new ArrayList<>(List.of("a", "b", "c", "d"));
        List<Pairing<String>> pairings = generator.generate(participants);
        participants.add("e");
        assertThat(pairings).hasSize(4);
    }

    @Test
    void failsLoudlyWhenRandomSourceCannotProduceDerangement() {
        DerangementGenerator broken = new DerangementGenerator(new Random() {
            @Override
            public int nextInt(int bound) {
                return bound - 1;
            }
        });
        assertThatThrownBy(() -> broken.generate(List.of("a", "b", "c")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("derangement");
    }

    @Test
    void concurrentGenerationRemainsCorrect() throws Exception {
        List<UUID> participants = participants(30);
        ConcurrentLinkedQueue<List<Pairing<UUID>>> results = new ConcurrentLinkedQueue<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            List<Future<?>> futures = IntStream.range(0, 2000)
                    .<Future<?>>mapToObj(i -> executor.submit(() -> results.add(generator.generate(participants))))
                    .toList();
            for (Future<?> future : futures) {
                future.get();
            }
        }
        assertThat(results).hasSize(2000);
        results.forEach(pairings -> assertValidDerangement(participants, pairings));
    }

    private static List<UUID> participants(int count) {
        return IntStream.range(0, count).mapToObj(i -> UUID.randomUUID()).toList();
    }

    private static <T> void assertValidDerangement(List<T> participants, List<Pairing<T>> pairings) {
        assertThat(pairings).hasSize(participants.size());
        Set<T> givers = new HashSet<>();
        Set<T> recipients = new HashSet<>();
        for (Pairing<T> pairing : pairings) {
            assertThat(pairing.giver()).isNotEqualTo(pairing.recipient());
            assertThat(givers.add(pairing.giver())).isTrue();
            assertThat(recipients.add(pairing.recipient())).isTrue();
        }
        assertThat(givers).containsExactlyInAnyOrderElementsOf(participants);
        assertThat(recipients).containsExactlyInAnyOrderElementsOf(participants);
    }
}
