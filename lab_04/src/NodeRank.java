import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NodeRank {

    private final Integer n;
    private final Double beta;
    private final Double initialR;
    private final Double teleportation;

    private final List<Map<Integer, Double>> matrix;
    private final List<Query> queries;

    private final List<List<Double>> iterationCache = new ArrayList<>();

    public NodeRank() throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            // parse first line
            final String[] firstLine = reader.readLine().trim().split(" ");
            n = Integer.parseInt(firstLine[0]);
            beta = Double.parseDouble(firstLine[1]);
            initialR = 1.0 / n;
            teleportation = (1 - beta) / n;

            // parse adjacency matrix
            matrix = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                matrix.add(new HashMap<>());
            }
            for (int i = 0; i < n; i++) {
                final List<Integer> row = Arrays.stream(reader.readLine().trim().split(" "))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

                for (Integer j : row) {
                    matrix.get(j).put(i, beta / row.size());
                }
            }

            final int numberOfQueries = Integer.parseInt(reader.readLine());

            // parse queries
            queries = new ArrayList<>(numberOfQueries);
            for (int i = 0; i < numberOfQueries; i++) {
                final String[] queryParts = reader.readLine().trim().split(" ");
                queries.add(new Query(Integer.parseInt(queryParts[0]), Integer.parseInt(queryParts[1])));
            }
        }
    }

    public void processQueries() {
        for (final Query query : queries) {
            final double result = processQuery(query);

            System.out.println(formatResult(result));
        }
    }

    private double processQuery(final Query query) {
        if (query.numberOfIterations <= iterationCache.size() - 1) {
            return iterationCache.get(query.numberOfIterations).get(query.node);
        }

        List<Double> oldR;

        if (iterationCache.isEmpty()) {
            oldR = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                oldR.add(initialR);
            }
            iterationCache.add(oldR);
        } else {
            oldR = iterationCache.get(iterationCache.size() - 1);
        }

        for (int iteration = iterationCache.size() - 1; iteration < query.numberOfIterations; iteration++) {
            final List<Double> newR = calculateNextR(oldR);
            iterationCache.add(newR);
            oldR = newR;
        }

        return oldR.get(query.node);
    }

    private List<Double> calculateNextR(List<Double> oldR) {
        final List<Double> newR = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            double sum = teleportation;

            for (final Map.Entry<Integer, Double> entry : matrix.get(i).entrySet()) {
                sum += entry.getValue() * oldR.get(entry.getKey());
            }

            newR.add(sum);
        }

        return newR;
    }

    private static String formatResult(Double result) {
        return String.format("%.10f", result);
    }

    public static void main(String[] args) throws IOException {
        final NodeRank nodeRank = new NodeRank();

        nodeRank.processQueries();
    }

    private static class Query {

        final Integer node;
        final Integer numberOfIterations;

        public Query(final Integer node, final Integer numberOfIterations) {
            this.node = node;
            this.numberOfIterations = numberOfIterations;
        }

    }

}
