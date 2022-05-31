import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;

public class DGIM {

    private Long n;

    private Long currentTime = 0L;

    private final Map<Long, Queue<Long>> bucketMap = new HashMap<>();

    public void process() throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            n = Long.parseLong(reader.readLine());

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (line.startsWith("q")) {
                    final long k = Long.parseLong(line.split(" ")[1]);
                    handleQuery(k);
                } else {
                    handleStream(line);
                }
            }
        }
    }

    private void handleStream(String line) {
        for (char bit : line.toCharArray()) {
            this.currentTime++;

            if (bit == '0') {
                continue;
            }

            deleteObsoleteBuckets();

            addBucket(currentTime, 1L);

            for (long i = 1; i < n; i = i * 2) {
                final Queue<Long> queue = bucketMap.get(i);

                if (queue == null) {
                    break;
                }

                if (queue.size() == 3) {
                    // merge buckets
                    queue.poll();
                    addBucket(queue.poll(), i * 2);
                }
            }
        }
    }

    private void addBucket(Long bucket, Long key) {
        final Queue<Long> queue = bucketMap.get(key);

        if (queue != null) {
            queue.add(bucket);
        } else {
            final Queue<Long> newQueue = new PriorityQueue<>();
            newQueue.add(bucket);
            bucketMap.put(key, newQueue);
        }
    }

    private void deleteObsoleteBuckets() {
        final long edgeTime = this.currentTime - n;

        for (long i = 1; i < n; i = i * 2) {
            final Queue<Long> queue = bucketMap.get(i);

            if (queue == null) {
                break;
            }

            final Queue<Long> filteredQueue = queue.stream()
                .filter(bucketEndTime -> bucketEndTime >= edgeTime)
                .collect(Collectors.toCollection(PriorityQueue::new));

            if (filteredQueue.isEmpty()) {
                bucketMap.put(i, null);
            } else {
                bucketMap.put(i, filteredQueue);
            }
        }
    }

    private void handleQuery(Long k) {
        final long edgeTime = this.currentTime - k;
        long sum = 0L;
        long lastFactor = 0L;

        outer: for (long i = 1; i < n; i = i * 2) {
            final Queue<Long> queue = bucketMap.get(i);

            if (queue == null) {
                break;
            }

            final Queue<Long> reversedQueue = new PriorityQueue<>(Collections.reverseOrder());
            reversedQueue.addAll(queue);

            while (!reversedQueue.isEmpty()) {
                final Long endTime = reversedQueue.poll();

                if (endTime <= edgeTime) {
                    break outer;
                }

                sum += i;
                lastFactor = i;
            }
        }

        if (sum > 0L) {
            sum -= (lastFactor == 1L ? 1 : lastFactor / 2);
        }

        System.out.println(sum);
    }

    public static void main(String[] args) throws IOException {
        final DGIM dgim = new DGIM();
        dgim.process();
    }

}
