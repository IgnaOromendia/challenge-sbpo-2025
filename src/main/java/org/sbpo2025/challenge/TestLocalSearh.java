package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestLocalSearh {
    public static void main(String[] args) {
        test01AddToEmptySolution();
        test02CannotAddBecauseOfCapacity();
        test03SwapToFitOrder();
        test04NoSwapOrAddPossible();
        test05SwapPossibleOrder();
        test06PossibleSwapButDoesNotDoItBecauseOfUB();
        test07AddsToEmptyAndSwap();
        System.out.println("All tests passed.");
    }


    private static void test01AddToEmptySolution() {
        List<Map<Integer, Integer>> orders = new ArrayList<>();
        List<Map<Integer, Integer>> aisles = new ArrayList<>();

        Map<Integer, Integer> order = Map.of(0,1);
        Map<Integer, Integer> aisle = Map.of(0,1);

        orders.add(order);
        aisles.add(aisle);

        List<Integer> usedOrders = new ArrayList<>();
        List<Integer> usedAisles = List.of(0);

        LocalSearcher localSearcher = new LocalSearcher(orders, aisles, 1, 0, 10);

        double solution = localSearcher.search(usedOrders, usedAisles);

        assert solution == 1;
        assert usedOrders.size() == 1;
        assert usedOrders.contains(0);

    }

    private static void test02CannotAddBecauseOfCapacity() {
        List<Map<Integer, Integer>> orders = new ArrayList<>();
        List<Map<Integer, Integer>> aisles = new ArrayList<>();

        Map<Integer, Integer> order = Map.of(0, 5);
        Map<Integer, Integer> aisle = Map.of(0, 1);  // not enough space

        orders.add(order);
        aisles.add(aisle);

        List<Integer> usedOrders = new ArrayList<>();
        List<Integer> usedAisles = List.of(0);

        LocalSearcher localSearcher = new LocalSearcher(orders, aisles, 1, 0, 10);
        double solution = localSearcher.search(usedOrders, usedAisles);

        assert solution == 0 : "Expected no improvement";
        assert usedOrders.isEmpty();
    }

    private static void test03SwapToFitOrder() {
        List<Map<Integer, Integer>> orders = new ArrayList<>();
        List<Map<Integer, Integer>> aisles = new ArrayList<>();

        Map<Integer, Integer> order1 = Map.of(0, 2);
        Map<Integer, Integer> order2 = Map.of(0, 1);
        Map<Integer, Integer> aisle = Map.of(0, 2);

        orders.add(order1); // not used
        orders.add(order2); // used
        aisles.add(aisle);

        List<Integer> usedOrders = new ArrayList<>(List.of(1)); // already using order2
        List<Integer> usedAisles = List.of(0);

        LocalSearcher localSearcher = new LocalSearcher(orders, aisles, 1, 0, 10);
        double solution = localSearcher.search(usedOrders, usedAisles);

        assert solution == 2 : "Expected swap success";
        assert usedOrders.size() == 1;
        assert usedOrders.contains(0) : "Expected order1 to be selected";
    }

    private static void test04NoSwapOrAddPossible() {
        List<Map<Integer, Integer>> orders = new ArrayList<>();
        List<Map<Integer, Integer>> aisles = new ArrayList<>();

        Map<Integer, Integer> order1 = Map.of(0, 2, 1,2);
        Map<Integer, Integer> order2 = Map.of(0, 1, 1,2);
        Map<Integer, Integer> aisle = Map.of(0, 1, 1, 2);

        orders.add(order1);
        orders.add(order2);
        aisles.add(aisle);

        List<Integer> usedOrders = new ArrayList<>(List.of(1));
        List<Integer> usedAisles = List.of(0);

        LocalSearcher localSearcher = new LocalSearcher(orders, aisles, 2, 0, 10);
        double solution = localSearcher.search(usedOrders, usedAisles);

        assert solution == 3 : "Expected no change";
        assert usedOrders.size() == 1;
        assert usedOrders.contains(1);
    }

    private static void test05SwapPossibleOrder() {
        List<Map<Integer, Integer>> orders = new ArrayList<>();
        List<Map<Integer, Integer>> aisles = new ArrayList<>();

        Map<Integer, Integer> order1 = Map.of(0, 2, 1,2);
        Map<Integer, Integer> order2 = Map.of(0, 1, 1,2);
        Map<Integer, Integer> aisle = Map.of(0, 2, 1, 2);

        orders.add(order1);
        orders.add(order2);
        aisles.add(aisle);

        List<Integer> usedOrders = new ArrayList<>(List.of(1));
        List<Integer> usedAisles = List.of(0);

        LocalSearcher localSearcher = new LocalSearcher(orders, aisles, 2, 0, 10);
        double solution = localSearcher.search(usedOrders, usedAisles);

        assert solution == 4 : "Expected no change";
        assert usedOrders.size() == 1;
        assert usedOrders.contains(0);
    }

    private static void test06PossibleSwapButDoesNotDoItBecauseOfUB() {
        List<Map<Integer, Integer>> orders = new ArrayList<>();
        List<Map<Integer, Integer>> aisles = new ArrayList<>();

        Map<Integer, Integer> order1 = Map.of(0, 2, 1,2);
        Map<Integer, Integer> order2 = Map.of(0, 1, 1,2);
        Map<Integer, Integer> aisle = Map.of(0, 2, 1, 2);

        orders.add(order1);
        orders.add(order2);
        aisles.add(aisle);

        List<Integer> usedOrders = new ArrayList<>(List.of(1));
        List<Integer> usedAisles = List.of(0);

        LocalSearcher localSearcher = new LocalSearcher(orders, aisles, 2, 0, 3);
        double solution = localSearcher.search(usedOrders, usedAisles);

        assert solution == 3 : "Expected no change";
        assert usedOrders.size() == 1;
        assert usedOrders.contains(1);
    }

    private static void test07AddsToEmptyAndSwap() {
        List<Map<Integer, Integer>> orders = new ArrayList<>();
        List<Map<Integer, Integer>> aisles = new ArrayList<>();

        Map<Integer, Integer> order1 = Map.of(0, 1, 1,1);
        Map<Integer, Integer> order2 = Map.of(0, 1, 1,2);
        Map<Integer, Integer> aisle = Map.of(0, 2, 1, 2);

        orders.add(order1);
        orders.add(order2);
        aisles.add(aisle);

        List<Integer> usedOrders = new ArrayList<>();
        List<Integer> usedAisles = List.of(0);

        LocalSearcher localSearcher = new LocalSearcher(orders, aisles, 2, 0, 10);
        double solution = localSearcher.search(usedOrders, usedAisles);

        assert solution == 3 : "Expected no change";
        assert usedOrders.size() == 1;
        assert usedOrders.contains(1);
    }
}
