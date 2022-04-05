import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CF {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.000");

    private final Integer numberOfItems;
    private final Integer numberOfUsers;
    private final List<List<Integer>> itemsTable;
    private final List<List<Integer>> usersTable;
    private final List<List<Integer>> queryList;
    private final List<List<Double>> normalizedItemRatings;
    private final List<List<Double>> normalizedUserRatings;
    private final Map<List<Integer>, Double> similarityCache = new HashMap<>();

    public CF() throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            // Parse the user-item table
            final String[] parts = reader.readLine().trim().split(" ");
            numberOfItems = Integer.parseInt(parts[0]);
            numberOfUsers = Integer.parseInt(parts[1]);

            itemsTable = new ArrayList<>(numberOfItems);
            for (int i = 0; i < numberOfItems; i++) {
                final List<Integer> itemRatings = Arrays.stream(reader.readLine().trim().split(" "))
                    .map(rating -> "X".equals(rating) ? 0 : Integer.parseInt(rating))
                    .collect(Collectors.toList());

                itemsTable.add(itemRatings);
            }

            usersTable = new ArrayList<>(numberOfUsers);
            for (int i = 0; i < numberOfUsers; i++) {
                final int index = i;
                final List<Integer> userRatings = itemsTable.stream()
                    .map(x -> x.get(index))
                    .collect(Collectors.toList());

                usersTable.add(userRatings);
            }

            // Parse the queries
            final int numberOfQueries = Integer.parseInt(reader.readLine().trim());
            queryList = new ArrayList<>(numberOfQueries);

            for (int i = 0; i < numberOfQueries; i++) {
                final List<Integer> query = Arrays.stream(reader.readLine().trim().split(" "))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

                queryList.add(query);
            }

            // Mean center the tables
            normalizedItemRatings = normalizeRatings(itemsTable);
            normalizedUserRatings = normalizeRatings(usersTable);
        }
    }

    public static void main(String[] args) throws IOException {
        final CF cf = new CF();

        cf.processQueries();
    }

    private static String formatResult(Double result) {
        final BigDecimal bd = BigDecimal.valueOf(result);
        final BigDecimal res = bd.setScale(3, RoundingMode.HALF_UP);

        return DECIMAL_FORMAT.format(res);
    }

    private static List<List<Double>> normalizeRatings(List<List<Integer>> ratings) {
        final List<List<Double>> normalizedRatings = new ArrayList<>(ratings.size());

        for (final List<Integer> row : ratings) {
            final int sum = row.stream().mapToInt(Integer::intValue).sum();
            final int count = (int) row.stream().filter(x -> x != 0).count();
            final double mean = (double) sum / count;

            final List<Double> normalizedRow = row.stream()
                .map(item -> item == 0 ? 0 : item - mean)
                .collect(Collectors.toList());

            normalizedRatings.add(normalizedRow);
        }

        return normalizedRatings;
    }

    private static Double cosineSimilarity(List<Double> firstRow, List<Double> secondRow) {
        double numerator = 0.0;
        for (int i = 0, n = firstRow.size(); i < n; i++) {
            numerator += (firstRow.get(i) * secondRow.get(i));
        }

        final double denominator = Math.sqrt(firstRow.stream().mapToDouble(x -> x * x).sum()) *
            Math.sqrt(secondRow.stream().mapToDouble(x -> x * x).sum());

        return denominator == 0.0 ? 0.0 : numerator / denominator;
    }

    public void processQueries() {
        for (final List<Integer> query : queryList) {
            final int i = query.get(0) - 1;
            final int j = query.get(1) - 1;
            final int t = query.get(2);
            final int k = query.get(3);

            final double recommendationResult = (t == 0) ?
                getRecommendationRating(i, j, k, t, numberOfItems, itemsTable, normalizedItemRatings) :
                getRecommendationRating(j, i, k, t, numberOfUsers, usersTable, normalizedUserRatings);

            System.out.println(formatResult(recommendationResult));
        }
    }

    private Double getRecommendationRating(int i, int j, int k, int t, int n, List<List<Integer>> ratings,
        List<List<Double>> normalizedRatings) {
        final Map<Integer, Double> similarityMap = new HashMap<>();

        final List<Double> itemI = normalizedRatings.get(i);
        for (int row = 0; row < n; row++) {
            if (row == i) {
                continue;
            }

            final Double cachedSimilarity = similarityCache.get(List.of(t, i, row));
            if (cachedSimilarity != null) {
                similarityMap.put(row, cachedSimilarity);
            } else {
                final Double coefficient = cosineSimilarity(itemI, normalizedRatings.get(row));
                similarityMap.put(row, coefficient);

                similarityCache.put(List.of(t, i, row), coefficient);
                similarityCache.put(List.of(t, row, i), coefficient);
            }
        }

        var a = similarityMap.entrySet().stream()
            .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
            .filter(x -> ratings.get(x.getKey()).get(j) > 0)
            .limit(k)
            .filter(x -> x.getValue() > 0)
            .collect(Collectors.toList());

        final List<Integer> indexesOfMostSimilarItems = a.stream()
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        final double nominator = indexesOfMostSimilarItems.stream()
            .mapToDouble(x -> similarityMap.get(x) * ratings.get(x).get(j))
            .sum();

        final double denominator = indexesOfMostSimilarItems.stream().mapToDouble(similarityMap::get).sum();

        return nominator / denominator;
    }

}
