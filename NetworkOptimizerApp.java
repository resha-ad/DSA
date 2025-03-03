import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.PriorityQueue;

public class NetworkOptimizerApp extends JFrame {
    private JPanel graphPanel;
    private JPanel controlPanel;
    private JButton addNodeButton;
    private JButton addEdgeButton;
    private JButton optimizeButton;
    private JButton calculatePathButton;
    private JTextArea resultArea;
    private JComboBox<String> algorithmSelector;
    private JComboBox<String> sourceNodeSelector;
    private JComboBox<String> targetNodeSelector;

    private Graph networkGraph;
    private List<Node> nodes;
    private List<Edge> edges;
    private List<Edge> selectedEdges;

    private static final int NODE_RADIUS = 20;
    private Node selectedNode = null;
    private Edge currentEdge = null;

    public NetworkOptimizerApp() {
        setTitle("Network Topology Optimizer");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        networkGraph = new Graph();
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
        selectedEdges = new ArrayList<>();

        initializeUI();

        setVisible(true);
    }

    private void initializeUI() {
        // Graph visualization panel
        graphPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw edges
                for (Edge edge : edges) {
                    if (selectedEdges.contains(edge)) {
                        g2d.setColor(Color.GREEN);
                        g2d.setStroke(new BasicStroke(3.0f));
                    } else {
                        g2d.setColor(Color.GRAY);
                        g2d.setStroke(new BasicStroke(1.5f));
                    }

                    g2d.drawLine(edge.source.x, edge.source.y, edge.destination.x, edge.destination.y);

                    // Draw edge weight/cost information
                    int midX = (edge.source.x + edge.destination.x) / 2;
                    int midY = (edge.source.y + edge.destination.y) / 2;
                    g2d.setColor(Color.BLACK);
                    g2d.drawString("Cost: " + edge.cost + ", BW: " + edge.bandwidth, midX, midY - 5);
                }

                // Draw nodes
                for (Node node : nodes) {
                    if (node.type.equals("Server")) {
                        g2d.setColor(Color.RED);
                    } else { // Client
                        g2d.setColor(Color.BLUE);
                    }

                    g2d.fillOval(node.x - NODE_RADIUS, node.y - NODE_RADIUS,
                            NODE_RADIUS * 2, NODE_RADIUS * 2);
                    g2d.setColor(Color.WHITE);
                    g2d.drawString(node.id, node.x - 5, node.y + 5);
                }
            }
        };

        graphPanel.setBackground(Color.WHITE);
        graphPanel.addMouseListener(new GraphPanelMouseListener());

        // Control panel
        controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));

        JPanel nodePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addNodeButton = new JButton("Add Server");
        addNodeButton.addActionListener(e -> {
            if (addNodeButton.getText().equals("Add Server")) {
                addNodeButton.setText("Add Client");
            } else {
                addNodeButton.setText("Add Server");
            }
        });
        nodePanel.add(addNodeButton);

        addEdgeButton = new JButton("Add Connection");
        addEdgeButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Click on two nodes to create a connection between them.");
        });
        nodePanel.add(addEdgeButton);

        controlPanel.add(nodePanel);

        JPanel algorithmPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        algorithmSelector = new JComboBox<>(new String[] {
                "Minimum Spanning Tree (Prim's)",
                "Minimum Spanning Tree (Kruskal's)",
                "Multi-Objective Optimization",
                "Hill Climbing Optimization"
        });
        algorithmPanel.add(new JLabel("Algorithm:"));
        algorithmPanel.add(algorithmSelector);

        optimizeButton = new JButton("Optimize Network");
        optimizeButton.addActionListener(e -> optimizeNetwork());
        algorithmPanel.add(optimizeButton);

        controlPanel.add(algorithmPanel);

        JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sourceNodeSelector = new JComboBox<>();
        targetNodeSelector = new JComboBox<>();
        calculatePathButton = new JButton("Calculate Path");
        calculatePathButton.addActionListener(e -> calculateShortestPath());

        pathPanel.add(new JLabel("From:"));
        pathPanel.add(sourceNodeSelector);
        pathPanel.add(new JLabel("To:"));
        pathPanel.add(targetNodeSelector);
        pathPanel.add(calculatePathButton);

        controlPanel.add(pathPanel);

        // Results area
        resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);

        // Add components to frame
        add(graphPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.SOUTH);
    }

    private void optimizeNetwork() {
        if (nodes.size() < 2) {
            JOptionPane.showMessageDialog(this, "Please add at least 2 nodes.");
            return;
        }

        if (edges.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add connections between nodes.");
            return;
        }

        String selectedAlgorithm = (String) algorithmSelector.getSelectedItem();

        // Clear previous selection
        selectedEdges.clear();

        if (selectedAlgorithm.contains("Prim's")) {
            selectedEdges.addAll(primMST());
            updateResultArea("Prim's Algorithm", selectedEdges);
        } else if (selectedAlgorithm.contains("Kruskal's")) {
            selectedEdges.addAll(kruskalMST());
            updateResultArea("Kruskal's Algorithm", selectedEdges);
        } else if (selectedAlgorithm.contains("Multi-Objective")) {
            selectedEdges.addAll(multiObjectiveOptimization());
            updateResultArea("Multi-Objective Optimization", selectedEdges);
        } else if (selectedAlgorithm.contains("Hill Climbing")) {
            selectedEdges.addAll(hillClimbingOptimization());
            updateResultArea("Hill Climbing Optimization", selectedEdges);
        }

        graphPanel.repaint();
    }

    private void calculateShortestPath() {
        if (sourceNodeSelector.getSelectedItem() == null ||
                targetNodeSelector.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Please select source and target nodes.");
            return;
        }

        String sourceId = (String) sourceNodeSelector.getSelectedItem();
        String targetId = (String) targetNodeSelector.getSelectedItem();

        Node source = findNodeById(sourceId);
        Node target = findNodeById(targetId);

        if (source == null || target == null) {
            return;
        }

        // Create a graph from selected edges
        Graph tempGraph = new Graph();
        for (Node node : nodes) {
            tempGraph.addNode(node.id);
        }

        for (Edge edge : selectedEdges) {
            tempGraph.addEdge(edge.source.id, edge.destination.id,
                    1.0 / edge.bandwidth, edge.cost); // Using inverse of bandwidth as latency
        }

        Map<String, DijkstraResult> result = tempGraph.dijkstra(source.id);

        if (result.get(target.id).distance == Double.MAX_VALUE) {
            resultArea.setText("No path found between " + sourceId + " and " + targetId);
        } else {
            // Reconstruct path
            List<String> path = new ArrayList<>();
            String current = target.id;

            while (current != null) {
                path.add(0, current);
                current = result.get(current).previousNode;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Shortest Path from ").append(sourceId).append(" to ").append(targetId).append(":\n");
            sb.append("Path: ").append(String.join(" -> ", path)).append("\n");
            sb.append("Total Latency: ").append(String.format("%.2f", result.get(target.id).distance)).append("\n");

            resultArea.setText(sb.toString());
        }
    }

    private Node findNodeById(String id) {
        for (Node node : nodes) {
            if (node.id.equals(id)) {
                return node;
            }
        }
        return null;
    }

    private List<Edge> primMST() {
        if (nodes.isEmpty())
            return new ArrayList<>();

        // Implementation of Prim's algorithm
        List<Edge> mst = new ArrayList<>();
        Set<Node> included = new HashSet<>();
        PriorityQueue<Edge> pq = new PriorityQueue<>(Comparator.comparingDouble(e -> e.cost));

        // Start with first node
        included.add(nodes.get(0));

        // Add all edges from the first node
        for (Edge edge : edges) {
            if (edge.source.equals(nodes.get(0)) || edge.destination.equals(nodes.get(0))) {
                pq.add(edge);
            }
        }

        while (!pq.isEmpty() && included.size() < nodes.size()) {
            Edge minEdge = pq.poll();

            Node nextNode = null;
            if (included.contains(minEdge.source) && !included.contains(minEdge.destination)) {
                nextNode = minEdge.destination;
            } else if (!included.contains(minEdge.source) && included.contains(minEdge.destination)) {
                nextNode = minEdge.source;
            } else {
                // Both nodes are already included, skip this edge
                continue;
            }

            // Add the next node to included set
            included.add(nextNode);
            mst.add(minEdge);

            // Add all edges connected to the next node
            for (Edge edge : edges) {
                if ((edge.source.equals(nextNode) && !included.contains(edge.destination)) ||
                        (edge.destination.equals(nextNode) && !included.contains(edge.source))) {
                    pq.add(edge);
                }
            }
        }

        return mst;
    }

    private List<Edge> kruskalMST() {
        List<Edge> mst = new ArrayList<>();

        // Implementation of Kruskal's algorithm
        DisjointSet ds = new DisjointSet(nodes.size());
        Map<Node, Integer> nodeIndices = new HashMap<>();

        // Assign indices to nodes
        for (int i = 0; i < nodes.size(); i++) {
            nodeIndices.put(nodes.get(i), i);
        }

        // Sort edges by cost
        List<Edge> sortedEdges = new ArrayList<>(edges);
        sortedEdges.sort(Comparator.comparingDouble(e -> e.cost));

        for (Edge edge : sortedEdges) {
            int sourceIdx = nodeIndices.get(edge.source);
            int destIdx = nodeIndices.get(edge.destination);

            if (ds.find(sourceIdx) != ds.find(destIdx)) {
                mst.add(edge);
                ds.union(sourceIdx, destIdx);
            }

            if (mst.size() == nodes.size() - 1) {
                break; // MST is complete
            }
        }

        return mst;
    }

    private List<Edge> multiObjectiveOptimization() {
        // A balanced optimization approach considering both cost and bandwidth
        List<Edge> selectedEdges = new ArrayList<>();

        // Create a weighted score for each edge: lower is better
        Map<Edge, Double> scores = new HashMap<>();
        double maxCost = edges.stream().mapToDouble(e -> e.cost).max().orElse(1.0);
        double maxBandwidth = edges.stream().mapToDouble(e -> e.bandwidth).max().orElse(1.0);

        for (Edge edge : edges) {
            // Normalize factors between 0 and 1 - lower score is better
            double normalizedCost = edge.cost / maxCost;
            double normalizedBandwidth = 1.0 - (edge.bandwidth / maxBandwidth); // Invert because higher bandwidth is
                                                                                // better

            // Combined score with equal weights (can be adjusted)
            double score = 0.5 * normalizedCost + 0.5 * normalizedBandwidth;
            scores.put(edge, score);
        }

        // Run Kruskal's with the custom scores
        DisjointSet ds = new DisjointSet(nodes.size());
        Map<Node, Integer> nodeIndices = new HashMap<>();

        // Assign indices to nodes
        for (int i = 0; i < nodes.size(); i++) {
            nodeIndices.put(nodes.get(i), i);
        }

        // Sort edges by the combined score
        List<Edge> sortedEdges = new ArrayList<>(edges);
        sortedEdges.sort(Comparator.comparingDouble(scores::get));

        for (Edge edge : sortedEdges) {
            int sourceIdx = nodeIndices.get(edge.source);
            int destIdx = nodeIndices.get(edge.destination);

            if (ds.find(sourceIdx) != ds.find(destIdx)) {
                selectedEdges.add(edge);
                ds.union(sourceIdx, destIdx);
            }

            if (selectedEdges.size() == nodes.size() - 1) {
                break; // MST is complete
            }
        }

        return selectedEdges;
    }

    private List<Edge> hillClimbingOptimization() {
        // Start with a random solution (using Kruskal's MST)
        List<Edge> currentSolution = kruskalMST();
        double currentScore = evaluateSolution(currentSolution);

        boolean improved = true;
        int iterations = 0;
        int maxIterations = 100;

        while (improved && iterations < maxIterations) {
            improved = false;
            iterations++;

            // Try replacing each edge in the solution with one that's not in the solution
            for (Edge inEdge : currentSolution) {
                for (Edge outEdge : edges) {
                    if (currentSolution.contains(outEdge))
                        continue;

                    // Create a new candidate solution by swapping edges
                    List<Edge> candidateSolution = new ArrayList<>(currentSolution);
                    candidateSolution.remove(inEdge);
                    candidateSolution.add(outEdge);

                    // Check if the candidate forms a valid spanning tree
                    if (isValidSpanningTree(candidateSolution)) {
                        double candidateScore = evaluateSolution(candidateSolution);

                        if (candidateScore < currentScore) {
                            currentSolution = candidateSolution;
                            currentScore = candidateScore;
                            improved = true;
                            break;
                        }
                    }
                }

                if (improved)
                    break;
            }
        }

        return currentSolution;
    }

    private boolean isValidSpanningTree(List<Edge> solution) {
        if (solution.size() != nodes.size() - 1)
            return false;

        DisjointSet ds = new DisjointSet(nodes.size());
        Map<Node, Integer> nodeIndices = new HashMap<>();

        // Assign indices to nodes
        for (int i = 0; i < nodes.size(); i++) {
            nodeIndices.put(nodes.get(i), i);
        }

        // Check if the solution forms a single connected component
        for (Edge edge : solution) {
            int sourceIdx = nodeIndices.get(edge.source);
            int destIdx = nodeIndices.get(edge.destination);
            ds.union(sourceIdx, destIdx);
        }

        // Check if all nodes are in the same set
        int root = ds.find(0);
        for (int i = 1; i < nodes.size(); i++) {
            if (ds.find(i) != root)
                return false;
        }

        return true;
    }

    private double evaluateSolution(List<Edge> solution) {
        // Evaluate a solution based on both cost and latency
        double totalCost = solution.stream().mapToDouble(e -> e.cost).sum();

        // Calculate average path length (approximation of latency)
        double avgBandwidth = solution.stream().mapToDouble(e -> e.bandwidth).average().orElse(1.0);
        double bandwidthPenalty = 1.0 / avgBandwidth;

        // Combined score (lower is better)
        return 0.7 * totalCost + 0.3 * bandwidthPenalty * 1000;
    }

    private void updateResultArea(String algorithm, List<Edge> selected) {
        double totalCost = selected.stream().mapToDouble(e -> e.cost).sum();
        double avgBandwidth = selected.stream().mapToDouble(e -> e.bandwidth).average().orElse(0);

        StringBuilder sb = new StringBuilder();
        sb.append("Optimization Results (").append(algorithm).append("):\n");
        sb.append("Total Network Cost: ").append(String.format("%.2f", totalCost)).append("\n");
        sb.append("Average Bandwidth: ").append(String.format("%.2f", avgBandwidth)).append("\n");
        sb.append("Selected Connections:\n");

        for (Edge edge : selected) {
            sb.append("- ").append(edge.source.id).append(" to ")
                    .append(edge.destination.id).append(" (Cost: ").append(edge.cost)
                    .append(", Bandwidth: ").append(edge.bandwidth).append(")\n");
        }

        resultArea.setText(sb.toString());

        // Update node selectors
        updateNodeSelectors();
    }

    private void updateNodeSelectors() {
        sourceNodeSelector.removeAllItems();
        targetNodeSelector.removeAllItems();

        for (Node node : nodes) {
            sourceNodeSelector.addItem(node.id);
            targetNodeSelector.addItem(node.id);
        }
    }

    private class GraphPanelMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (addNodeButton.getText().equals("Add Server") ||
                    addNodeButton.getText().equals("Add Client")) {
                // Add node mode
                String type = addNodeButton.getText().equals("Add Server") ? "Server" : "Client";
                String id = type.substring(0, 1) + (nodes.size() + 1); // S1, S2, C1, C2, etc.

                Node newNode = new Node(id, e.getX(), e.getY(), type);
                nodes.add(newNode);
                networkGraph.addNode(id);
                updateNodeSelectors();

            } else if (selectedNode != null) {
                // Adding edge between nodes
                for (Node node : nodes) {
                    if (isPointInNode(e.getX(), e.getY(), node) && !node.equals(selectedNode)) {
                        // Ask for edge properties
                        String costStr = JOptionPane.showInputDialog("Enter connection cost:");
                        String bandwidthStr = JOptionPane.showInputDialog("Enter connection bandwidth:");

                        try {
                            double cost = Double.parseDouble(costStr);
                            double bandwidth = Double.parseDouble(bandwidthStr);

                            Edge newEdge = new Edge(selectedNode, node, cost, bandwidth);
                            edges.add(newEdge);
                            networkGraph.addEdge(selectedNode.id, node.id, bandwidth, cost);

                            selectedNode = null;
                            break;
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(NetworkOptimizerApp.this,
                                    "Please enter valid numeric values");
                        }
                    }
                }
            } else {
                // First node selection for edge
                for (Node node : nodes) {
                    if (isPointInNode(e.getX(), e.getY(), node)) {
                        selectedNode = node;
                        break;
                    }
                }
            }

            graphPanel.repaint();
        }
    }

    private boolean isPointInNode(int x, int y, Node node) {
        int dx = x - node.x;
        int dy = y - node.y;
        return dx * dx + dy * dy <= NODE_RADIUS * NODE_RADIUS;
    }

    // Model classes
    private static class Node {
        String id;
        int x, y;
        String type; // "Server" or "Client"

        Node(String id, int x, int y, String type) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }

    private static class Edge {
        Node source;
        Node destination;
        double cost;
        double bandwidth;

        Edge(Node source, Node destination, double cost, double bandwidth) {
            this.source = source;
            this.destination = destination;
            this.cost = cost;
            this.bandwidth = bandwidth;
        }
    }

    private static class Graph {
        private Map<String, Map<String, EdgeInfo>> adjacencyMap;

        Graph() {
            adjacencyMap = new HashMap<>();
        }

        void addNode(String nodeId) {
            adjacencyMap.putIfAbsent(nodeId, new HashMap<>());
        }

        void addEdge(String source, String destination, double bandwidth, double cost) {
            // Ensure nodes exist
            addNode(source);
            addNode(destination);

            // Add bidirectional edge
            adjacencyMap.get(source).put(destination, new EdgeInfo(bandwidth, cost));
            adjacencyMap.get(destination).put(source, new EdgeInfo(bandwidth, cost));
        }

        Map<String, DijkstraResult> dijkstra(String startNode) {
            Map<String, DijkstraResult> results = new HashMap<>();
            Set<String> visited = new HashSet<>();
            PriorityQueue<DijkstraNode> pq = new PriorityQueue<>(
                    Comparator.comparingDouble(n -> n.distance));

            // Initialize distances
            for (String nodeId : adjacencyMap.keySet()) {
                results.put(nodeId, new DijkstraResult(
                        nodeId.equals(startNode) ? 0 : Double.MAX_VALUE, null));
            }

            pq.add(new DijkstraNode(startNode, 0));

            while (!pq.isEmpty()) {
                DijkstraNode current = pq.poll();

                if (visited.contains(current.nodeId))
                    continue;
                visited.add(current.nodeId);

                Map<String, EdgeInfo> neighbors = adjacencyMap.get(current.nodeId);
                for (Map.Entry<String, EdgeInfo> entry : neighbors.entrySet()) {
                    String neighbor = entry.getKey();
                    EdgeInfo edgeInfo = entry.getValue();

                    if (visited.contains(neighbor))
                        continue;

                    // Using inverse of bandwidth as latency measure
                    double latency = 1.0 / edgeInfo.bandwidth;
                    double newDistance = results.get(current.nodeId).distance + latency;

                    if (newDistance < results.get(neighbor).distance) {
                        results.put(neighbor, new DijkstraResult(newDistance, current.nodeId));
                        pq.add(new DijkstraNode(neighbor, newDistance));
                    }
                }
            }

            return results;
        }

        private static class EdgeInfo {
            double bandwidth;
            double cost;

            EdgeInfo(double bandwidth, double cost) {
                this.bandwidth = bandwidth;
                this.cost = cost;
            }
        }

        private static class DijkstraNode {
            String nodeId;
            double distance;

            DijkstraNode(String nodeId, double distance) {
                this.nodeId = nodeId;
                this.distance = distance;
            }
        }
    }

    private static class DijkstraResult {
        double distance;
        String previousNode;

        DijkstraResult(double distance, String previousNode) {
            this.distance = distance;
            this.previousNode = previousNode;
        }
    }

    private static class DisjointSet {
        private int[] parent;
        private int[] rank;

        DisjointSet(int size) {
            parent = new int[size];
            rank = new int[size];

            for (int i = 0; i < size; i++) {
                parent[i] = i;
                rank[i] = 0;
            }
        }

        int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]); // Path compression
            }
            return parent[x];
        }

        void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);

            if (rootX == rootY)
                return;

            // Union by rank
            if (rank[rootX] < rank[rootY]) {
                parent[rootX] = rootY;
            } else if (rank[rootX] > rank[rootY]) {
                parent[rootY] = rootX;
            } else {
                parent[rootY] = rootX;
                rank[rootX]++;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(NetworkOptimizerApp::new);
    }
}