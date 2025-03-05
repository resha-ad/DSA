//5

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class NetworkOptimizerApp extends JFrame {
    // Data structures
    private Map<String, Node> nodes = new HashMap<>();
    private List<Connection> connections = new ArrayList<>();
    private List<Connection> selectedConnections = new ArrayList<>();

    // UI Components
    private NetworkPanel networkPanel;
    private JTextArea analysisArea;

    // Current selection
    private Node selectedNode = null;
    private Connection selectedConnection = null;

    public NetworkOptimizerApp() {
        super("Network Topology Optimizer");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));

        // Create components
        setupUI();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void setupUI() {
        // Create network panel
        networkPanel = new NetworkPanel();
        add(new JScrollPane(networkPanel), BorderLayout.CENTER);

        // Create control panel
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Node controls
        JPanel nodePanel = new JPanel(new GridLayout(3, 1, 5, 5));
        nodePanel.setBorder(BorderFactory.createTitledBorder("Node Controls"));

        JButton addServerBtn = new JButton("Add Server");
        addServerBtn.addActionListener(e -> addNode("server"));

        JButton addClientBtn = new JButton("Add Client");
        addClientBtn.addActionListener(e -> addNode("client"));

        JButton removeNodeBtn = new JButton("Remove Selected Node");
        removeNodeBtn.addActionListener(e -> removeSelectedNode());

        nodePanel.add(addServerBtn);
        nodePanel.add(addClientBtn);
        nodePanel.add(removeNodeBtn);

        // Connection controls
        JPanel connPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        connPanel.setBorder(BorderFactory.createTitledBorder("Connection Controls"));

        JButton addConnBtn = new JButton("Add Connection");
        addConnBtn.addActionListener(e -> addConnection());

        JButton removeConnBtn = new JButton("Remove Connection");
        removeConnBtn.addActionListener(e -> removeConnection());

        connPanel.add(addConnBtn);
        connPanel.add(removeConnBtn);

        // Optimization controls
        JPanel optPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        optPanel.setBorder(BorderFactory.createTitledBorder("Optimization"));

        JButton mstBtn = new JButton("Minimum Cost Tree");
        mstBtn.addActionListener(e -> optimizeMST());

        JButton balancedBtn = new JButton("Balance Cost/Bandwidth");
        balancedBtn.addActionListener(e -> optimizeBalanced());

        JButton pathBtn = new JButton("Find Shortest Path");
        pathBtn.addActionListener(e -> findShortestPath());

        optPanel.add(mstBtn);
        optPanel.add(balancedBtn);
        optPanel.add(pathBtn);

        // Add panels to control panel
        controlPanel.add(nodePanel);
        controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        controlPanel.add(connPanel);
        controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        controlPanel.add(optPanel);

        // Add analysis area
        analysisArea = new JTextArea(5, 30);
        analysisArea.setEditable(false);
        JScrollPane analysisScroll = new JScrollPane(analysisArea);
        analysisScroll.setBorder(BorderFactory.createTitledBorder("Network Analysis"));

        // Add components to frame
        add(controlPanel, BorderLayout.WEST);
        add(analysisScroll, BorderLayout.SOUTH);

        updateAnalysis();
    }

    private void addNode(String type) {
        String label = JOptionPane.showInputDialog(this,
                "Enter label for the " + type + ":",
                type.substring(0, 1).toUpperCase() + type.substring(1));

        if (label != null && !label.trim().isEmpty()) {
            String id = "node" + (nodes.size() + 1);
            int x = 100 + (int) (Math.random() * (networkPanel.getWidth() - 200));
            int y = 100 + (int) (Math.random() * (networkPanel.getHeight() - 200));

            nodes.put(id, new Node(id, type, label, x, y));
            networkPanel.repaint();
            updateAnalysis();
        }
    }

    private void removeSelectedNode() {
        if (selectedNode != null) {
            // Remove connections to this node
            connections.removeIf(c -> c.source.equals(selectedNode.id) || c.target.equals(selectedNode.id));
            selectedConnections.removeIf(c -> c.source.equals(selectedNode.id) || c.target.equals(selectedNode.id));

            nodes.remove(selectedNode.id);
            selectedNode = null;
            networkPanel.repaint();
            updateAnalysis();
        } else {
            JOptionPane.showMessageDialog(this, "Please select a node first");
        }
    }

    private void addConnection() {
        if (nodes.size() < 2) {
            JOptionPane.showMessageDialog(this, "Need at least two nodes to create a connection");
            return;
        }

        // Create node selection dropdowns
        JComboBox<String> sourceBox = new JComboBox<>();
        JComboBox<String> targetBox = new JComboBox<>();

        for (Node node : nodes.values()) {
            sourceBox.addItem(node.label + " (" + node.id + ")");
            targetBox.addItem(node.label + " (" + node.id + ")");
        }

        JTextField costField = new JTextField("10", 5);
        JTextField bwField = new JTextField("100", 5);

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Source:"));
        panel.add(sourceBox);
        panel.add(new JLabel("Target:"));
        panel.add(targetBox);
        panel.add(new JLabel("Cost:"));
        panel.add(costField);
        panel.add(new JLabel("Bandwidth:"));
        panel.add(bwField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Connection",
                JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String sourceText = (String) sourceBox.getSelectedItem();
            String targetText = (String) targetBox.getSelectedItem();

            String sourceId = sourceText.substring(sourceText.lastIndexOf("(") + 1, sourceText.lastIndexOf(")"));
            String targetId = targetText.substring(targetText.lastIndexOf("(") + 1, targetText.lastIndexOf(")"));

            if (sourceId.equals(targetId)) {
                JOptionPane.showMessageDialog(this, "Source and target cannot be the same");
                return;
            }

            // Check if connection already exists
            for (Connection c : connections) {
                if ((c.source.equals(sourceId) && c.target.equals(targetId)) ||
                        (c.source.equals(targetId) && c.target.equals(sourceId))) {
                    JOptionPane.showMessageDialog(this, "This connection already exists");
                    return;
                }
            }

            try {
                double cost = Double.parseDouble(costField.getText().trim());
                double bandwidth = Double.parseDouble(bwField.getText().trim());

                if (cost <= 0 || bandwidth <= 0) {
                    throw new NumberFormatException("Values must be positive");
                }

                connections.add(new Connection(sourceId, targetId, cost, bandwidth));
                networkPanel.repaint();
                updateAnalysis();

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter valid positive numbers");
            }
        }
    }

    private void removeConnection() {
        if (selectedConnection != null) {
            connections.remove(selectedConnection);
            selectedConnections.remove(selectedConnection);
            selectedConnection = null;
            networkPanel.repaint();
            updateAnalysis();
        } else {
            JOptionPane.showMessageDialog(this, "Please select a connection first");
        }
    }

    private void optimizeMST() {
        if (nodes.size() < 2 || connections.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Need at least two nodes and one connection");
            return;
        }

        selectedConnections.clear();

        // Sort connections by cost (Kruskal's algorithm)
        List<Connection> sortedConnections = new ArrayList<>(connections);
        sortedConnections.sort(Comparator.comparingDouble(c -> c.cost));

        // Union-find data structure for MST
        Map<String, String> parent = new HashMap<>();
        for (String nodeId : nodes.keySet()) {
            parent.put(nodeId, nodeId);
        }

        // Implementation of find operation
        class UnionFind {
            String find(Map<String, String> parent, String node) {
                if (!parent.get(node).equals(node)) {
                    parent.put(node, find(parent, parent.get(node)));
                }
                return parent.get(node);
            }
        }

        UnionFind uf = new UnionFind();

        // Build MST
        for (Connection conn : sortedConnections) {
            String rootSource = uf.find(parent, conn.source);
            String rootTarget = uf.find(parent, conn.target);

            if (!rootSource.equals(rootTarget)) {
                // This edge is part of MST
                selectedConnections.add(conn);
                parent.put(rootSource, rootTarget);
            }

            // Stop when we have n-1 edges
            if (selectedConnections.size() == nodes.size() - 1) {
                break;
            }
        }

        networkPanel.repaint();
        updateAnalysis();
    }

    private void optimizeBalanced() {
        if (nodes.size() < 2 || connections.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Need at least two nodes and one connection");
            return;
        }

        selectedConnections.clear();

        // Find max cost and bandwidth for normalization
        double maxCost = connections.stream().mapToDouble(c -> c.cost).max().orElse(1.0);
        double maxBandwidth = connections.stream().mapToDouble(c -> c.bandwidth).max().orElse(1.0);

        // Sort by balanced metric (normalized cost - normalized bandwidth)
        List<Connection> sortedConnections = new ArrayList<>(connections);
        sortedConnections.sort(Comparator.comparingDouble(c -> (c.cost / maxCost) - (c.bandwidth / maxBandwidth)));

        // Union-find data structure
        Map<String, String> parent = new HashMap<>();
        for (String nodeId : nodes.keySet()) {
            parent.put(nodeId, nodeId);
        }

        // Implementation of find operation
        class UnionFind {
            String find(Map<String, String> parent, String node) {
                if (!parent.get(node).equals(node)) {
                    parent.put(node, find(parent, parent.get(node)));
                }
                return parent.get(node);
            }
        }

        UnionFind uf = new UnionFind();

        // Build balanced tree
        for (Connection conn : sortedConnections) {
            String rootSource = uf.find(parent, conn.source);
            String rootTarget = uf.find(parent, conn.target);

            if (!rootSource.equals(rootTarget)) {
                selectedConnections.add(conn);
                parent.put(rootSource, rootTarget);
            }

            if (selectedConnections.size() == nodes.size() - 1) {
                break;
            }
        }

        networkPanel.repaint();
        updateAnalysis();
    }

    private void findShortestPath() {
        if (nodes.size() < 2) {
            JOptionPane.showMessageDialog(this, "Need at least two nodes to find a path");
            return;
        }

        // Source and target selection
        JComboBox<String> sourceBox = new JComboBox<>();
        JComboBox<String> targetBox = new JComboBox<>();

        for (Node node : nodes.values()) {
            sourceBox.addItem(node.label + " (" + node.id + ")");
            targetBox.addItem(node.label + " (" + node.id + ")");
        }

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Source:"));
        panel.add(sourceBox);
        panel.add(new JLabel("Target:"));
        panel.add(targetBox);

        int result = JOptionPane.showConfirmDialog(this, panel, "Find Shortest Path",
                JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String sourceText = (String) sourceBox.getSelectedItem();
            String targetText = (String) targetBox.getSelectedItem();

            String sourceId = sourceText.substring(sourceText.lastIndexOf("(") + 1, sourceText.lastIndexOf(")"));
            String targetId = targetText.substring(targetText.lastIndexOf("(") + 1, targetText.lastIndexOf(")"));

            if (sourceId.equals(targetId)) {
                JOptionPane.showMessageDialog(this, "Source and target cannot be the same");
                return;
            }

            // Use Dijkstra's algorithm
            List<Connection> path = findDijkstraPath(sourceId, targetId);

            if (path.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No path exists between these nodes");
            } else {
                // Highlight path
                networkPanel.setHighlightedPath(path);

                // Calculate metrics
                double totalCost = 0;
                double totalLatency = 0;

                for (Connection conn : path) {
                    totalCost += conn.cost;
                    totalLatency += 1.0 / conn.bandwidth;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("Path from ").append(nodes.get(sourceId).label)
                        .append(" to ").append(nodes.get(targetId).label)
                        .append(":\n");

                // Build path description
                String currentNode = sourceId;
                sb.append(nodes.get(currentNode).label);

                for (Connection conn : path) {
                    String nextNode = conn.source.equals(currentNode) ? conn.target : conn.source;
                    sb.append(" → ").append(nodes.get(nextNode).label);
                    currentNode = nextNode;
                }

                sb.append("\nTotal cost: ").append(String.format("%.2f", totalCost));
                sb.append("\nEstimated latency: ").append(String.format("%.4f", totalLatency));

                JOptionPane.showMessageDialog(this, sb.toString(), "Path Analysis",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private List<Connection> findDijkstraPath(String sourceId, String targetId) {
        // Prepare adjacency list
        Map<String, Map<String, Connection>> graph = new HashMap<>();
        for (String nodeId : nodes.keySet()) {
            graph.put(nodeId, new HashMap<>());
        }

        // Add connections (use selected connections if available, otherwise all)
        List<Connection> connsToUse = selectedConnections.isEmpty() ? connections : selectedConnections;

        for (Connection conn : connsToUse) {
            // Use bidirectional connections
            graph.get(conn.source).put(conn.target, conn);
            graph.get(conn.target).put(conn.source, conn);
        }

        // Dijkstra's algorithm
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Map<String, Connection> prevConn = new HashMap<>();
        PriorityQueue<NodeDist> queue = new PriorityQueue<>(
                Comparator.comparingDouble(nd -> nd.distance));

        // Initialize
        for (String nodeId : nodes.keySet()) {
            dist.put(nodeId, Double.POSITIVE_INFINITY);
        }

        dist.put(sourceId, 0.0);
        queue.add(new NodeDist(sourceId, 0.0));

        while (!queue.isEmpty()) {
            NodeDist current = queue.poll();
            String u = current.nodeId;

            // Target reached
            if (u.equals(targetId)) {
                break;
            }

            // Skip if better path already found
            if (current.distance > dist.get(u)) {
                continue;
            }

            // Check neighbors
            for (Map.Entry<String, Connection> neighbor : graph.get(u).entrySet()) {
                String v = neighbor.getKey();
                Connection conn = neighbor.getValue();

                // Use inverse bandwidth as weight for latency
                double weight = 1.0 / conn.bandwidth;

                double alt = dist.get(u) + weight;
                if (alt < dist.get(v)) {
                    dist.put(v, alt);
                    prev.put(v, u);
                    prevConn.put(v, conn);
                    queue.add(new NodeDist(v, alt));
                }
            }
        }

        // Build path
        List<Connection> path = new ArrayList<>();
        String current = targetId;

        if (prev.get(current) == null) {
            // No path exists
            return path;
        }

        while (!current.equals(sourceId)) {
            String previous = prev.get(current);
            path.add(0, prevConn.get(current));
            current = previous;
        }

        return path;
    }

    private void updateAnalysis() {
        StringBuilder sb = new StringBuilder();

        // Network statistics
        sb.append("Network Statistics:\n");
        sb.append("Nodes: ").append(nodes.size()).append(" (");
        long serverCount = nodes.values().stream().filter(n -> n.type.equals("server")).count();
        sb.append(serverCount).append(" servers, ");
        sb.append(nodes.size() - serverCount).append(" clients)\n");
        sb.append("Connections: ").append(connections.size()).append("\n");

        // Current topology analysis
        List<Connection> topologyConns = selectedConnections.isEmpty() ? connections : selectedConnections;
        double totalCost = topologyConns.stream().mapToDouble(c -> c.cost).sum();
        double avgBandwidth = topologyConns.isEmpty() ? 0
                : topologyConns.stream().mapToDouble(c -> c.bandwidth).average().getAsDouble();

        sb.append("\nCurrent Topology Metrics:\n");
        sb.append("Total cost: ").append(String.format("%.2f", totalCost)).append("\n");
        sb.append("Average bandwidth: ").append(String.format("%.2f", avgBandwidth)).append("\n");

        // Check if network is connected
        boolean isConnected = checkConnectivity();
        sb.append("Network is ").append(isConnected ? "connected" : "not connected");

        analysisArea.setText(sb.toString());
    }

    private boolean checkConnectivity() {
        if (nodes.isEmpty() || nodes.size() == 1)
            return true;

        // Use BFS to check connectivity
        List<Connection> connsToCheck = selectedConnections.isEmpty() ? connections : selectedConnections;

        // Build adjacency list
        Map<String, List<String>> adjList = new HashMap<>();
        for (String nodeId : nodes.keySet()) {
            adjList.put(nodeId, new ArrayList<>());
        }

        for (Connection conn : connsToCheck) {
            adjList.get(conn.source).add(conn.target);
            adjList.get(conn.target).add(conn.source);
        }

        // BFS traversal
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        String startNode = nodes.keySet().iterator().next();
        queue.add(startNode);
        visited.add(startNode);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            for (String neighbor : adjList.get(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return visited.size() == nodes.size();
    }

    // Inner classes

    private class NetworkPanel extends JPanel {
        private List<Connection> highlightedPath;

        public NetworkPanel() {
            setPreferredSize(new Dimension(800, 600));
            setBackground(Color.WHITE);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Check if clicked on a node
                    for (Node node : nodes.values()) {
                        if (distance(e.getX(), e.getY(), node.x, node.y) <= 20) {
                            selectedNode = node;
                            selectedConnection = null;
                            repaint();
                            return;
                        }
                    }

                    // Check if clicked on a connection
                    for (Connection conn : connections) {
                        Node source = nodes.get(conn.source);
                        Node target = nodes.get(conn.target);

                        if (isNearLine(e.getX(), e.getY(), source.x, source.y, target.x, target.y)) {
                            selectedConnection = conn;
                            selectedNode = null;
                            repaint();
                            return;
                        }
                    }

                    // Clear selection if clicked on empty space
                    selectedNode = null;
                    selectedConnection = null;
                    highlightedPath = null;
                    repaint();
                }

                private double distance(int x1, int y1, int x2, int y2) {
                    return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
                }

                private boolean isNearLine(int px, int py, int x1, int y1, int x2, int y2) {
                    double lineLength = distance(x1, y1, x2, y2);
                    if (lineLength == 0)
                        return false;

                    double t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / (lineLength * lineLength);
                    t = Math.max(0, Math.min(1, t));

                    double projX = x1 + t * (x2 - x1);
                    double projY = y1 + t * (y2 - y1);

                    return distance(px, py, (int) projX, (int) projY) <= 5;
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (selectedNode != null) {
                        selectedNode.x = e.getX();
                        selectedNode.y = e.getY();
                        repaint();
                    }
                }
            });
        }

        public void setHighlightedPath(List<Connection> path) {
            this.highlightedPath = path;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw connections
            for (Connection conn : connections) {
                Node source = nodes.get(conn.source);
                Node target = nodes.get(conn.target);

                if (source != null && target != null) {
                    boolean isSelected = selectedConnections.contains(conn) || conn == selectedConnection;
                    boolean isHighlighted = highlightedPath != null && highlightedPath.contains(conn);

                    if (isHighlighted) {
                        g2d.setColor(Color.GREEN);
                        g2d.setStroke(new BasicStroke(3));
                    } else if (isSelected) {
                        g2d.setColor(Color.BLUE);
                        g2d.setStroke(new BasicStroke(2));
                    } else {
                        g2d.setColor(Color.GRAY);
                        g2d.setStroke(new BasicStroke(1));
                    }

                    g2d.drawLine(source.x, source.y, target.x, target.y);

                    // Draw connection info
                    int midX = (source.x + target.x) / 2;
                    int midY = (source.y + target.y) / 2;

                    // Draw info background
                    g2d.setColor(new Color(240, 240, 240, 220));
                    g2d.fillRect(midX - 30, midY - 15, 60, 30);

                    // Draw info text
                    g2d.setColor(Color.BLACK);
                    g2d.drawString("C:" + (int) conn.cost, midX - 25, midY);
                    g2d.drawString("B:" + (int) conn.bandwidth, midX - 25, midY + 15);
                }
            }

            // Draw nodes
            for (Node node : nodes.values()) {
                // Choose color based on type
                if (node.type.equals("server")) {
                    g2d.setColor(new Color(70, 130, 180)); // Steel blue for servers
                } else {
                    g2d.setColor(new Color(60, 179, 113)); // Medium sea green for clients
                }

                // Draw selected nodes with highlight
                if (node == selectedNode) {
                    g2d.setColor(g2d.getColor().brighter());
                    g2d.fillOval(node.x - 15, node.y - 15, 30, 30);
                    g2d.setColor(Color.BLACK);
                    g2d.drawOval(node.x - 15, node.y - 15, 30, 30);
                } else {
                    g2d.fillOval(node.x - 12, node.y - 12, 24, 24);
                }

                // Draw node label
                g2d.setColor(Color.BLACK);
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(node.label);
                g2d.drawString(node.label, node.x - textWidth / 2, node.y + 25);
            }
        }
    }

    // Node class
    private static class Node {
        String id;
        String type; // "server" or "client"
        String label;
        int x, y; // position

        public Node(String id, String type, String label, int x, int y) {
            this.id = id;
            this.type = type;
            this.label = label;
            this.x = x;
            this.y = y;
        }
    }

    // Connection class
    private static class Connection {
        String source;
        String target;
        double cost;
        double bandwidth;

        public Connection(String source, String target, double cost, double bandwidth) {
            this.source = source;
            this.target = target;
            this.cost = cost;
            this.bandwidth = bandwidth;
        }
    }

    // Helper class for Dijkstra's algorithm
    private static class NodeDist {
        String nodeId;
        double distance;

        public NodeDist(String nodeId, double distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
    }

    // Main method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new NetworkOptimizerApp());
    }
}
