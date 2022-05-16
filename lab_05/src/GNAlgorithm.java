import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class GNAlgorithm {

    private final List<Edge> edges = new ArrayList<>();

    private final Map<Integer, List<Edge>> edgeMap = new HashMap<>();

    private final Map<Integer, List<Boolean>> propertyVectorMap = new HashMap<>();

    private Double maximumModularity = Double.MIN_VALUE;

    private List<Edge> maximumModularityEdges = new ArrayList<>();

    public GNAlgorithm() throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            for (String line = reader.readLine(); !line.isBlank(); line = reader.readLine()) {
                final int[] nodeArray = Arrays.stream(line.trim().split(" "))
                    .mapToInt(Integer::parseInt)
                    .toArray();

                final Edge edge1 = new Edge(nodeArray[0], nodeArray[1]);
                final Edge edge2 = new Edge(nodeArray[1], nodeArray[0]);
                edges.add(edge1);
                edges.add(edge2);

                if (edgeMap.containsKey(nodeArray[0])) {
                    edgeMap.get(nodeArray[0]).add(edge1);
                } else {
                    edgeMap.put(nodeArray[0], new ArrayList<>(List.of(edge1)));
                }

                if (edgeMap.containsKey(nodeArray[1])) {
                    edgeMap.get(nodeArray[1]).add(edge2);
                } else {
                    edgeMap.put(nodeArray[1], new ArrayList<>(List.of(edge2)));
                }
            }

            for (String line = reader.readLine(); line != null && !line.isBlank(); line = reader.readLine()) {
                final int node = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                final List<Boolean> propertiesVector = Arrays.stream(line.substring(line.indexOf(' ') + 1).split(" "))
                    .map(x -> x.equals("1"))
                    .collect(Collectors.toList());

                propertyVectorMap.put(node, propertiesVector);
            }

            edges.forEach(Edge::calculateWeight);
        }
    }

    public void run() {
        while (!edges.isEmpty()) {
            calculateBetweenness();

//            final double modularity = calculateModularity();
//            if (modularity > maximumModularity) {
//                maximumModularity = modularity;
//                maximumModularityEdges = new ArrayList<>(edges);
//            }

            final List<Edge> edgesToRemove = getEdgesWithHighestBetweenness();

            edgesToRemove.stream()
                .map(Edge::toString)
                .collect(Collectors.toCollection(TreeSet::new))
                .forEach(System.out::println);

            for (final Edge edge : edgesToRemove) {
                edgeMap.get(edge.from).remove(edge);
                edgeMap.get(edge.to).remove(new Edge(edge.to, edge.from));
            }

            edges.removeAll(edgesToRemove);
        }

        printCommunitiesWithMaximumModularity();
    }

    private void calculateBetweenness() {
        edges.forEach(Edge::clearBetweenness);

        for (final int startNode : edgeMap.keySet()) {
            final List<SearchNode> shortestSearchPaths = findShortestPaths(startNode);
            final List<List<Integer>> shortestPaths = transformSearchPaths(shortestSearchPaths);

            for (final List<Integer> path : shortestPaths) {
                final long count = shortestPaths.stream()
                    .filter(x -> x.size() == path.size())
                    .filter(x -> Objects.equals(x.get(0), path.get(0)) &&
                        Objects.equals(x.get(x.size() - 1), path.get(path.size() - 1)))
                    .count();

                final double incrementBy = 1.0 / count;

                for (int i = 0; i < path.size() - 1; i++) {
                    final Edge edge1 = new Edge(path.get(i), path.get(i + 1));
                    final Edge edge2 = new Edge(path.get(i + 1), path.get(i));

                    final Optional<Edge> optionalEdge1 = edges.stream()
                        .filter(e -> Objects.equals(e, edge1))
                        .findFirst();

                    optionalEdge1.ifPresent(e -> e.incrementBetweenness(incrementBy));

                    final Optional<Edge> optionalEdge2 = edges.stream()
                        .filter(e -> Objects.equals(e, edge2))
                        .findFirst();

                    optionalEdge2.ifPresent(e -> e.incrementBetweenness(incrementBy));
                }
            }
        }

        edges.forEach(Edge::fixBetweenness);
    }

    private List<SearchNode> findShortestPaths(Integer startNode) {
        final List<SearchNode> searchNodes = new ArrayList<>();
        Queue<SearchNode> queue = new PriorityQueue<>();

        final SearchNode startSearchNode = new SearchNode(startNode, 0);
        queue.add(startSearchNode);
        searchNodes.add(startSearchNode);

        while (!queue.isEmpty()) {
            final Queue<SearchNode> nextQueue = new PriorityQueue<>();

            while (!queue.isEmpty()) {
                final SearchNode searchNode = queue.remove();

                final List<Edge> nodeEdges = edgeMap.get(searchNode.node);
                for (final Edge edge : nodeEdges) {
                    final Optional<SearchNode> optionalSearchNode = searchNodes.stream()
                        .filter(s -> s.node == edge.to)
                        .findFirst();

                    if (optionalSearchNode.isPresent()) {
                        if (optionalSearchNode.get().update(searchNode, searchNode.weight + edge.weight)) {
                            nextQueue.add(optionalSearchNode.get());
                        }
                    } else {
                        final SearchNode node =
                            new SearchNode(edge.to, searchNode.weight + edge.weight, searchNode);

                        nextQueue.add(node);
                        searchNodes.add(node);
                    }
                }
            }

            queue = nextQueue;
        }

        return searchNodes;
    }

    public double calculateModularity() {
        final double m2 = 1. / 2 * edges.size() / 2;

        // TODO
        for (final int u : edgeMap.keySet()) {

        }

        return 0.0;
    }

    private List<List<Integer>> transformSearchPaths(List<SearchNode> searchNodes) {
        final List<List<Integer>> shortestPaths = new ArrayList<>();

        for (final SearchNode searchNode : searchNodes) {
            if (searchNode.parents.isEmpty()) {
                continue;
            }

            for (final SearchNode parent : searchNode.parents) {
                final List<List<Integer>> paths = getPath(parent);

                paths.forEach(path -> path.add(searchNode.node));
                shortestPaths.addAll(paths);
            }
        }

        return shortestPaths;
    }

    private List<List<Integer>> getPath(SearchNode searchNode) {
        if (searchNode.parents.isEmpty()) {
            final List<Integer> path = new ArrayList<>();
            path.add(searchNode.node);

            final List<List<Integer>> paths = new ArrayList<>();
            paths.add(path);
            return paths;
        }

        final List<List<Integer>> paths = new ArrayList<>();

        for (final SearchNode parent : searchNode.parents) {
            final List<List<Integer>> newPaths = getPath(parent);
            newPaths.forEach(path -> path.add(searchNode.node));
            paths.addAll(newPaths);
        }

        return paths;
    }

    private List<Edge> getEdgesWithHighestBetweenness() {
        final Optional<Edge> optionalEdge = edges.stream()
            .sorted(Comparator.comparingDouble(Edge::getBetweenness).reversed())
            .findFirst();

        if (optionalEdge.isPresent()) {
            final Edge edge = optionalEdge.get();
            return edges.stream()
                .filter(e -> Objects.equals(e.betweenness, edge.betweenness))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private void printCommunitiesWithMaximumModularity() {
        // TODO
    }

    private static int countSameValue(List<Boolean> vector1, List<Boolean> vector2) {
        int count = 0;
        for (int i = 0, n = vector1.size(); i < n; i++) {
            if (Objects.equals(vector1.get(i), vector2.get(i))) {
                count++;
            }
        }

        return count;
    }

    private class Edge implements Comparable<Edge> {

        private final int from;
        private final int to;

        private double weight;

        private double betweenness;

        public Edge(int from, int to) {
            this.from = from;
            this.to = to;
            this.weight = Double.MIN_VALUE;
            this.betweenness = 0.0;
        }

        private void calculateWeight() {
            this.weight = propertyVectorMap.get(from).size() -
                (countSameValue(propertyVectorMap.get(from), propertyVectorMap.get(to)) - 1.0);
        }

        private void incrementBetweenness(double increment) {
            this.betweenness += increment;
        }

        private void fixBetweenness() {
            this.betweenness /= 2.0;
        }

        private void clearBetweenness() {
            this.betweenness = 0.0;
        }

        public double getBetweenness() {
            return betweenness;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Edge edge = (Edge) o;
            return from == edge.from && to == edge.to;
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }

        @Override
        public String toString() {
            return String.format("%d %d", Math.min(from, to), Math.max(from, to));
        }

        @Override
        public int compareTo(Edge other) {
            final int first = Integer.compare(Math.min(from, to), Math.min(other.from, other.to));

            if (first != 0) {
                return first;
            }

            return Integer.compare(Math.max(from, to), Math.max(other.from, other.to));
        }

    }

    private static class SearchNode implements Comparable<SearchNode> {

        private final List<SearchNode> parents = new ArrayList<>();

        private final int node;
        private double weight;

        public SearchNode(int node, double weight) {
            this.node = node;
            this.weight = weight;
        }

        public SearchNode(int node, double weight, SearchNode parent) {
            this.node = node;
            this.weight = weight;
            this.parents.add(parent);
        }

        public boolean update(SearchNode parent, double weight) {
            if (this.weight == weight) {
                this.parents.add(parent);

                return true;
            } else if (weight < this.weight) {
                this.weight = weight;
                this.parents.clear();
                this.parents.add(parent);

                return true;
            }

            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SearchNode that = (SearchNode) o;
            return node == that.node;
        }

        @Override
        public int hashCode() {
            return Objects.hash(node);
        }

        @Override
        public int compareTo(SearchNode other) {
            final int first = Double.compare(weight, other.weight);

            if (first != 0) {
                return first;
            }

            return Integer.compare(node, other.node);
        }

    }

    public static void main(String[] args) throws IOException {
        final GNAlgorithm gnAlgorithm = new GNAlgorithm();

        gnAlgorithm.run();
    }

}
