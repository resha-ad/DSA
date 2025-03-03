import tkinter as tk
from tkinter import ttk, messagebox, colorchooser
import networkx as nx 
import matplotlib.pyplot as plt
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
import numpy as np
import heapq
from matplotlib.figure import Figure

class NetworkNode:
    """Class to represent a network node (server or client)"""
    def __init__(self, node_id, node_type, x, y):
        self.id = node_id
        self.type = node_type  # 'server' or 'client'
        self.x = x
        self.y = y
        self.connections = {}  # {node_id: (cost, bandwidth)}

class NetworkOptimizer:
    """Core algorithm implementation for network optimization"""
    def __init__(self):
        self.nodes = {}
        self.edge_costs = {}
        self.edge_bandwidths = {}
        
    def add_node(self, node_id, node_type, x, y):
        """Add a new node to the network"""
        self.nodes[node_id] = NetworkNode(node_id, node_type, x, y)
        
    def add_connection(self, from_id, to_id, cost, bandwidth):
        """Add a potential connection between nodes"""
        if from_id in self.nodes and to_id in self.nodes:
            self.nodes[from_id].connections[to_id] = (cost, bandwidth)
            self.nodes[to_id].connections[from_id] = (cost, bandwidth)
            self.edge_costs[(from_id, to_id)] = cost
            self.edge_costs[(to_id, from_id)] = cost
            self.edge_bandwidths[(from_id, to_id)] = bandwidth
            self.edge_bandwidths[(to_id, from_id)] = bandwidth
    
    def normalize_objectives(self):
        """Normalize cost and bandwidth values to be comparable"""
        max_cost = max(self.edge_costs.values()) if self.edge_costs else 1
        min_bandwidth = min(self.edge_bandwidths.values()) if self.edge_bandwidths else 1
        
        normalized_weights = {}
        for edge, cost in self.edge_costs.items():
            # Convert high bandwidth to low weight (for shortest path)
            bandwidth_factor = min_bandwidth / self.edge_bandwidths[edge]
            # Normalize and combine objectives with adaptive weighting
            normalized_weights[edge] = 0.7 * (cost / max_cost) + 0.3 * bandwidth_factor
            
        return normalized_weights
        
    def find_minimum_spanning_tree(self, cost_weight=0.7, bandwidth_weight=0.3):
        """Find MST with weighted objectives using Prim's algorithm with a twist"""
        if not self.nodes:
            return []
            
        # Prepare the combined weight graph
        normalized_weights = self.normalize_objectives()
        
        # Custom implementation of Prim's algorithm
        mst_edges = []
        node_ids = list(self.nodes.keys())
        
        # Start with first node
        included = {node_ids[0]}
        candidate_edges = []
        
        # Add initial edges
        for neighbor in self.nodes[node_ids[0]].connections:
            cost, bandwidth = self.nodes[node_ids[0]].connections[neighbor]
            # Higher bandwidth is better (lower weight)
            bandwidth_factor = 1.0 / bandwidth if bandwidth > 0 else float('inf')
            combined_weight = cost_weight * cost + bandwidth_weight * bandwidth_factor
            heapq.heappush(candidate_edges, (combined_weight, node_ids[0], neighbor))
        
        # Build MST
        while candidate_edges and len(included) < len(node_ids):
            weight, u, v = heapq.heappop(candidate_edges)
            
            if v not in included:
                included.add(v)
                mst_edges.append((u, v, weight))
                
                # Add new candidate edges
                for neighbor in self.nodes[v].connections:
                    if neighbor not in included:
                        cost, bandwidth = self.nodes[v].connections[neighbor]
                        bandwidth_factor = 1.0 / bandwidth if bandwidth > 0 else float('inf')
                        combined_weight = cost_weight * cost + bandwidth_weight * bandwidth_factor
                        heapq.heappush(candidate_edges, (combined_weight, v, neighbor))
        
        return mst_edges
    
    def calculate_shortest_path(self, start_id, end_id, weight_type='combined'):
        """Calculate shortest path between two nodes using Dijkstra's algorithm"""
        if start_id not in self.nodes or end_id not in self.nodes:
            return None, float('inf')
            
        # Initialize
        distances = {node: float('inf') for node in self.nodes}
        distances[start_id] = 0
        priority_queue = [(0, start_id)]
        previous = {node: None for node in self.nodes}
        
        while priority_queue:
            current_distance, current_node = heapq.heappop(priority_queue)
            
            if current_distance > distances[current_node]:
                continue
                
            if current_node == end_id:
                break
                
            for neighbor, (cost, bandwidth) in self.nodes[current_node].connections.items():
                if weight_type == 'cost':
                    weight = cost
                elif weight_type == 'bandwidth':
                    # Lower weight for higher bandwidth
                    weight = 1.0 / bandwidth if bandwidth > 0 else float('inf')
                else:  # combined
                    normalized_weights = self.normalize_objectives()
                    weight = normalized_weights[(current_node, neighbor)]
                    
                distance = current_distance + weight
                
                if distance < distances[neighbor]:
                    distances[neighbor] = distance
                    previous[neighbor] = current_node
                    heapq.heappush(priority_queue, (distance, neighbor))
        
        # Reconstruct path
        if distances[end_id] == float('inf'):
            return None, float('inf')
            
        path = []
        current = end_id
        while current:
            path.append(current)
            current = previous[current]
        path.reverse()
        
        return path, distances[end_id]
    
    def calculate_network_metrics(self, selected_edges):
        """Calculate total cost and latency for a given network topology"""
        total_cost = 0
        avg_latency = 0
        
        # Calculate total cost
        for u, v, _ in selected_edges:
            total_cost += self.edge_costs[(u, v)]
        
        # Calculate average latency (based on bandwidth)
        node_pairs = 0
        total_latency = 0
        
        # Convert selected edges to a graph
        G = nx.Graph()
        for u, v, _ in selected_edges:
            G.add_edge(u, v)
        
        # For each pair of nodes, calculate the path latency
        nodes_list = list(self.nodes.keys())
        for i in range(len(nodes_list)):
            for j in range(i+1, len(nodes_list)):
                start = nodes_list[i]
                end = nodes_list[j]
                
                try:
                    path = nx.shortest_path(G, start, end)
                    path_latency = 0
                    
                    # Calculate path latency based on bandwidth
                    for k in range(len(path)-1):
                        u, v = path[k], path[k+1]
                        bandwidth = self.edge_bandwidths.get((u, v), 1)
                        # Model latency as inversely proportional to bandwidth
                        path_latency += 100 / bandwidth
                        
                    total_latency += path_latency
                    node_pairs += 1
                except nx.NetworkXNoPath:
                    # If no path exists, don't count this pair
                    pass
        
        avg_latency = total_latency / node_pairs if node_pairs > 0 else float('inf')
        
        return total_cost, avg_latency
        
    def apply_hybrid_optimization(self):
        """
        Apply a hybrid optimization approach combining MST and genetic algorithm concepts
        to balance cost and latency objectives
        """
        # Start with a minimum spanning tree based on combined weights
        base_mst = self.find_minimum_spanning_tree(cost_weight=0.7, bandwidth_weight=0.3)
        
        # Generate multiple solutions with different weights
        solutions = []
        
        # Try different weight combinations
        for i in range(11):
            cost_weight = i * 0.1
            bandwidth_weight = 1.0 - cost_weight
            
            mst = self.find_minimum_spanning_tree(cost_weight=cost_weight, bandwidth_weight=bandwidth_weight)
            total_cost, avg_latency = self.calculate_network_metrics(mst)
            
            # Store the solution
            solutions.append((mst, total_cost, avg_latency))
        
        # Find non-dominated solutions (Pareto front)
        pareto_front = []
        for i, (mst_i, cost_i, latency_i) in enumerate(solutions):
            dominated = False
            for j, (mst_j, cost_j, latency_j) in enumerate(solutions):
                if i != j and cost_j <= cost_i and latency_j <= latency_i and (cost_j < cost_i or latency_j < latency_i):
                    dominated = True
                    break
            
            if not dominated:
                pareto_front.append((mst_i, cost_i, latency_i))
        
        # Select the solution with the best balance
        best_solution = None
        best_score = float('inf')
        
        for mst, cost, latency in pareto_front:
            # Normalize the objectives
            max_cost = max(sol[1] for sol in pareto_front)
            max_latency = max(sol[2] for sol in pareto_front)
            
            norm_cost = cost / max_cost if max_cost > 0 else 0
            norm_latency = latency / max_latency if max_latency > 0 else 0
            
            # Calculate weighted sum (lower is better)
            score = 0.5 * norm_cost + 0.5 * norm_latency
            
            if score < best_score:
                best_score = score
                best_solution = mst
        
        return best_solution

class NetworkOptimizerGUI:
    """GUI application for network topology optimization"""
    def __init__(self, root):
        self.root = root
        self.root.title("Network Topology Optimizer")
        self.root.geometry("1200x800")
        
        self.network = NetworkOptimizer()
        self.setup_ui()
        
        # For tracking mouse events
        self.selected_node = None
        self.creating_connection = False
        self.connection_start = None
        
        # Current optimized topology
        self.current_topology = []
        
        # For shortest path visualization
        self.current_path = None
        self.path_start = None
        self.path_end = None
        
        # Node counter for auto-naming
        self.node_counter = 1
        
    def setup_ui(self):
        """Set up the main UI components"""
        # Main frame
        main_frame = ttk.Frame(self.root)
        main_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)
        
        # Left panel for controls
        left_panel = ttk.Frame(main_frame, width=300)
        left_panel.pack(side=tk.LEFT, fill=tk.Y, padx=5, pady=5)
        
        # Node creation section
        node_frame = ttk.LabelFrame(left_panel, text="Create Node")
        node_frame.pack(fill=tk.X, padx=5, pady=5)
        
        ttk.Label(node_frame, text="Node Type:").pack(anchor=tk.W, padx=5, pady=2)
        self.node_type_var = tk.StringVar(value="server")
        ttk.Radiobutton(node_frame, text="Server", variable=self.node_type_var, value="server").pack(anchor=tk.W, padx=20)
        ttk.Radiobutton(node_frame, text="Client", variable=self.node_type_var, value="client").pack(anchor=tk.W, padx=20)
        
        ttk.Button(node_frame, text="Create Node (Click on canvas)", command=self.enable_node_creation).pack(fill=tk.X, padx=5, pady=5)
        
        # Connection section
        connection_frame = ttk.LabelFrame(left_panel, text="Create Connection")
        connection_frame.pack(fill=tk.X, padx=5, pady=5)
        
        ttk.Label(connection_frame, text="Cost:").pack(anchor=tk.W, padx=5, pady=2)
        self.cost_var = tk.DoubleVar(value=10.0)
        ttk.Entry(connection_frame, textvariable=self.cost_var).pack(fill=tk.X, padx=5, pady=2)
        
        ttk.Label(connection_frame, text="Bandwidth (Mbps):").pack(anchor=tk.W, padx=5, pady=2)
        self.bandwidth_var = tk.DoubleVar(value=100.0)
        ttk.Entry(connection_frame, textvariable=self.bandwidth_var).pack(fill=tk.X, padx=5, pady=2)
        
        ttk.Button(connection_frame, text="Create Connection (Select two nodes)", command=self.enable_connection_creation).pack(fill=tk.X, padx=5, pady=5)
        
        # Optimization section
        optimization_frame = ttk.LabelFrame(left_panel, text="Optimization")
        optimization_frame.pack(fill=tk.X, padx=5, pady=5)
        
        ttk.Button(optimization_frame, text="Find Optimal Network", command=self.find_optimal_network).pack(fill=tk.X, padx=5, pady=5)
        
        ttk.Label(optimization_frame, text="Cost Weight:").pack(anchor=tk.W, padx=5, pady=2)
        self.cost_weight_var = tk.DoubleVar(value=0.5)
        cost_scale = ttk.Scale(optimization_frame, from_=0.0, to=1.0, variable=self.cost_weight_var, orient=tk.HORIZONTAL)
        cost_scale.pack(fill=tk.X, padx=5, pady=2)
        
        # Path calculation section
        path_frame = ttk.LabelFrame(left_panel, text="Shortest Path")
        path_frame.pack(fill=tk.X, padx=5, pady=5)
        
        ttk.Label(path_frame, text="Start Node:").pack(anchor=tk.W, padx=5, pady=2)
        self.start_node_var = tk.StringVar()
        self.start_node_combo = ttk.Combobox(path_frame, textvariable=self.start_node_var, state="readonly")
        self.start_node_combo.pack(fill=tk.X, padx=5, pady=2)
        
        ttk.Label(path_frame, text="End Node:").pack(anchor=tk.W, padx=5, pady=2)
        self.end_node_var = tk.StringVar()
        self.end_node_combo = ttk.Combobox(path_frame, textvariable=self.end_node_var, state="readonly")
        self.end_node_combo.pack(fill=tk.X, padx=5, pady=2)
        
        ttk.Button(path_frame, text="Calculate Path", command=self.calculate_path).pack(fill=tk.X, padx=5, pady=5)
        
        # Metrics display
        metrics_frame = ttk.LabelFrame(left_panel, text="Network Metrics")
        metrics_frame.pack(fill=tk.X, padx=5, pady=5)
        
        ttk.Label(metrics_frame, text="Total Cost:").pack(anchor=tk.W, padx=5, pady=2)
        self.total_cost_var = tk.StringVar(value="N/A")
        ttk.Label(metrics_frame, textvariable=self.total_cost_var).pack(anchor=tk.W, padx=20, pady=2)
        
        ttk.Label(metrics_frame, text="Avg. Latency:").pack(anchor=tk.W, padx=5, pady=2)
        self.avg_latency_var = tk.StringVar(value="N/A")
        ttk.Label(metrics_frame, textvariable=self.avg_latency_var).pack(anchor=tk.W, padx=20, pady=2)
        
        # Right panel for network visualization
        right_panel = ttk.Frame(main_frame)
        right_panel.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True, padx=5, pady=5)
        
        # Canvas for drawing the network
        self.figure = Figure(figsize=(8, 6), dpi=100)
        self.canvas = FigureCanvasTkAgg(self.figure, right_panel)
        self.canvas.get_tk_widget().pack(fill=tk.BOTH, expand=True)
        
        self.ax = self.figure.add_subplot(111)
        self.ax.set_xlim(0, 10)
        self.ax.set_ylim(0, 10)
        self.ax.set_title("Network Topology")
        
        # Connect mouse events
        self.canvas.mpl_connect("button_press_event", self.on_canvas_click)
        
        # Action buttons
        button_frame = ttk.Frame(right_panel)
        button_frame.pack(fill=tk.X, padx=5, pady=5)
        
        ttk.Button(button_frame, text="Clear All", command=self.clear_network).pack(side=tk.LEFT, padx=5)
        ttk.Button(button_frame, text="Save Image", command=self.save_image).pack(side=tk.LEFT, padx=5)
        
        # Status bar
        self.status_var = tk.StringVar(value="Ready")
        status_bar = ttk.Label(self.root, textvariable=self.status_var, relief=tk.SUNKEN, anchor=tk.W)
        status_bar.pack(side=tk.BOTTOM, fill=tk.X)
        
        # Initial draw
        self.update_canvas()
        
    def enable_node_creation(self):
        """Enable node creation mode"""
        self.creating_connection = False
        self.connection_start = None
        self.selected_node = None
        self.status_var.set("Click on canvas to place a new node")
        
    def enable_connection_creation(self):
        """Enable connection creation mode"""
        self.creating_connection = True
        self.connection_start = None
        self.selected_node = None
        self.status_var.set("Select the first node for connection")
        
    def on_canvas_click(self, event):
        """Handle canvas click events"""
        if event.xdata is None or event.ydata is None:
            return
            
        x, y = event.xdata, event.ydata
        
        if self.creating_connection:
            # Check if clicked on a node
            clicked_node = self.find_node_at_position(x, y)
            
            if clicked_node:
                if self.connection_start is None:
                    # First node selected
                    self.connection_start = clicked_node
                    self.status_var.set(f"Selected node {clicked_node}. Now select the second node.")
                else:
                    # Second node selected
                    if clicked_node != self.connection_start:
                        # Create connection
                        cost = self.cost_var.get()
                        bandwidth = self.bandwidth_var.get()
                        self.network.add_connection(self.connection_start, clicked_node, cost, bandwidth)
                        self.status_var.set(f"Created connection from {self.connection_start} to {clicked_node}")
                        self.update_canvas()
                        self.update_node_lists()
                    else:
                        self.status_var.set("Cannot connect a node to itself.")
                    
                    # Reset
                    self.connection_start = None
                    self.creating_connection = False
        else:
            # Creating a new node
            node_type = self.node_type_var.get()
            node_id = f"Node{self.node_counter}"
            self.node_counter += 1
            
            self.network.add_node(node_id, node_type, x, y)
            self.status_var.set(f"Created {node_type} node {node_id} at ({x:.2f}, {y:.2f})")
            
            self.update_canvas()
            self.update_node_lists()
            
    def find_node_at_position(self, x, y):
        """Find a node at the given position"""
        for node_id, node in self.network.nodes.items():
            if abs(node.x - x) < 0.3 and abs(node.y - y) < 0.3:
                return node_id
        return None
        
    def update_canvas(self):
        """Update the network visualization"""
        self.ax.clear()
        self.ax.set_xlim(0, 10)
        self.ax.set_ylim(0, 10)
        self.ax.set_title("Network Topology")
        
        # Draw nodes
        for node_id, node in self.network.nodes.items():
            color = 'red' if node.type == 'server' else 'blue'
            self.ax.plot(node.x, node.y, 'o', color=color, markersize=10)
            self.ax.text(node.x, node.y + 0.3, node_id, ha='center')
        
        # Draw all potential connections (light gray)
        for node_id, node in self.network.nodes.items():
            for neighbor, (cost, bandwidth) in node.connections.items():
                if node_id < neighbor:  # Avoid drawing connections twice
                    neighbor_node = self.network.nodes[neighbor]
                    self.ax.plot([node.x, neighbor_node.x], [node.y, neighbor_node.y], 'gray', linestyle=':', linewidth=1)
                    # Add cost and bandwidth labels
                    mid_x = (node.x + neighbor_node.x) / 2
                    mid_y = (node.y + neighbor_node.y) / 2
                    self.ax.text(mid_x, mid_y, f"C:{cost}\nB:{bandwidth}", ha='center', va='center', 
                                 bbox=dict(facecolor='white', alpha=0.7))
        
        # Draw optimized topology (if any) in bold green
        for u, v, _ in self.current_topology:
            if u in self.network.nodes and v in self.network.nodes:
                node_u = self.network.nodes[u]
                node_v = self.network.nodes[v]
                self.ax.plot([node_u.x, node_v.x], [node_u.y, node_v.y], 'green', linewidth=2)
        
        # Draw shortest path (if any) in bold red
        if self.current_path:
            for i in range(len(self.current_path) - 1):
                u = self.current_path[i]
                v = self.current_path[i + 1]
                if u in self.network.nodes and v in self.network.nodes:
                    node_u = self.network.nodes[u]
                    node_v = self.network.nodes[v]
                    self.ax.plot([node_u.x, node_v.x], [node_u.y, node_v.y], 'red', linewidth=3)
        
        self.canvas.draw()
        
    def update_node_lists(self):
        """Update the node selection dropdown lists"""
        nodes = list(self.network.nodes.keys())
        
        self.start_node_combo['values'] = nodes
        self.end_node_combo['values'] = nodes
        
        if nodes and not self.start_node_var.get():
            self.start_node_var.set(nodes[0])
        
        if len(nodes) > 1 and not self.end_node_var.get():
            self.end_node_var.set(nodes[1] if len(nodes) > 1 else nodes[0])
        
    def find_optimal_network(self):
        """Find and display the optimal network topology"""
        if len(self.network.nodes) < 2:
            messagebox.showinfo("Error", "Need at least 2 nodes to optimize network")
            return
            
        self.current_path = None  # Clear any existing paths
        
        # Use the hybrid optimization approach
        self.current_topology = self.network.apply_hybrid_optimization()
        
        # Calculate and display metrics
        if self.current_topology:
            total_cost, avg_latency = self.network.calculate_network_metrics(self.current_topology)
            self.total_cost_var.set(f"{total_cost:.2f}")
            self.avg_latency_var.set(f"{avg_latency:.2f} ms")
            
        self.update_canvas()
        self.status_var.set("Optimized network topology displayed")
        
    def calculate_path(self):
        """Calculate and display the shortest path between selected nodes"""
        start_node = self.start_node_var.get()
        end_node = self.end_node_var.get()
        
        if not start_node or not end_node:
            messagebox.showinfo("Error", "Select start and end nodes")
            return
            
        # Build a graph from the current topology
        G = nx.Graph()
        for u, v, _ in self.current_topology:
            G.add_edge(u, v)
        
        # Check if nodes are connected
        if not nx.has_path(G, start_node, end_node):
            messagebox.showinfo("Error", f"No path exists between {start_node} and {end_node} in the current topology")
            return
            
        # Calculate shortest path
        path, distance = self.network.calculate_shortest_path(start_node, end_node, weight_type='combined')
        
        if path:
            self.current_path = path
            self.path_start = start_node
            self.path_end = end_node
            
            # Calculate bandwidth along the path
            min_bandwidth = float('inf')
            for i in range(len(path) - 1):
                u, v = path[i], path[i+1]
                bandwidth = self.network.edge_bandwidths.get((u, v), 0)
                min_bandwidth = min(min_bandwidth, bandwidth)
            
            self.status_var.set(f"Shortest path from {start_node} to {end_node} displayed. Bottleneck bandwidth: {min_bandwidth} Mbps")
            self.update_canvas()
        else:
            messagebox.showinfo("Error", f"No path found between {start_node} and {end_node}")
        
    def clear_network(self):
        """Clear the entire network"""
        self.network = NetworkOptimizer()
        self.current_topology = []
        self.current_path = None
        self.node_counter = 1
        self.total_cost_var.set("N/A")
        self.avg_latency_var.set("N/A")
        self.update_canvas()
        self.update_node_lists()
        self.status_var.set("Network cleared")
        
    def save_image(self):
        """Save the network visualization as an image"""
        self.figure.savefig("network_topology.png", dpi=300, bbox_inches='tight')
        self.status_var.set("Image saved as 'network_topology.png'")

if __name__ == "__main__":
    root = tk.Tk()
    app = NetworkOptimizerGUI(root)
    root.mainloop()