import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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

    private final Double m2;

    private Double maximumModularity = Double.MIN_VALUE;

    private Map<Integer, List<Edge>> maximumModularityEdgeMap = new HashMap<>();

    private final Map<Integer, List<Edge>> initialEdgeMap = new HashMap<>();

    private static final Comparator<Set<Integer>> COMMUNITY_COMPARATOR = (o1, o2) -> {
        final int first = Integer.compare(o1.size(), o2.size());

        if (first != 0) {
            return first;
        }

        final Iterator<Integer> i1 = o1.iterator();
        final Iterator<Integer> i2 = o2.iterator();

        while (i1.hasNext()) {
            final int second = Integer.compare(i1.next(), i2.next());

            if (second != 0) {
                return second;
            }
        }

        return 0;
    };

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

                if (!edgeMap.containsKey(node)) {
                    edgeMap.put(node, new ArrayList<>());
                }
            }

            edges.forEach(Edge::calculateWeight);

            for (final Map.Entry<Integer, List<Edge>> entry : edgeMap.entrySet()) {
                initialEdgeMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }

            final double m = 0.5 * edges.stream()
                .mapToDouble(edge -> edge.weight)
                .sum();
            m2 = 1.0 / (2 * m);
        }
    }

    public void run() {
        while (!edges.isEmpty()) {
            final double modularity = calculateModularity();
            if (modularity > maximumModularity) {
                maximumModularity = modularity;

                maximumModularityEdgeMap = new HashMap<>();
                for (final Map.Entry<Integer, List<Edge>> entry : edgeMap.entrySet()) {
                    maximumModularityEdgeMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                }
            }

            calculateBetweenness();

            final List<Edge> edgesToRemove = getEdgesWithHighestBetweenness();

            edgesToRemove.stream()
                .collect(Collectors.toCollection(TreeSet::new))
                .stream()
                .map(Edge::toString)
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
            final Set<List<Integer>> shortestPaths = transformSearchPaths(shortestSearchPaths);

            for (final List<Integer> path : shortestPaths) {
                final long count = shortestPaths.stream()
                    .filter(x -> Objects.equals(x.get(0), path.get(0)) &&
                        Objects.equals(x.get(x.size() - 1), path.get(path.size() - 1)))
                    .count();

                final double incrementBy = 1.0 / count;

                for (int i = 0; i < path.size() - 1; i++) {
                    final Edge edge1 = new Edge(path.get(i), path.get(i + 1));
                    final Edge edge2 = new Edge(path.get(i + 1), path.get(i));

                    edges.stream()
                        .filter(e -> Objects.equals(e, edge1))
                        .forEach(e -> e.incrementBetweenness(incrementBy));

                    edges.stream()
                        .filter(e -> Objects.equals(e, edge2))
                        .forEach(e -> e.incrementBetweenness(incrementBy));
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
        final Set<Set<Integer>> communities = groupCommunities(edgeMap);

        double sum = 0.0;
        for (final int u : edgeMap.keySet()) {
            final double ku = sumNodeEdgeWeights(u);

            for (final int v : edgeMap.keySet()) {
                if (checkNodesInSameCommunity(communities, u, v)) {
                    final double kv = sumNodeEdgeWeights(v);
                    final double edgeWeight = getEdgeWeight(u, v);

                    sum += edgeWeight - (ku * kv * m2);
                }
            }
        }

        return roundToFourDecimals(roundDouble(sum * m2));
    }

    private double sumNodeEdgeWeights(int node) {
        return initialEdgeMap.get(node).stream()
            .mapToDouble(edge -> edge.weight)
            .sum();
    }

    private double getEdgeWeight(int from, int to) {
        if (from == to) {
            return 0.0;
        }

        return initialEdgeMap.get(from).stream()
            .filter(edge -> edge.to == to)
            .findFirst()
            .map(edge -> edge.weight)
            .orElse(0.0);
    }

    private Set<Set<Integer>> groupCommunities(Map<Integer, List<Edge>> map) {
        final Set<Set<Integer>> communities = new TreeSet<>(COMMUNITY_COMPARATOR);
        final Set<Integer> visited = new HashSet<>();

        for (final int node : map.keySet()) {
            if (!visited.contains(node)) {
                final Set<Integer> community = new TreeSet<>();
                final Queue<Integer> queue = new LinkedList<>();

                queue.add(node);
                visited.add(node);

                while (!queue.isEmpty()) {
                    final Integer currentNode = queue.remove();
                    community.add(currentNode);

                    for (final Edge edge : map.get(currentNode)) {
                        if (!visited.contains(edge.to)) {
                            queue.add(edge.to);
                            visited.add(edge.to);
                        }
                    }
                }

                communities.add(community);
            }
        }

        return communities;
    }

    private boolean checkNodesInSameCommunity(Set<Set<Integer>> communities, int u, int v) {
        for (final Set<Integer> community : communities) {
            if (community.contains(u) && community.contains(v)) {
                return true;
            }
        }

        return false;
    }

    private double roundToFourDecimals(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private double roundDouble(double value) {
        if (Math.abs(value) < 0.00001) {
            return 0.0;
        }

        return value;
    }

    private Set<List<Integer>> transformSearchPaths(List<SearchNode> searchNodes) {
        final Set<List<Integer>> shortestPaths = new HashSet<>();

        for (final SearchNode searchNode : searchNodes) {
            if (searchNode.parents.isEmpty()) {
                continue;
            }

            for (final SearchNode parent : searchNode.parents) {
                final Set<List<Integer>> paths = getPath(parent);

                paths.forEach(path -> path.add(searchNode.node));
                shortestPaths.addAll(paths);
            }
        }

        return shortestPaths;
    }

    private Set<List<Integer>> getPath(SearchNode searchNode) {
        if (searchNode.parents.isEmpty()) {
            final List<Integer> path = new ArrayList<>();
            path.add(searchNode.node);

            final Set<List<Integer>> paths = new HashSet<>();
            paths.add(path);
            return paths;
        }

        final Set<List<Integer>> paths = new HashSet<>();

        for (final SearchNode parent : searchNode.parents) {
            final Set<List<Integer>> newPaths = getPath(parent);
            newPaths.forEach(path -> path.add(searchNode.node));
            paths.addAll(newPaths);
        }

        return paths;
    }

    private List<Edge> getEdgesWithHighestBetweenness() {
        final double maxBetweenness = edges.stream()
            .mapToDouble(edge -> edge.betweenness)
            .max()
            .getAsDouble();

        return edges.stream()
            .filter(e -> Math.abs(e.betweenness - maxBetweenness) < 1e-5)
            .collect(Collectors.toList());
    }

    private void printCommunitiesWithMaximumModularity() {
        final Set<Set<Integer>> communities = groupCommunities(maximumModularityEdgeMap);

        final List<String> communityStrings = new ArrayList<>();
        for (final Set<Integer> community : communities) {
            final String communityString = community.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("-"));

            communityStrings.add(communityString);
        }

        System.out.println(communityStrings.stream().collect(Collectors.joining(" ")));
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
            this.weight = 0.0;
            this.betweenness = 0.0;
        }

        private void calculateWeight() {
            if (from == to) {
                return;
            }

            this.weight = propertyVectorMap.get(from).size() -
                (countSameValue(propertyVectorMap.get(from), propertyVectorMap.get(to)) - 1.0);
        }

        private void incrementBetweenness(double increment) {
            this.betweenness += new BigDecimal(increment).setScale(4, RoundingMode.HALF_UP).doubleValue();
        }

        private void fixBetweenness() {
            this.betweenness = new BigDecimal(this.betweenness / 2).setScale(4, RoundingMode.HALF_UP).doubleValue();
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
            if (Math.abs(this.weight - weight) < 1e-5) {
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
