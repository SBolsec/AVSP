import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class NodeRank {

    private final Integer n;
    private final Double beta;
    private final Double initialR;
    private final Double teleportation;

    private final List<List<Integer>> neighbors;
    private final List<Query> queries;

    private final List<double[]> iterationCache = new ArrayList<>();

    public NodeRank() throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            // parse first line
            final String[] firstLine = reader.readLine().trim().split(" ");
            n = Integer.parseInt(firstLine[0]);
            beta = Double.parseDouble(firstLine[1]);
            initialR = 1.0 / n;
            teleportation = (1 - beta) / n;

            // parse adjacency matrix
            neighbors = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                final List<Integer> row = Arrays.stream(reader.readLine().trim().split(" "))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

                neighbors.add(row);
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
            return iterationCache.get(query.numberOfIterations)[query.node];
        }

        double[] oldR = getInitialR();

        for (int iteration = iterationCache.size() - 1; iteration < query.numberOfIterations; iteration++) {
            final double[] newR = calculateNextR(oldR);
            iterationCache.add(newR);
            oldR = newR;
        }

        return oldR[query.node];
    }

    private double[] getInitialR() {
        if (!iterationCache.isEmpty()) {
            return iterationCache.get(iterationCache.size() - 1);
        }

        final double[] oldR = new double[n];
        for (int i = 0; i < n; i++) {
            oldR[i] = initialR;
        }
        iterationCache.add(oldR);

        return oldR;
    }

    private double[] calculateNextR(double[] oldR) {
        final double[] newR = new double[n];
        Arrays.fill(newR, teleportation);

        for (int i = 0; i < n; i++) {
            final double coefficient = oldR[i] / neighbors.get(i).size();

            for (final int j : neighbors.get(i)) {
                newR[j] += beta * coefficient;
            }
        }

        return newR;
    }

    private static String formatResult(Double result) {
        return String.format(Locale.US, "%.10f", result);
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
