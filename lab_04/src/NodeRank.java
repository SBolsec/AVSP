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

    private final Map<Integer, Node> adjecencyMatrix;
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
            adjecencyMatrix = new HashMap<>(n);
            for (int i = 0; i < n; i++) {
                final List<Integer> row = Arrays.stream(reader.readLine().trim().split(" "))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
                adjecencyMatrix.put(i, new Node(row));
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
        if (query.numberOfIterations < iterationCache.size() - 1) {
            return iterationCache.get(query.numberOfIterations).get(query.node);
        }

        List<Double> oldR;

        if (iterationCache.isEmpty()) {
            oldR = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                oldR.add(initialR);
            }
        } else {
            oldR = iterationCache.get(iterationCache.size() - 1);
        }

        for (int iteration = iterationCache.size() - 1; iteration < query.numberOfIterations; iteration++) {
            final List<Double> newR = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                double sum = teleportation;
                for (int j = 0; j < n; j++) {
                    final Node node = adjecencyMatrix.get(j);

                    sum += node.neighbours.contains(i) ? (node.m * oldR.get(j)) : 0.0;
                }
                newR.add(sum);
            }

            iterationCache.add(newR);
            oldR = newR;
        }

        return oldR.get(query.node);
    }

    private static String formatResult(Double result) {
        return String.format("%.10f", result);
    }

    public static void main(String[] args) throws IOException {
        final NodeRank nodeRank = new NodeRank();

        nodeRank.processQueries();
    }

    private class Node {

        final List<Integer> neighbours;

        final Double m;

        public Node(final List<Integer> neighbours) {
            this.neighbours = neighbours;
            this.m = beta * 1.0 / neighbours.size();
        }

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
