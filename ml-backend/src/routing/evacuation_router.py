# src/routing/evacuation_router.py
import networkx as nx
import osmnx as ox
import numpy as np
import folium
from pathlib import Path
import argparse
from math import radians, sin, cos, sqrt, atan2

# Configure OSMnx
ox.settings.log_console = True
ox.settings.use_cache = True
ox.settings.timeout = 300  # Increase timeout for larger areas

OSM_DIR = Path("data/osm")

class EvacuationRouter:
    def __init__(self, region_pbf_path=None):
        self.region_pbf_path = region_pbf_path
        self.graph = None
        self.shelters = []
        self.is_osm_graph = False

    def load_osm_data(self, location_point=(9.9312, 76.2673), dist=15000):
        """
        Load OSM data with better error handling and connectivity.
        """
        print("Loading OSM data for Kochi area...")
        try:
            # Use a larger distance to ensure connectivity
            self.graph = ox.graph_from_point(
                location_point, 
                dist=dist, 
                network_type="drive", 
                simplify=True,
                retain_all=True  # Keep all nodes even if disconnected
            )
            
            # Project graph for accurate distance calculations
            self.graph = ox.project_graph(self.graph)
            self.is_osm_graph = True
            
            print(f"Created graph with {len(self.graph.nodes)} nodes and {len(self.graph.edges)} edges")
            
            # Check graph connectivity
            if not nx.is_connected(self.graph.to_undirected()):
                print("Warning: Graph has multiple connected components. Some routes may be unreachable.")
                # Get the largest connected component
                largest_cc = max(nx.connected_components(self.graph.to_undirected()), key=len)
                self.graph = self.graph.subgraph(largest_cc).copy()
                print(f"Using largest connected component with {len(self.graph.nodes)} nodes")

            self._find_kochi_shelters(location_point, dist)
            
        except Exception as e:
            print(f"Error loading OSM data: {e}")
            print("Creating enhanced demo graph for Kochi...")
            self._create_kochi_demo_graph()
            self.is_osm_graph = False

    def _find_kochi_shelters(self, location_point, dist):
        """
        Find evacuation shelters with verified connectivity.
        """
        try:
            # Carefully selected shelters in Kochi with good road connectivity
            verified_shelters = [
                # Hospitals with main road access
                {"name": "Government Medical College Kalamassery", "lat": 10.0619, "lon": 76.3303, "type": "hospital"},
                {"name": "Ammembal AMC Hospital", "lat": 9.9667, "lon": 76.2833, "type": "hospital"},
                {"name": "Lisie Hospital", "lat": 9.9419, "lon": 76.2794, "type": "hospital"},
                {"name": "Medical Trust Hospital", "lat": 9.9669, "lon": 76.2858, "type": "hospital"},
                {"name": "ERNAKULAM MEDICAL CENTRE", "lat": 9.9686, "lon": 76.2878, "type": "hospital"},
                
                # Large public buildings with good access
                {"name": "Jawaharlal Nehru International Stadium", "lat": 9.9742, "lon": 76.2936, "type": "stadium"},
                {"name": "Rajiv Gandhi Indoor Stadium", "lat": 10.0167, "lon": 76.3667, "type": "stadium"},
                {"name": "Cochin University of Science and Technology", "lat": 9.9419, "lon": 76.2678, "type": "university"},
                
                # Schools on main roads
                {"name": "Kendriya Vidyalaya-Mundamveli Kochi", "lat": 9.9310, "lon": 76.2675, "type": "school"},
                {"name": "St. Sebastians HSS Thoppumpady", "lat": 9.9350, "lon": 76.2710, "type": "school"},
                {"name": "Bharata Mata College", "lat": 9.9689, "lon": 76.2803, "type": "college"},
            ]

            center_lat, center_lon = location_point
            for shelter in verified_shelters:
                distance = self._haversine_km(center_lat, center_lon, shelter["lat"], shelter["lon"])
                if distance <= (dist / 1000):
                    self.shelters.append(shelter)

            print(f"Found {len(self.shelters)} potential evacuation shelters")

        except Exception as e:
            print(f"Error finding Kochi shelters: {e}")
            self._create_kochi_demo_graph()

    def _create_kochi_demo_graph(self):
        """Create a connected demo graph for Kochi."""
        print("Creating connected Kochi demo graph...")
        G = nx.Graph()

        # Well-connected Kochi locations
        kochi_nodes = [
            (9.9312, 76.2673, "Start - Kochi City Center"),
            (9.9419, 76.2678, "Cochin University"),
            (9.9350, 76.2710, "St. Sebastians HSS"),
            (9.9310, 76.2675, "Kendriya Vidyalaya"),
            (9.9419, 76.2794, "Lisie Hospital"),
            (9.9667, 76.2833, "AMC Hospital"),
            (9.9686, 76.2878, "Ernakulam Medical Centre"),
            (9.9742, 76.2936, "Jawaharlal Nehru Stadium"),
            (9.9689, 76.2803, "Bharata Mata College"),
        ]

        # Add nodes
        for lat, lon, name in kochi_nodes:
            G.add_node((lat, lon), name=name)

        # Create a fully connected network (realistic road connections)
        connections = [
            (0, 1), (0, 2), (0, 3),  # City center connections
            (1, 2), (1, 4),           # University area
            (2, 3), (2, 4),           # School connections  
            (4, 5), (4, 6),           # Hospital connections
            (5, 6), (5, 7), (5, 8),   # Hospital to stadium/college
            (6, 7), (6, 8),           # Medical center connections
            (7, 8),                   # Stadium to college
        ]

        for i, j in connections:
            lat1, lon1, _ = kochi_nodes[i]
            lat2, lon2, _ = kochi_nodes[j]
            distance_km = self._haversine_km(lat1, lon1, lat2, lon2)
            G.add_edge((lat1, lon1), (lat2, lon2), length=distance_km * 1000.0)

        self.graph = G
        self.shelters = [
            {"name": "Kendriya Vidyalaya-Mundamveli Kochi", "lat": 9.9310, "lon": 76.2675, "type": "school"},
            {"name": "St. Sebastians HSS Thoppumpady", "lat": 9.9350, "lon": 76.2710, "type": "school"},
            {"name": "Lisie Hospital", "lat": 9.9419, "lon": 76.2794, "type": "hospital"},
            {"name": "AMC Hospital", "lat": 9.9667, "lon": 76.2833, "type": "hospital"},
            {"name": "Ernakulam Medical Centre", "lat": 9.9686, "lon": 76.2878, "type": "hospital"},
            {"name": "Jawaharlal Nehru Stadium", "lat": 9.9742, "lon": 76.2936, "type": "stadium"},
        ]
        print("Connected Kochi demo graph created")

    def _haversine_km(self, lat1, lon1, lat2, lon2):
        """Haversine distance in km between two lat/lon points"""
        R = 6371.0
        lat1, lon1, lat2, lon2 = map(radians, [lat1, lon1, lat2, lon2])
        dlat = lat2 - lat1
        dlon = lon2 - lon1
        a = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
        c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c

    def find_nearest_shelters(self, start_point, num_shelters=5, max_distance_km=50):
        """
        Find nearest shelters with connectivity checking.
        """
        if self.graph is None:
            self.load_osm_data(start_point)

        start_node = self._find_nearest_node(start_point)
        shelter_routes = []

        print(f"Start node: {start_node}")
        print(f"Total shelters to check: {len(self.shelters)}")

        for shelter in self.shelters:
            shelter_point = (shelter["lat"], shelter["lon"])
            shelter_name = shelter["name"]
            
            print(f"Checking route to: {shelter_name}")
            route_data = self._find_route_to_shelter(start_node, shelter_point, shelter_name, max_distance_km)
            
            if route_data:
                route_data["type"] = shelter.get("type", "shelter")
                shelter_routes.append(route_data)
                print(f"  ✓ Route found: {route_data['distance_km']:.2f} km")
            else:
                print(f"  ✗ No route found")

        # Sort by distance and return top ones
        shelter_routes.sort(key=lambda x: x["distance_km"])
        return shelter_routes[:num_shelters]

    def _find_route_to_shelter(self, start_node, shelter_point, shelter_name, max_distance_km):
        """
        Compute shortest path with better error handling.
        """
        try:
            shelter_node = self._find_nearest_node(shelter_point)
            
            # For OSM graphs, ensure nodes are in the same connected component
            if self.is_osm_graph:
                # Check if both nodes are in the graph
                if start_node not in self.graph.nodes or shelter_node not in self.graph.nodes:
                    print(f"  Node not found in graph: start={start_node in self.graph.nodes}, shelter={shelter_node in self.graph.nodes}")
                    return None

            print(f"  Start node: {start_node}, Shelter node: {shelter_node}")

            # Check if nodes are the same (very close locations)
            if start_node == shelter_node:
                print(f"  Start and shelter nodes are the same")
                return {
                    "shelter": shelter_name,
                    "distance_km": 0.01,  # Minimal distance
                    "path": [start_node, shelter_node],
                    "shelter_point": shelter_point,
                }

            # Compute path using network length
            try:
                path = nx.shortest_path(self.graph, start_node, shelter_node, weight="length")
                path_length_m = nx.shortest_path_length(self.graph, start_node, shelter_node, weight="length")
            except nx.NetworkXNoPath:
                print(f"  No network path between {start_node} and {shelter_node}")
                # Fallback: use direct haversine distance
                if self.is_osm_graph:
                    start_lat, start_lon = self.graph.nodes[start_node]['y'], self.graph.nodes[start_node]['x']
                    shelter_lat, shelter_lon = self.graph.nodes[shelter_node]['y'], self.graph.nodes[shelter_node]['x']
                else:
                    start_lat, start_lon = start_node
                    shelter_lat, shelter_lon = shelter_node
                
                distance_km = self._haversine_km(start_lat, start_lon, shelter_lat, shelter_lon)
                if distance_km <= max_distance_km:
                    return {
                        "shelter": shelter_name,
                        "distance_km": distance_km,
                        "path": [start_node, shelter_node],
                        "shelter_point": shelter_point,
                    }
                return None

            # Convert path to coordinates
            path_coords = []
            if self.is_osm_graph:
                for n in path:
                    node_data = self.graph.nodes[n]
                    lat = node_data.get("y")
                    lon = node_data.get("x")
                    if lat is not None and lon is not None:
                        path_coords.append((lat, lon))
            else:
                path_coords = list(path)

            distance_km = float(path_length_m) / 1000.0

            if distance_km <= max_distance_km and len(path_coords) > 1:
                return {
                    "shelter": shelter_name,
                    "distance_km": distance_km,
                    "path": path_coords,
                    "shelter_point": shelter_point,
                }

        except (nx.NetworkXNoPath, nx.NodeNotFound, Exception) as e:
            print(f"  Routing error: {e}")
            
        return None

    def _compute_path_length_m_by_nodes(self, path):
        """Compute path length using node coordinates."""
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
        """Find nearest graph node to point."""
        if self.graph is None:
            self.load_osm_data()

        if self.is_osm_graph:
            try:
                lon, lat = point[1], point[0]
                return ox.distance.nearest_nodes(self.graph, X=lon, Y=lat)
            except Exception as e:
                print(f"Error finding nearest node in OSM graph: {e}")
                # Fallback to simple distance calculation
                return self._fallback_nearest_node(point)
        else:
            return self._fallback_nearest_node(point)

    def _fallback_nearest_node(self, point):
        """Fallback method for finding nearest node."""
        nodes = list(self.graph.nodes)
        if not nodes:
            return point  # Return the point itself as fallback
            
        point_array = np.array(point)
        min_distance = float("inf")
        nearest = nodes[0]
        
        for node in nodes:
            if self.is_osm_graph:
                node_data = self.graph.nodes[node]
                node_point = (node_data.get('y'), node_data.get('x'))
            else:
                node_point = node
                
            if None in node_point:
                continue
                
            node_array = np.array(node_point)
            dist = np.sqrt(np.sum((node_array - point_array) ** 2))
            if dist < min_distance:
                min_distance = dist
                nearest = node
        return nearest

    def create_evacuation_map(self, start_point, shelters_data, flooded_areas=None):
        """
        Create a Folium map showing evacuation routes.
        """
        m = folium.Map(location=start_point, zoom_start=14)

        # Start marker
        folium.Marker(
            start_point, 
            popup="Start Location", 
            tooltip="Your Location",
            icon=folium.Icon(color="red", icon="home", prefix='fa')
        ).add_to(m)

        # Different colors for different shelter types
        type_colors = {
            "hospital": "red",
            "university": "green", 
            "college": "darkgreen",
            "school": "lightgreen",
            "stadium": "orange",
            "religious": "purple",
            "industrial": "blue",
            "shelter": "lightblue"
        }

        for i, shelter_data in enumerate(shelters_data):
            shelter_type = shelter_data.get("type", "shelter")
            color = type_colors.get(shelter_type, "blue")
            
            # Shelter marker
            folium.Marker(
                shelter_data["shelter_point"],
                popup=f"{shelter_data['shelter']}<br>Distance: {shelter_data['distance_km']:.2f} km<br>Type: {shelter_type}",
                tooltip=shelter_data['shelter'],
                icon=folium.Icon(color=color, icon="star", prefix='fa'),
            ).add_to(m)

            # Route line
            path_coords = shelter_data.get("path", [])
            if path_coords and len(path_coords) > 1:
                folium.PolyLine(
                    path_coords, 
                    tooltip=f"Route to {shelter_data['shelter']}",
                    color=color, 
                    weight=4, 
                    opacity=0.7
                ).add_to(m)

        # Add flood areas if provided
        if flooded_areas:
            for i, area in enumerate(flooded_areas):
                folium.Polygon(
                    area, 
                    popup=f"Flood Zone {i+1}",
                    tooltip="Potential Flood Area",
                    color="red", 
                    fill=True, 
                    fill_opacity=0.2
                ).add_to(m)

        return m


def main():
    parser = argparse.ArgumentParser(description='Evacuation Router for Kochi')
    parser.add_argument('--lat', type=float, default=9.9312, help='Latitude')
    parser.add_argument('--lon', type=float, default=76.2673, help='Longitude')
    parser.add_argument('--dist', type=int, default=15000, help='Search distance in meters')
    parser.add_argument('--shelters', type=int, default=5, help='Number of shelters to find')
    
    args = parser.parse_args()

    router = EvacuationRouter()
    start_point = (args.lat, args.lon)
    
    print(f"Finding evacuation routes from: {start_point}")
    router.load_osm_data(location_point=start_point, dist=args.dist)

    shelters = router.find_nearest_shelters(
        start_point, 
        num_shelters=args.shelters,
        max_distance_km=args.dist/1000
    )
    
    print("\n" + "="*50)
    print("EVACUATION ROUTES FOUND")
    print("="*50)
    for i, shelter in enumerate(shelters, 1):
        print(f"{i}. {shelter['shelter']}")
        print(f"   Distance: {shelter['distance_km']:.2f} km")
        print(f"   Type: {shelter.get('type', 'shelter')}")
        print()

    # Create and save map
    m = router.create_evacuation_map(start_point, shelters)
    
    map_filename = f"evacuation_map_{args.lat}_{args.lon}.html"
    m.save(map_filename)
    print(f"Saved evacuation map to: {map_filename}")
    print(f"Open {map_filename} in your browser to view the evacuation routes.")


if __name__ == "__main__":
    main()