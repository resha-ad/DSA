
//4b - Breadth-First Search(BFS)
import java.util.*;

public class PackageDelivery {

    public static int minRoadsToTraverse(int[] packages, int[][] roads) {
        int n = packages.length;
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }
        for (int[] road : roads) {
            int a = road[0], b = road[1];
            graph.get(a).add(b);
            graph.get(b).add(a);
        }

        int minRoads = Integer.MAX_VALUE;

        for (int start = 0; start < n; start++) {
            int[] distance = bfs(start, graph, n);
            boolean allCollected = true;
            for (int i = 0; i < n; i++) {
                if (packages[i] == 1 && distance[i] > 2) {
                    allCollected = false; // If any package is beyond distance 2, skip this start node
                    break;
                }
            }
            if (allCollected) {
                int totalRoads = 0;
                for (int i = 0; i < n; i++) {
                    if (packages[i] == 1) {
                        totalRoads += distance[i] * 2; // Round trip
                    }
                }
                if (totalRoads < minRoads) {
                    minRoads = totalRoads;
                }
            }
        }

        return minRoads == Integer.MAX_VALUE ? -1 : minRoads;
    }

    private static int[] bfs(int start, List<List<Integer>> graph, int n) {
        int[] distance = new int[n];
        Arrays.fill(distance, -1);
        Queue<Integer> queue = new LinkedList<>();
        queue.add(start);
        distance[start] = 0;

        while (!queue.isEmpty()) {
            int node = queue.poll();
            for (int neighbor : graph.get(node)) {
                if (distance[neighbor] == -1) {
                    distance[neighbor] = distance[node] + 1;
                    queue.add(neighbor);
                }
            }
        }

        return distance;
    }

    public static void main(String[] args) {
        int[] packages = { 1, 0, 0, 0, 0, 1 };
        int[][] roads = { { 0, 1 }, { 1, 2 }, { 2, 3 }, { 3, 4 }, { 4, 5 } };
        System.out.println(minRoadsToTraverse(packages, roads)); // Output: 4
    }
}
