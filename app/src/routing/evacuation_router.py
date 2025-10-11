# src/routing/evacuation_router.py
import networkx as nx
import osmnx as ox
import numpy as np
import folium
from config import OSM_DIR
from math import radians, sin, cos, sqrt, atan2

class EvacuationRouter:
    def __init__(self, region_pbf_path=None):
        """
        Router that prefers OSMnx graph when available; falls back to a small demo graph.
        """
        self.region_pbf_path = region_pbf_path
        self.graph = None
        self.shelters = None
        self.is_osm_graph = False

    def load_osm_data(self, location_point=(10.0, 76.3), dist=10000):
        """
        Load OSM data using OSMnx.
        Args:
            location_point: (lat, lon) tuple for the area center
            dist: search radius in meters
        """
        print("Loading OSM data using OSMnx...")
        try:
            # Build a driving graph around the point
            self.graph = ox.graph_from_point(location_point, dist=dist, network_type="drive", simplify=True)
            self.is_osm_graph = True
            print(f"Created graph with {len(self.graph.nodes)} nodes and {len(self.graph.edges)} edges")

            # Query point POIs (hospitals, clinics, schools, emergency)
            # Different OSNmx versions expose this under different names; use features_from_point if available.
            try:
                self.shelters = ox.features_from_point(
                    location_point,
                    tags={
                        "amenity": ["hospital", "clinic", "school", "public_building"],
                        "emergency": ["yes"],
                    },
                    dist=dist,
                )
            except Exception:
                # older/newer versions might use geometries_from_point
                self.shelters = ox.geometries_from_point(
                    location_point,
                    tags={
                        "amenity": ["hospital", "clinic", "school", "public_building"],
                        "emergency": ["yes"],
                    },
                    dist=dist,
                )

            print(f"Found {len(self.shelters)} potential shelters")

        except Exception as e:
            print(f"Error loading OSM data: {e}")
            print("Creating demo graph for testing...")
            self._create_demo_graph()
            self.is_osm_graph = False

    def _create_demo_graph(self):
        """Create a small demo graph with coordinate nodes (lat, lon) as keys"""
        print("Creating demo graph...")
        G = nx.Graph()

        demo_nodes = [
            (10.0, 76.3, "Start"),
            (10.01, 76.31, "Hospital A"),
            (10.02, 76.29, "School B"),
            (9.99, 76.32, "Community Center"),
            (10.03, 76.28, "Shelter C"),
        ]

        # Add nodes using (lat, lon) tuples as node identifiers (demo mode)
        for lat, lon, name in demo_nodes:
            G.add_node((lat, lon), name=name)

        # Connect every pair (simple demo) using haversine distance (km)
        for i, node1 in enumerate(demo_nodes):
            for j, node2 in enumerate(demo_nodes):
                if i < j:
                    lat1, lon1, _ = node1
                    lat2, lon2, _ = node2
                    distance_km = self._haversine_km(lat1, lon1, lat2, lon2)
                    # store distance in km as 'length' (consistent with OSMnx 'length' in meters normally)
                    G.add_edge((lat1, lon1), (lat2, lon2), length=distance_km * 1000.0, weight=distance_km)
        self.graph = G

        self.shelters = [
            {"name": "Hospital A", "lat": 10.01, "lon": 76.31},
            {"name": "School B", "lat": 10.02, "lon": 76.29},
            {"name": "Community Center", "lat": 9.99, "lon": 76.32},
        ]
        print("Demo graph created with sample nodes and edges")

    def _haversine_km(self, lat1, lon1, lat2, lon2):
        """Haversine distance in km between two lat/lon points"""
        R = 6371.0
        lat1, lon1, lat2, lon2 = map(radians, [lat1, lon1, lat2, lon2])
        dlat = lat2 - lat1
        dlon = lon2 - lon1
        a = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
        c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c

    def find_nearest_shelters(self, start_point, num_shelters=3, max_distance_km=10):
        """
        Find nearest shelters from start_point (lat, lon).
        Returns up to num_shelters routes (sorted by distance_km).
        """
        if self.graph is None:
            self.load_osm_data(start_point)

        # Find start node robustly
        start_node = self._find_nearest_node(start_point)
        shelter_routes = []

        # If shelters is a GeoDataFrame
        if hasattr(self.shelters, "iterrows"):
            for idx, shelter in self.shelters.iterrows():
                if hasattr(shelter, "geometry") and shelter.geometry is not None:
                    geom = shelter.geometry
                    if geom.geom_type == "Point":
                        shelter_point = (geom.y, geom.x)
                    else:
                        # polygon/multipolygon -> use centroid
                        cent = geom.centroid
                        shelter_point = (cent.y, cent.x)

                    shelter_name = shelter.get("name", f"Shelter_{idx}")
                    route_data = self._find_route_to_shelter(start_node, shelter_point, shelter_name, max_distance_km)
                    if route_data:
                        shelter_routes.append(route_data)
        else:
            # Demo shelters list with dicts
            for shelter in self.shelters:
                shelter_point = (shelter["lat"], shelter["lon"])
                shelter_name = shelter["name"]
                route_data = self._find_route_to_shelter(start_node, shelter_point, shelter_name, max_distance_km)
                if route_data:
                    shelter_routes.append(route_data)

        shelter_routes.sort(key=lambda x: x["distance_km"])
        return shelter_routes[:num_shelters]

    def _find_route_to_shelter(self, start_node, shelter_point, shelter_name, max_distance_km):
        """
        Compute shortest path and distance to shelter_point (lat,lon).
        Returns dict with 'shelter','distance_km','path' (list of (lat,lon)), 'shelter_point'.
        """
        try:
            if self.is_osm_graph:
                # use osmnx-nearest node lookup (expects lon, lat)
                shelter_node = ox.distance.nearest_nodes(self.graph, X=shelter_point[1], Y=shelter_point[0])
            else:
                shelter_node = self._find_nearest_node(shelter_point)

            # ensure start_node is valid for this graph representation
            if self.is_osm_graph and not isinstance(start_node, (int, str)):
                # if start_node came as a tuple (demo-style) convert by nearest_nodes
                start_node = ox.distance.nearest_nodes(self.graph, X=start_node[1], Y=start_node[0])

            # compute path
            # prefer 'length' (meters) attribute used by OSMnx. If not present, compute via haversine
            try:
                path = nx.shortest_path(self.graph, start_node, shelter_node, weight="length")
                path_length_m = nx.shortest_path_length(self.graph, start_node, shelter_node, weight="length")
            except Exception:
                # fallback: compute simply by accumulated haversine over node coordinates
                path = nx.shortest_path(self.graph, start_node, shelter_node)
                path_length_m = self._compute_path_length_m_by_nodes(path)

            # convert path into list of (lat, lon) coordinate pairs for folium
            path_coords = []
            if self.is_osm_graph:
                # node ids -> node attributes x (lon), y (lat)
                for n in path:
                    node_data = self.graph.nodes[n]
                    lat = node_data.get("y") if "y" in node_data else node_data.get("lat")
                    lon = node_data.get("x") if "x" in node_data else node_data.get("lon")
                    path_coords.append((lat, lon))
            else:
                # demo graph nodes are coordinate tuples (lat, lon)
                path_coords = list(path)

            distance_km = float(path_length_m) / 1000.0

            if distance_km <= max_distance_km:
                return {
                    "shelter": shelter_name,
                    "distance_km": distance_km,
                    "path": path_coords,
                    "shelter_point": shelter_point,
                }

        except (nx.NetworkXNoPath, nx.NodeNotFound, Exception) as e:
            # skip unreachable or problems
            print(f"No path found to {shelter_name}: {e}")
        return None

    def _compute_path_length_m_by_nodes(self, path):
        """If edges don't have length, estimate using node coordinates (meters)."""
        total_m = 0.0
        for i in range(len(path) - 1):
            n1, n2 = path[i], path[i + 1]
            if self.is_osm_graph:
                a = self.graph.nodes[n1]
                b = self.graph.nodes[n2]
                lat1, lon1 = a.get("y"), a.get("x")
                lat2, lon2 = b.get("y"), b.get("x")
            else:
                lat1, lon1 = n1
                lat2, lon2 = n2
            total_m += self._haversine_km(lat1, lon1, lat2, lon2) * 1000.0
        return total_m

    def _find_nearest_node(self, point):
        """
        Return the graph node nearest to point=(lat,lon).
        Works for both OSMnx graphs (node ids) and demo graph (coordinate tuple keys).
        """
        if self.graph is None:
            self._create_demo_graph()

        if self.is_osm_graph:
            # osmnx expects (lon, lat) order
            lon, lat = point[1], point[0]
            return ox.distance.nearest_nodes(self.graph, X=lon, Y=lat)
        else:
            # demo: nodes are (lat, lon) tuples
            nodes = list(self.graph.nodes)
            point_array = np.array(point)
            min_distance = float("inf")
            nearest = None
            for node in nodes:
                node_array = np.array(node)
                dist = np.sqrt(np.sum((node_array - point_array) ** 2))
                if dist < min_distance:
                    min_distance = dist
                    nearest = node
            return nearest

    def create_evacuation_map(self, start_point, shelters_data, flooded_areas=None):
        """
        Create a Folium map showing evacuation routes.
        start_point: (lat, lon)
        shelters_data: list of dicts with keys 'shelter','distance_km','path','shelter_point'
        """
        m = folium.Map(location=start_point, zoom_start=13)

        folium.Marker(start_point, popup="Start Location", icon=folium.Icon(color="red", icon="home")).add_to(m)

        colors = ["blue", "green", "purple"]
        for i, shelter_data in enumerate(shelters_data):
            color = colors[i % len(colors)]

            # shelter marker
            folium.Marker(
                shelter_data["shelter_point"],
                popup=f"{shelter_data['shelter']} - {shelter_data['distance_km']:.2f} km",
                icon=folium.Icon(color=color, icon="star"),
            ).add_to(m)

            # ensure path is a list of (lat, lon) pairs
            path_coords = shelter_data.get("path", [])
            if path_coords and isinstance(path_coords[0], (list, tuple)) and len(path_coords[0]) == 2:
                folium.PolyLine(path_coords, popup=f"Route to {shelter_data['shelter']}", color=color, weight=3, opacity=0.8).add_to(m)
            else:
                # skip invalid path
                print(f"Skipping invalid path for {shelter_data['shelter']}")

        if flooded_areas:
            for area in flooded_areas:
                folium.Polygon(area, popup="Flooded Area", color="red", fill=True, fill_opacity=0.3).add_to(m)

        return m


def demo_routing():
    router = EvacuationRouter(region_pbf_path=OSM_DIR / "southern_india.osm.pbf")
    # load OSM around the point
    start_point = (9.9312, 76.2673)  # Kochi / Ernakulam approx.
    router.load_osm_data(location_point=start_point, dist=10000)

    shelters = router.find_nearest_shelters(start_point, num_shelters=3)
    print("Found evacuation routes:")
    for shelter in shelters:
        print(f"- {shelter['shelter']}: {shelter['distance_km']:.2f} km")

    m = router.create_evacuation_map(start_point, shelters)
    m.save("evacuation_map.html")
    print("Saved evacuation map to evacuation_map.html")


if __name__ == "__main__":
    demo_routing()
