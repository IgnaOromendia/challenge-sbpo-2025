package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestGreedyCovering {
    public static void main(String[] args) {
        test01AddOrdersToUniqueAisle();
        test02AddOrdersToUniqueAisleButSomeOrdersCannotBePicked();
        test02AddOrdersToUniqueAisleButSomeOrdersCannotBePickedBecauseOfUB();
        test04OnlyTwoAislesButOneIsBetter();
        test05AisleScoreIsAdaptiveInSimpleCase();
        test05AisleScoreIsAdaptiveInMoreComplexCase();
        test06AisleScoreIsAdaptiveInMoreMOREComplexCase();
        System.out.println("All tests passed.");
    }

    private static void test01AddOrdersToUniqueAisle() {
        List<Map<Integer, Integer>> orders = List.of(Map.of(0, 1), Map.of(0, 1));
        List<Map<Integer, Integer>> aisles = List.of(Map.of(0, 2));

        GreedyCovering greedyCovering = new GreedyCovering(orders, aisles, 1, 0, 4);

        List<Integer> used_orders = new ArrayList<>();
        List<Integer> used_aisles = new ArrayList<>();

        double result = greedyCovering.solve(used_orders, used_aisles);

        assert result == 2.0;
        assert used_orders.size() == 2;
        assert used_orders.contains(0);
        assert used_orders.contains(1);
        assert used_aisles.size() == 1;
        assert used_aisles.contains(0);

        System.out.println("Test 1 Ok");
    }

    private static void test02AddOrdersToUniqueAisleButSomeOrdersCannotBePicked() {
        List<Map<Integer, Integer>> orders = List.of(Map.of(0, 1), Map.of(0, 1), Map.of(0, 1));
        List<Map<Integer, Integer>> aisles = List.of(Map.of(0, 2));

        GreedyCovering greedyCovering = new GreedyCovering(orders, aisles, 1, 0, 4);

        List<Integer> used_orders = new ArrayList<>();
        List<Integer> used_aisles = new ArrayList<>();

        double result = greedyCovering.solve(used_orders, used_aisles);

        assert result == 2.0;
        assert used_orders.size() == 2;
        assert used_orders.contains(1);
        assert used_orders.contains(2);
        assert used_aisles.size() == 1;
        assert used_aisles.contains(0);

        System.out.println("Test 2 Ok");
    }

    private static void test02AddOrdersToUniqueAisleButSomeOrdersCannotBePickedBecauseOfUB() {
        List<Map<Integer, Integer>> orders = List.of(Map.of(0, 1), Map.of(0, 1), Map.of(0, 1));
        List<Map<Integer, Integer>> aisles = List.of(Map.of(0, 2));

        GreedyCovering greedyCovering = new GreedyCovering(orders, aisles, 1, 0, 1);

        List<Integer> used_orders = new ArrayList<>();
        List<Integer> used_aisles = new ArrayList<>();

        double result = greedyCovering.solve(used_orders, used_aisles);

        assert result == 1.0;
        assert used_orders.size() == 1;
        assert used_orders.contains(2);
        assert used_aisles.size() == 1;
        assert used_aisles.contains(0);

        System.out.println("Test 3 Ok");
    }

    private static void test04OnlyTwoAislesButOneIsBetter() {
        List<Map<Integer, Integer>> orders = List.of(Map.of(0, 1), Map.of(0, 1), Map.of(0, 1));
        List<Map<Integer, Integer>> aisles = List.of(Map.of(0, 1), Map.of(0, 2));

        GreedyCovering greedyCovering = new GreedyCovering(orders, aisles, 1, 0, 3);

        List<Integer> used_orders = new ArrayList<>();
        List<Integer> used_aisles = new ArrayList<>();

        double result = greedyCovering.solve(used_orders, used_aisles);

        assert result == 2.0;
        assert used_orders.size() == 2;
        assert used_orders.contains(2);
        assert used_orders.contains(1);
        assert used_aisles.size() == 1;
        assert used_aisles.contains(1);

        System.out.println("Test 4 Ok");
    }

    private static void test05AisleScoreIsAdaptiveInSimpleCase() {
        List<Map<Integer, Integer>> orders = List.of(Map.of(0, 1), Map.of(0, 1), Map.of(0, 1), Map.of(0, 1),
                                                        Map.of(1, 1), Map.of(1, 1));
        List<Map<Integer, Integer>> aisles = List.of(Map.of(0, 4), Map.of(0, 2), Map.of(1, 2));

        GreedyCovering greedyCovering = new GreedyCovering(orders, aisles, 1, 5, 10);

        List<Integer> used_orders = new ArrayList<>();
        List<Integer> used_aisles = new ArrayList<>();

        double result = greedyCovering.solve(used_orders, used_aisles);

        assert result == 3.0;
        assert used_orders.size() == 6;
        assert used_aisles.size() == 2;
        assert used_aisles.contains(0);
        assert used_aisles.contains(2);

        System.out.println("Test 5 Ok");
    }

    private static void test05AisleScoreIsAdaptiveInMoreComplexCase() {
        List<Map<Integer, Integer>> orders = List.of(Map.of(0, 1), Map.of(0, 1), Map.of(0, 1), Map.of(0, 1), Map.of(0, 1), Map.of(0, 1), Map.of(0, 1),
                                                        Map.of(1, 1), Map.of(1, 1));
        List<Map<Integer, Integer>> aisles = List.of(Map.of(0, 4), Map.of(0, 3), Map.of(1, 2));

        GreedyCovering greedyCovering = new GreedyCovering(orders, aisles, 1, 5, 10);

        List<Integer> used_orders = new ArrayList<>();
        List<Integer> used_aisles = new ArrayList<>();

        double result = greedyCovering.solve(used_orders, used_aisles);

        assert result == 3.5;
        assert used_orders.size() == 7;
        assert used_aisles.size() == 2;
        assert used_aisles.contains(0);
        assert used_aisles.contains(1);

        System.out.println("Test 6 Ok");
    }

    private static void test06AisleScoreIsAdaptiveInMoreMOREComplexCase() {
        List<Map<Integer, Integer>> orders = List.of(
            Map.of(0, 1), Map.of(0, 1), Map.of(0, 1), Map.of(0, 1), Map.of(0, 1),
            Map.of(1, 1), Map.of(1, 1),
            Map.of(2, 1), Map.of(2, 1), Map.of(2, 1), Map.of(2, 1),
            Map.of(3, 1), Map.of(3, 1), Map.of(3, 1), Map.of(3, 1), Map.of(3, 1), Map.of(3, 1));

        List<Map<Integer, Integer>> aisles = List.of(
            Map.of(0, 4, 4, 1, 1, 1),  // Score inicial: 5
            Map.of(0, 3, 2, 3, 1, 7),  // Score inicial: 8
            Map.of(4, 8, 0, 3,  3, 3), // Score inicial: 6
            Map.of(0, 2, 1, 2, 2, 2)); // Score inicial: 6

        // Primero se elige el pasillo 1. El estado luego de esa eleccion es
        // #Ordenes elem 0: 2
        // #Ordenes elem 1: 0
        // #Ordenes elem 2: 1
        // #Ordenes elem 3: 6
        // Por lo tanto, el mejor score tras agregar el pasillo 1 lo tiene el pasillo 2 (5)

        GreedyCovering greedyCovering = new GreedyCovering(orders, aisles, 1, 12, 30);

        List<Integer> used_orders = new ArrayList<>();
        List<Integer> used_aisles = new ArrayList<>();

        double result = greedyCovering.solve(used_orders, used_aisles);

        assert result == 6.5;
        assert used_orders.size() == 13;
        assert used_aisles.size() == 2;
        assert used_aisles.contains(1);
        assert used_aisles.contains(2);

        System.out.println("Test 7 Ok");
    }
}
