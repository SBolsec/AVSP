import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PCY {

    public static void main(String[] args) throws IOException {
        int b, threshold;

        final List<List<Integer>> baskets = new ArrayList<>();
        final Map<Integer, Integer> itemCount = new HashMap<>();
        final Map<Integer, Integer> compartments = new HashMap<>();
        final Map<List<Integer>, Integer> pairs = new HashMap<>();

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            int n = Integer.parseInt(reader.readLine());
            double s = Double.parseDouble(reader.readLine());
            b = Integer.parseInt(reader.readLine());
            threshold = (int) Math.floor(s * n);

            for (int i = 0; i < n; i++) {
                final List<Integer> basket = Arrays.stream(reader.readLine().trim().split(" "))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

                baskets.add(basket);
                basket.forEach(item -> itemCount.compute(item, (key, val) -> val == null ? 1 : val + 1));
            }
        }

        final int itemCountSize = itemCount.size();

        for (final List<Integer> basket : baskets) {
            final int basketSize = basket.size();
            for (int i = 0; i < basketSize; i++) {
                for (int j = i + 1; j < basketSize; j++) {
                    final int item1 = basket.get(i);
                    final int item2 = basket.get(j);

                    if (itemCount.get(item1) >= threshold && itemCount.get(item2) >= threshold) {
                        final int k = (item1 * itemCountSize + item2) % b;
                        compartments.compute(k, (key, val) -> val == null ? 1 : val + 1);
                    }
                }
            }
        }

        for (final List<Integer> basket : baskets) {
            final int basketSize = basket.size();
            for (int i = 0; i < basketSize; i++) {
                for (int j = i + 1; j < basketSize; j++) {
                    final int item1 = basket.get(i);
                    final int item2 = basket.get(j);

                    if (itemCount.get(item1) >= threshold && itemCount.get(item2) >= threshold) {
                        final int k = (item1 * itemCountSize + item2) % b;
                        if (compartments.getOrDefault(k, 0) >= threshold) {
                            pairs.compute(List.of(item1, item2), (key, val) -> val == null ? 1 : val + 1);
                        }
                    }
                }
            }
        }

        System.out.println(itemCountSize * (itemCountSize - 1) / 2);
        System.out.println(pairs.size());
        pairs.values().stream().sorted(Comparator.reverseOrder()).forEach(System.out::println);
    }

}
