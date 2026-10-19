
"""
Randomized 2D Path Search with Live Grid and Search-Tree Visualizers

Requirements:
    Python 3.10+
    pip install matplotlib networkx numpy

Run:
    python path_search_visualizer.py

The application opens three windows:
    1. Parameters and controls
    2. 2D grid map
    3. Inverted search-tree graph
"""

from collections import deque
import random
import tkinter as tk
from tkinter import ttk, messagebox

import matplotlib
matplotlib.use("TkAgg")
import matplotlib.pyplot as plt
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
import networkx as nx
import numpy as np


# ============================================================
# PHASE 1: ENVIRONMENT GENERATION
# ============================================================

DIRECTIONS = [
    (-1, 0, "Up"),
    (1, 0, "Down"),
    (0, -1, "Left"),
    (0, 1, "Right"),
]


def neighbors(cell, size):
    """Return valid north, south, east, and west neighbors."""
    row, col = cell

    for dr, dc, _ in DIRECTIONS:
        nr = row + dr
        nc = col + dc

        if 0 <= nr < size and 0 <= nc < size:
            yield (nr, nc)


def generate_grid(
    size,
    obstacle_percent,
    start_mode="Random",
    goal_mode="Random",
    start_text="",
    goal_text=""
):
    """
    Generate a randomized grid while guaranteeing that
    a path exists between the start and goal.

    If the requested obstacle density would disconnect
    the goal, the obstacle is rejected.

    Custom coordinates use zero-based row,column notation.
    """

    all_cells = [
        (r, c)
        for r in range(size)
        for c in range(size)
    ]

    obstacle_count = round(
        size * size * obstacle_percent / 100
    )

    def choose_location(mode, custom, avoid=None):
        candidates = [
            cell for cell in all_cells
            if cell != avoid
        ]

        if mode == "Custom":
            try:
                values = custom.split(",")

                if len(values) != 2:
                    raise ValueError

                row, col = [
                    int(value.strip())
                    for value in values
                ]

                point = (row, col)

            except ValueError:
                raise ValueError(
                    "Enter custom coordinates as row,column "
                    "(for example, 0,0)."
                )

            if not (
                0 <= point[0] < size
                and 0 <= point[1] < size
            ):
                raise ValueError(
                    f"Coordinate {point} is outside "
                    f"the {size}x{size} grid."
                )

            if point == avoid:
                raise ValueError(
                    "Start and goal must be different cells."
                )

            return point

        if mode == "Center":
            center = ((size - 1) / 2, (size - 1) / 2)

            return min(
                candidates,
                key=lambda p: (
                    (p[0] - center[0]) ** 2
                    + (p[1] - center[1]) ** 2,
                    random.random()
                )
            )

        if mode == "Edge":
            edge_cells = [
                p for p in candidates
                if p[0] in (0, size - 1)
                or p[1] in (0, size - 1)
            ]

            return random.choice(edge_cells)

        return random.choice(candidates)

    start = choose_location(
        start_mode, start_text
    )

    goal = choose_location(
        goal_mode, goal_text, avoid=start
    )

    # Start and goal can never become obstacles.
    available_cells = [
        cell for cell in all_cells
        if cell != start and cell != goal
    ]

    random.shuffle(available_cells)

    blocked = set()

    def goal_is_reachable(obstacles):
        """BFS reachability test that ignores blocked cells."""
        queue = deque([start])
        visited = {start}

        while queue:
            current = queue.popleft()

            if current == goal:
                return True

            for adjacent in neighbors(current, size):
                if (
                    adjacent not in obstacles
                    and adjacent not in visited
                ):
                    visited.add(adjacent)
                    queue.append(adjacent)

        return False

    # Add obstacles only when a valid route remains.
    for cell in available_cells:
        if len(blocked) >= obstacle_count:
            break

        blocked.add(cell)

        if not goal_is_reachable(blocked):
            blocked.remove(cell)

    return blocked, start, goal


def build_graph(size, blocked):
    """Build an undirected graph of traversable grid cells."""
    graph = nx.Graph()

    for row in range(size):
        for col in range(size):
            cell = (row, col)

            if cell in blocked:
                continue

            graph.add_node(cell)

            for adjacent in neighbors(cell, size):
                if adjacent not in blocked:
                    graph.add_edge(cell, adjacent)

    return graph


# ============================================================
# PHASE 2: UNINFORMED SEARCH ALGORITHMS
# ============================================================

def run_search(graph, start, goal, method="BFS"):
    """
    Generator that performs BFS or DFS and yields search events.

    BFS finds a shortest path in an unweighted graph.
    DFS does not guarantee a shortest path.
    """

    if method == "BFS":
        frontier = deque([start])
        pop_frontier = frontier.popleft
        add_frontier = frontier.append
    else:
        frontier = [start]
        pop_frontier = frontier.pop
        add_frontier = frontier.append

    parent = {start: None}
    discovered = {start}
    expanded = []
    tree_edges = []

    yield {
        "kind": "init",
        "current": start,
        "frontier": list(frontier),
        "discovered": set(discovered),
        "expanded": list(expanded),
        "parent": dict(parent),
        "tree_edges": list(tree_edges),
        "path": []
    }

    found = False

    while frontier:
        current = pop_frontier()
        expanded.append(current)

        yield {
            "kind": "expand",
            "current": current,
            "frontier": list(frontier),
            "discovered": set(discovered),
            "expanded": list(expanded),
            "parent": dict(parent),
            "tree_edges": list(tree_edges),
            "path": []
        }

        if current == goal:
            found = True
            break

        adjacent_cells = sorted(graph.neighbors(current))

        if method == "DFS":
            adjacent_cells.reverse()

        for adjacent in adjacent_cells:
            if adjacent in discovered:
                continue

            discovered.add(adjacent)
            parent[adjacent] = current
            tree_edges.append((current, adjacent))
            add_frontier(adjacent)

            yield {
                "kind": "discover",
                "current": current,
                "new": adjacent,
                "frontier": list(frontier),
                "discovered": set(discovered),
                "expanded": list(expanded),
                "parent": dict(parent),
                "tree_edges": list(tree_edges),
                "path": []
            }

    # Reconstruct the path from goal to start.
    path = []

    if found:
        node = goal

        while node is not None:
            path.append(node)
            node = parent[node]

        path.reverse()

    yield {
        "kind": "done",
        "current": goal if found else (
            expanded[-1] if expanded else start
        ),
        "frontier": list(frontier),
        "discovered": set(discovered),
        "expanded": list(expanded),
        "parent": dict(parent),
        "tree_edges": list(tree_edges),
        "path": path,
        "found": found
    }


# ============================================================
# PHASE 3: USER INTERFACE AND VISUALIZATION
# ============================================================

class PathSearchApp:

    def __init__(self, root):
        self.root = root
        self.root.title(
            "Randomized 2D Path Search - Controls"
        )
        self.root.geometry("470x650")
        self.root.resizable(False, False)

        self.size_var = tk.IntVar(value=15)
        self.obstacle_var = tk.DoubleVar(value=25)
        self.method_var = tk.StringVar(value="BFS")

        self.start_mode_var = tk.StringVar(value="Random")
        self.goal_mode_var = tk.StringVar(value="Random")

        self.start_text_var = tk.StringVar(value="0,0")
        self.goal_text_var = tk.StringVar(value="14,14")

        self.delay_var = tk.IntVar(value=180)
        self.seed_var = tk.StringVar(value="")

        self.status_var = tk.StringVar(
            value="Set parameters, then click Generate / Run."
        )

        self.blocked = set()
        self.start = (0, 0)
        self.goal = (14, 14)
        self.graph = nx.Graph()

        self.events = None
        self.latest_event = None
        self.running = False
        self.after_id = None
        self.generation_succeeded = False

        self._build_controls()
        self._create_view_windows()
        self.generate_only()

    # --------------------------------------------------------
    # CONTROL WINDOW
    # --------------------------------------------------------

    def _build_controls(self):
        frame = ttk.Frame(self.root, padding=12)
        frame.pack(fill="both", expand=True)

        ttk.Label(
            frame,
            text="Randomized 2D Path Search",
            font=("TkDefaultFont", 16, "bold")
        ).pack(anchor="w", pady=(0, 10))

        ttk.Label(
            frame,
            text="Grid size (N x N; 5-35)"
        ).pack(anchor="w")

        ttk.Spinbox(
            frame,
            from_=5,
            to=35,
            textvariable=self.size_var,
            width=10
        ).pack(anchor="w", pady=4)

        ttk.Label(
            frame,
            text="Obstacle density (%)"
        ).pack(anchor="w")

        ttk.Scale(
            frame,
            from_=0,
            to=45,
            variable=self.obstacle_var,
            orient="horizontal",
            length=320
        ).pack(anchor="w", pady=4)

        self.obstacle_label = ttk.Label(frame, text="25%")
        self.obstacle_label.pack(anchor="w")

        self.obstacle_var.trace_add(
            "write",
            lambda *_: self.obstacle_label.config(
                text=f"{self.obstacle_var.get():.0f}%"
            )
        )

        ttk.Label(
            frame,
            text="Uninformed search algorithm"
        ).pack(anchor="w")

        ttk.Combobox(
            frame,
            textvariable=self.method_var,
            values=("BFS", "DFS"),
            state="readonly",
            width=12
        ).pack(anchor="w", pady=4)

        ttk.Label(
            frame,
            text="Start placement"
        ).pack(anchor="w")

        ttk.Combobox(
            frame,
            textvariable=self.start_mode_var,
            values=("Random", "Center", "Edge", "Custom"),
            state="readonly",
            width=12
        ).pack(anchor="w", pady=4)

        ttk.Label(
            frame,
            text="Custom start row,column (zero-based)"
        ).pack(anchor="w")

        ttk.Entry(
            frame,
            textvariable=self.start_text_var,
            width=14
        ).pack(anchor="w", pady=4)

        ttk.Label(
            frame,
            text="Goal placement"
        ).pack(anchor="w")

        ttk.Combobox(
            frame,
            textvariable=self.goal_mode_var,
            values=("Random", "Center", "Edge", "Custom"),
            state="readonly",
            width=12
        ).pack(anchor="w", pady=4)

        ttk.Label(
            frame,
            text="Custom goal row,column (zero-based)"
        ).pack(anchor="w")

        ttk.Entry(
            frame,
            textvariable=self.goal_text_var,
            width=14
        ).pack(anchor="w", pady=4)

        ttk.Label(
            frame,
            text="Animation delay (milliseconds)"
        ).pack(anchor="w")

        ttk.Scale(
            frame,
            from_=20,
            to=700,
            variable=self.delay_var,
            orient="horizontal",
            length=320
        ).pack(anchor="w", pady=4)

        ttk.Label(
            frame,
            text="Optional random seed (blank = random)"
        ).pack(anchor="w")

        ttk.Entry(
            frame,
            textvariable=self.seed_var,
            width=14
        ).pack(anchor="w", pady=4)

        buttons = ttk.Frame(frame)
        buttons.pack(fill="x", pady=10)

        ttk.Button(
            buttons,
            text="Generate / Run",
            command=self.generate_and_run
        ).pack(side="left", padx=2)

        ttk.Button(
            buttons,
            text="Generate Only",
            command=self.generate_only
        ).pack(side="left", padx=2)

        ttk.Button(
            buttons,
            text="Step",
            command=self.step_once
        ).pack(side="left", padx=2)

        ttk.Button(
            buttons,
            text="Stop",
            command=self.stop
        ).pack(side="left", padx=2)

        ttk.Button(
            buttons,
            text="Reset Views",
            command=self.reset_views
        ).pack(side="left", padx=2)

        ttk.Separator(frame).pack(fill="x", pady=7)

        ttk.Label(
            frame,
            textvariable=self.status_var,
            wraplength=420
        ).pack(anchor="w")

        ttk.Label(
            frame,
            text=(
                "Legend: black = obstacle; light blue = discovered; "
                "blue = frontier; gold = expanded; "
                "green = final path; red = current position."
            ),
            wraplength=420
        ).pack(anchor="w", pady=(8, 0))

    # --------------------------------------------------------
    # SEPARATE VISUALIZATION WINDOWS
    # --------------------------------------------------------

    def _create_view_windows(self):
        self.map_window = tk.Toplevel(self.root)
        self.map_window.title("A - 2D Grid Map")
        self.map_window.geometry("650x650")

        self.map_fig, self.map_ax = plt.subplots(
            figsize=(6, 6)
        )

        self.map_canvas = FigureCanvasTkAgg(
            self.map_fig,
            master=self.map_window
        )

        self.map_canvas.get_tk_widget().pack(
            fill="both",
            expand=True
        )

        self.tree_window = tk.Toplevel(self.root)
        self.tree_window.title("B - Inverted Search Tree")
        self.tree_window.geometry("850x650")

        self.tree_fig, self.tree_ax = plt.subplots(
            figsize=(8, 6)
        )

        self.tree_canvas = FigureCanvasTkAgg(
            self.tree_fig,
            master=self.tree_window
        )

        self.tree_canvas.get_tk_widget().pack(
            fill="both",
            expand=True
        )

    # --------------------------------------------------------
    # GENERATE ENVIRONMENT
    # --------------------------------------------------------

    def _read_parameters(self):
        size = int(self.size_var.get())

        if not 5 <= size <= 35:
            raise ValueError(
                "Grid size must be between 5 and 35."
            )

        density = max(
            0,
            min(45, float(self.obstacle_var.get()))
        )

        seed = self.seed_var.get().strip()

        if seed:
            random.seed(int(seed))
            np.random.seed(int(seed))

        return size, density

    def generate_only(self):
        self.stop()
        self.generation_succeeded = False

        try:
            size, density = self._read_parameters()

            self.blocked, self.start, self.goal = generate_grid(
                size,
                density,
                self.start_mode_var.get(),
                self.goal_mode_var.get(),
                self.start_text_var.get(),
                self.goal_text_var.get()
            )

            self.graph = build_graph(size, self.blocked)

            self.events = None

            self.latest_event = {
                "kind": "init",
                "current": self.start,
                "frontier": [self.start],
                "discovered": {self.start},
                "expanded": [],
                "parent": {self.start: None},
                "tree_edges": [],
                "path": []
            }

            self._draw_map(self.latest_event)
            self._draw_tree(self.latest_event)

            requested = round(size * size * density / 100)

            self.status_var.set(
                f"Generated {size}x{size} grid with "
                f"{len(self.blocked)} obstacles "
                f"(requested: {requested}). "
                f"Start={self.start}, Goal={self.goal}."
            )

            self.generation_succeeded = True

        except Exception as exc:
            messagebox.showerror(
                "Invalid parameters",
                str(exc)
            )

    def generate_and_run(self):
        self.generate_only()

        if not self.generation_succeeded:
            return

        if (
            self.graph.has_node(self.start)
            and self.graph.has_node(self.goal)
        ):
            self.events = run_search(
                self.graph,
                self.start,
                self.goal,
                self.method_var.get()
            )

            self.running = True
            self._animate()

    # --------------------------------------------------------
    # ANIMATE SEARCH
    # --------------------------------------------------------

    def _animate(self):
        if not self.running or self.events is None:
            return

        try:
            event = next(self.events)
            self.latest_event = event

            self._draw_map(event)
            self._draw_tree(event)

            if event["kind"] == "done":
                self.running = False

                if event["found"]:
                    self.status_var.set(
                        f"{self.method_var.get()} found a path: "
                        f"{len(event['path']) - 1} moves; "
                        f"{len(event['expanded'])} states expanded; "
                        f"{len(event['discovered'])} discovered."
                    )
                else:
                    self.status_var.set(
                        "No path found. "
                        f"{len(event['expanded'])} states expanded; "
                        f"{len(event['discovered'])} discovered."
                    )

                self.after_id = None
                return

            self.status_var.set(
                f"{self.method_var.get()} - {event['kind']}: "
                f"current={event['current']}; "
                f"frontier={len(event['frontier'])}; "
                f"discovered={len(event['discovered'])}; "
                f"expanded={len(event['expanded'])}."
            )

            self.after_id = self.root.after(
                max(20, int(self.delay_var.get())),
                self._animate
            )

        except StopIteration:
            self.running = False
            self.after_id = None

    def step_once(self):
        if not self.generation_succeeded:
            return

        self.stop()

        if self.events is None:
            self.events = run_search(
                self.graph,
                self.start,
                self.goal,
                self.method_var.get()
            )

        try:
            event = next(self.events)
            self.latest_event = event

            self._draw_map(event)
            self._draw_tree(event)

            if event["kind"] == "done":
                self.status_var.set(
                    (
                        f"Search complete: {len(event['path']) - 1} "
                        "moves."
                    )
                    if event["found"]
                    else "Search complete: no path found."
                )
                self.events = None
            else:
                self.status_var.set(
                    f"Step: {event['kind']} - "
                    f"current={event['current']}; "
                    f"frontier={len(event['frontier'])}; "
                    f"expanded={len(event['expanded'])}."
                )

        except StopIteration:
            self.events = None

    def stop(self):
        self.running = False

        if self.after_id is not None:
            try:
                self.root.after_cancel(self.after_id)
            except Exception:
                pass

            self.after_id = None

    def reset_views(self):
        self.stop()
        self.events = None

        self.latest_event = {
            "kind": "init",
            "current": self.start,
            "frontier": [self.start],
            "discovered": {self.start},
            "expanded": [],
            "parent": {self.start: None},
            "tree_edges": [],
            "path": []
        }

        self._draw_map(self.latest_event)
        self._draw_tree(self.latest_event)

        self.status_var.set(
            "Views reset. Click Generate / Run or Step to start."
        )

    # --------------------------------------------------------
    # DRAW THE 2D GRID WITH FULL-CELL COLORS
    # --------------------------------------------------------

    def _draw_map(self, event):
        size = int(self.size_var.get())
        ax = self.map_ax
        ax.clear()

        # Cell categories:
        # 0 = empty
        # 1 = obstacle
        # 2 = discovered
        # 3 = expanded
        # 4 = frontier
        # 5 = final path

        grid = np.zeros((size, size), dtype=int)

        # Obstacles are always black.
        for row, col in self.blocked:
            grid[row, col] = 1

        discovered = event.get("discovered", set())
        expanded = set(event.get("expanded", []))
        frontier = set(event.get("frontier", []))
        path = set(event.get("path", []))

        # Fill discovered cells.
        for row, col in discovered:
            if (row, col) not in (self.start, self.goal):
                grid[row, col] = 2

        # Expanded cells take priority over discovered cells.
        for row, col in expanded:
            if (row, col) not in (self.start, self.goal):
                grid[row, col] = 3

        # Frontier cells take priority over expanded cells.
        for row, col in frontier:
            if (row, col) not in (self.start, self.goal):
                grid[row, col] = 4

        # Final path takes priority over all search states.
        for row, col in path:
            if (row, col) not in (self.start, self.goal):
                grid[row, col] = 5

        colors = [
            "#f7f7f7",  # Empty
            "#222222",  # Obstacle
            "#5b8fd1",  # Discovered
            "#e5b94d",  # Expanded
            "#4e9de6",  # Frontier
            "#1b9e55",  # Final path
        ]

        cmap = matplotlib.colors.ListedColormap(colors)

        norm = matplotlib.colors.BoundaryNorm(
            np.arange(-0.5, 6.5, 1),
            cmap.N
        )

        # Every cell is colored across its full area.
        ax.imshow(
            grid,
            cmap=cmap,
            norm=norm,
            origin="upper",
            interpolation="nearest"
        )

        # Grid cell boundaries.
        ax.set_xticks(
            np.arange(-0.5, size, 1),
            minor=True
        )

        ax.set_yticks(
            np.arange(-0.5, size, 1),
            minor=True
        )

        ax.grid(
            which="minor",
            color="#b8b8b8",
            linewidth=0.45
        )

        ax.tick_params(
            which="minor",
            bottom=False,
            left=False
        )

        ax.set_xticks(range(size))
        ax.set_yticks(range(size))
        ax.tick_params(labelsize=7)

        ax.set_xlabel("Column")
        ax.set_ylabel("Row")

        ax.set_title(
            "2D State Space - "
            + event.get("kind", "ready").title()
        )

        # Current agent position.
        current_row, current_col = event.get(
            "current", self.start
        )

        ax.scatter(
            current_col,
            current_row,
            s=100,
            c="#d62728",
            marker="o",
            edgecolors="white",
            linewidths=1.2,
            zorder=8
        )

        # Start and goal markers.
        start_row, start_col = self.start
        goal_row, goal_col = self.goal

        ax.scatter(
            start_col,
            start_row,
            s=150,
            c="#2ca02c",
            marker="*",
            edgecolors="white",
            linewidths=0.7,
            zorder=9
        )

        ax.scatter(
            goal_col,
            goal_row,
            s=150,
            c="#9467bd",
            marker="X",
            edgecolors="white",
            linewidths=0.7,
            zorder=9
        )

        ax.text(
            start_col,
            start_row,
            " S",
            color="#145b20",
            fontsize=8,
            weight="bold"
        )

        ax.text(
            goal_col,
            goal_row,
            " G",
            color="#5c2b86",
            fontsize=8,
            weight="bold"
        )

        self.map_fig.tight_layout()
        self.map_canvas.draw_idle()

    # --------------------------------------------------------
    # DRAW THE INVERTED SEARCH TREE
    # --------------------------------------------------------

    def _draw_tree(self, event):
        ax = self.tree_ax
        ax.clear()

        tree_edges = event.get("tree_edges", [])

        parent = event.get(
            "parent",
            {self.start: None}
        )

        nodes = set(parent.keys()) or {self.start}

        tree = nx.DiGraph()
        tree.add_nodes_from(nodes)
        tree.add_edges_from(tree_edges)

        # Determine each node's depth from the root.
        depths = {self.start: 0}

        if nx.is_directed_acyclic_graph(tree):
            ordered_nodes = list(nx.topological_sort(tree))
        else:
            ordered_nodes = list(nodes)

        for node in ordered_nodes:
            p = parent.get(node)

            if p is not None and p in depths:
                depths[node] = depths[p] + 1

        # Arrange nodes by depth, with the root at the top.
        by_depth = {}

        for node in nodes:
            depth = depths.get(node, 0)
            by_depth.setdefault(depth, []).append(node)

        positions = {}

        for depth, layer in by_depth.items():
            layer.sort()
            width = len(layer)

            for index, node in enumerate(layer):
                x = (
                    index - (width - 1) / 2
                ) * max(
                    1.0,
                    7.0 / max(1, width)
                )

                positions[node] = (x, -depth)

        path_nodes = set(event.get("path", []))
        expanded = set(event.get("expanded", []))
        frontier = set(event.get("frontier", []))

        node_colors = []

        for node in tree.nodes():
            if node == self.start:
                node_colors.append("#2ca02c")
            elif node == self.goal:
                node_colors.append("#9467bd")
            elif node in path_nodes:
                node_colors.append("#1b9e55")
            elif node in frontier:
                node_colors.append("#4e9de6")
            elif node in expanded:
                node_colors.append("#e5b94d")
            else:
                node_colors.append("#c9d6e8")

        edge_colors = []

        for source, target in tree.edges():
            if (
                source in path_nodes
                and target in path_nodes
            ):
                edge_colors.append("#1b9e55")
            else:
                edge_colors.append("#777777")

        if tree.number_of_nodes() > 0:
            nx.draw_networkx_edges(
                tree,
                positions,
                ax=ax,
                arrows=True,
                arrowstyle="-|>",
                arrowsize=8,
                edge_color=edge_colors,
                width=1.0
            )

            nx.draw_networkx_nodes(
                tree,
                positions,
                ax=ax,
                node_color=node_colors,
                node_size=360,
                edgecolors="#444444",
                linewidths=0.5
            )

            labels = {
                node: f"{node[0]},{node[1]}"
                for node in tree.nodes()
            }

            nx.draw_networkx_labels(
                tree,
                positions,
                labels=labels,
                ax=ax,
                font_size=6
            )

        ax.set_title(
            f"Inverted Search Tree - {self.method_var.get()} "
            "(root at top)"
        )

        ax.text(
            0.01,
            0.01,
            "Labels are row,column. Blue = frontier, "
            "gold = expanded, green = final path.",
            transform=ax.transAxes,
            fontsize=8,
            va="bottom"
        )

        ax.axis("off")

        self.tree_fig.tight_layout()
        self.tree_canvas.draw_idle()


# ============================================================
# PROGRAM ENTRY POINT
# ============================================================

def main():
    root = tk.Tk()
    app = PathSearchApp(root)
    root.mainloop()


if __name__ == "__main__":
    main()
