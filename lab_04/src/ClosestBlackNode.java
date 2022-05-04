import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

public class ClosestBlackNode {

    private static final Integer MAX_DISTANCE = 10;

    private final Integer numberOfNodes;
    private final Integer numberOfEdges;

    private final List<Boolean> isBlackNode;

    private final List<Set<Integer>> edges;

    ClosestBlackNode() throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            final String[] firstLine = reader.readLine().trim().split(" ");
            numberOfNodes = Integer.parseInt(firstLine[0]);
            numberOfEdges = Integer.parseInt(firstLine[1]);

            isBlackNode = new ArrayList<>(numberOfNodes);
            for (int i = 0; i < numberOfNodes; i++) {
                isBlackNode.add(reader.readLine().trim().equals("1"));
            }

            edges = new ArrayList<>(numberOfEdges);
            for (int i = 0; i < numberOfNodes; i++) {
                edges.add(new TreeSet<>());
            }

            for (int i = 0; i < numberOfEdges; i++) {
                final String[] edge = reader.readLine().trim().split(" ");
                final int from = Integer.parseInt(edge[0]);
                final int to = Integer.parseInt(edge[1]);

                edges.get(from).add(to);
                edges.get(to).add(from);
            }
        }
    }

    public void process() {
        loop: for (int i = 0; i < numberOfNodes; i++) {
            if (isBlackNode.get(i)) {
                System.out.println(i + " 0");
                continue;
            }

            Queue<Integer> queue = new PriorityQueue<>(edges.get(i));

            for (int j = 0; j < MAX_DISTANCE; j++) {
                Queue<Integer> nextQueue = new PriorityQueue<>();

                while (!queue.isEmpty()) {
                    final int node = queue.remove();

                    if (isBlackNode.get(node)) {
                        System.out.println(node + " " + (j + 1));
                        continue loop;
                    }

                    edges.get(node).forEach(nextQueue::add);
                }

                queue = nextQueue;
            }

            System.out.println("-1 -1");
        }
    }

    public static void main(String[] args) throws IOException {
        final ClosestBlackNode closestBlackNode = new ClosestBlackNode();

        closestBlackNode.process();
    }

}
