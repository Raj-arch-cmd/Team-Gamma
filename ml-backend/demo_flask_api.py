"""
demo_flask_api.py

Flask demo API for Hyperlocal-Flood-AI presentation (REAL routing, mocked models, simulated alerts)

- Single-line choices:
    DEFAULT_SELECTION = "ALL"    # change to "LOW"/"MED"/"HIGH"/"ALL"
    USE_REAL_ROUTING = True      # will try to use your real src/routing/evacuation_router.EvacuationRouter
    FORCE_MOCK_MODELS = True     # model inference is mocked (fast & stable)
    SIMULATE_ALERTS = True       # alerts are simulated/written to file (no SMS/push)

Usage:
    python demo_flask_api.py
    python demo_flask_api.py --which HIGH   # override DEFAULT_SELECTION for this run

Endpoints:
    GET /run[?which=LOW|MED|HIGH|ALL]  - trigger demo run(s)
    GET /report/<label>               - get most recent emergency_report.json for label
    GET /map/<label>                  - serve evacuation_map.html for label (if created)
    GET /list                         - list saved demo run directories
"""
from flask import Flask, request, jsonify, send_file
import argparse
import json
from pathlib import Path
from datetime import datetime
import numpy as np
import os
import threading

# ---------------------- Single-line configuration ----------------------
DEFAULT_SELECTION = "ALL"    # <-- change to LOW / MED / HIGH / ALL before running
USE_REAL_ROUTING = True      # Try to use real OSM-based routing (requires osmnx & internet or local graph)
FORCE_MOCK_MODELS = True     # Keep model inference mocked for stable demo (do not call heavy ML models)
SIMULATE_ALERTS = True       # Alerts are simulated (no SMS/push sent)
# ----------------------------------------------------------------------

# demo coordinates
LOW = (9.9390, 76.2600)
MED = (9.9300, 76.2700)
HIGH = (9.9800, 76.2600)
PRESET_POINTS = [("LOW", LOW), ("MED", MED), ("HIGH", HIGH)]
OUT_BASE = Path("outputs/demo_runs")
OUT_BASE.mkdir(parents=True, exist_ok=True)

# simple risk scoring
def sigmoid(x):
    return 1.0 / (1.0 + np.exp(-x))

def compute_risk_score(mean_prob, historical_freq=0.0, elev_factor=0.5, rainfall=0.0, urban=0.0):
    w_p = 0.6
    w_h = 0.2
    w_e = 0.2
    w_r = 0.3
    w_u = 0.1
    bias = 0.5
    x = w_p * mean_prob + w_h * historical_freq + w_e * elev_factor + w_r * rainfall + w_u * urban - bias
    return float(np.clip(sigmoid(x), 0.0, 1.0))

# -------------------------------------------------------------------------
# Try to import your real EvacuationRouter (from src.routing.evacuation_router)
# -------------------------------------------------------------------------
EvacuationRouter = None
router_real_available = False
if USE_REAL_ROUTING:
    try:
        # attempt import
        from src.routing.evacuation_router import EvacuationRouter as EvRouter
        EvacuationRouter = EvRouter
        router_real_available = True
        print("Info: Real EvacuationRouter imported.")
    except Exception as e:
        print("Warning: Could not import real EvacuationRouter (will use demo router).")
        print("Import error:", e)
        EvacuationRouter = None
        router_real_available = False

# -------------------------------------------------------------------------
# Mocked model & demo router (keeps demo stable)
# -------------------------------------------------------------------------
class MockIntegrated:
    def __init__(self, forced_mean_prob=0.05):
        self.forced_mean_prob = forced_mean_prob
    def predict_flood_extent(self, image):
        arr = np.full((256, 256), self.forced_mean_prob, dtype="float32")
        rr, cc = np.ogrid[:256, :256]
        mask = ((rr - 128) ** 2 + (cc - 128) ** 2) < (40 ** 2)
        arr[mask] = min(1.0, self.forced_mean_prob * 4)
        return arr

class DemoEvacuationRouter:
    def find_nearest_shelters(self, start_point, num_shelters=3):
        lat, lon = start_point
        return [
            {"shelter": "Demo Shelter A", "distance_km": 0.9, "path": [(lat, lon), (lat+0.01, lon+0.01)], "shelter_point": (lat+0.01, lon+0.01)},
            {"shelter": "Demo Shelter B", "distance_km": 1.8, "path": [(lat, lon), (lat+0.02, lon+0.02)], "shelter_point": (lat+0.02, lon+0.02)},
            {"shelter": "Demo Shelter C", "distance_km": 2.5, "path": [(lat, lon), (lat+0.03, lon+0.03)], "shelter_point": (lat+0.03, lon+0.03)},
        ]
    def create_evacuation_map(self, start_point, shelters_data, flooded_areas=None):
        html = f"<html><body><h2>Demo evacuation map for {start_point}</h2>"
        for s in shelters_data:
            html += f"<p>{s['shelter']}: {s['distance_km']:.2f} km</p>"
        html += "</body></html>"
        return html

# -------------------------------------------------------------------------
# Helper: normalize router output into JSON-friendly shelters list
# -------------------------------------------------------------------------
def _to_latlon_tuple(pt):
    """
    Convert shapely geometry-like or tuple/list to (lat, lon).
    Returns (lat, lon) or None.
    """
    try:
        # shapely Point-like
        if hasattr(pt, "x") and hasattr(pt, "y"):
            return float(pt.y), float(pt.x)
        # tuple/list (lat, lon)
        if isinstance(pt, (tuple, list)) and len(pt) >= 2:
            a, b = pt[0], pt[1]
            return float(a), float(b)
    except Exception:
        pass
    return None

def normalize_shelters_for_json(router, start_point, raw_shelters, max_distance_km=10):
    """
    Convert router output (GeoDataFrame, list of dicts, or other) to list of dicts:
    [{'shelter':name, 'distance_km':float, 'path':[[lat,lon],...], 'shelter_point':[lat,lon]}]
    """
    results = []
    if raw_shelters is None:
        return results

    # Case: GeoDataFrame-like
    if hasattr(raw_shelters, "iterrows"):
        # attempt to compute routes using router._find_route_to_shelter if available
        try:
            start_node = router._find_nearest_node(start_point)
        except Exception:
            start_node = None
        for idx, row in raw_shelters.iterrows():
            try:
                geom = getattr(row, "geometry", None) or row.get("geometry")
                if geom is None:
                    continue
                # shelter point (lat, lon)
                if hasattr(geom, "geom_type") and geom.geom_type == "Point":
                    shelter_point = (geom.y, geom.x)
                else:
                    cent = geom.centroid
                    shelter_point = (cent.y, cent.x)
                shelter_name = row.get("name") or row.get("amenity") or f"Shelter_{idx}"
                # try route computation
                route_data = None
                try:
                    if start_node is None:
                        start_node = router._find_nearest_node(start_point)
                    route_data = router._find_route_to_shelter(start_node, shelter_point, shelter_name, max_distance_km)
                except Exception:
                    route_data = None
                if route_data:
                    path = [[float(p[0]), float(p[1])] for p in route_data.get("path", [])]
                    results.append({
                        "shelter": str(route_data.get("shelter", shelter_name)),
                        "distance_km": float(route_data.get("distance_km", 0.0)),
                        "path": path,
                        "shelter_point": [float(shelter_point[0]), float(shelter_point[1])]
                    })
            except Exception:
                continue
        return results

    # Case: already a list/tuple (likely dicts)
    if isinstance(raw_shelters, (list, tuple)):
        for entry in raw_shelters:
            try:
                if isinstance(entry, dict):
                    name = entry.get("shelter") or entry.get("name") or "Shelter"
                    dist = entry.get("distance_km") or entry.get("distance") or entry.get("dist") or 0.0
                    # normalize path
                    path_list = []
                    for p in entry.get("path", []):
                        # p may be (lat, lon) or (lon, lat) depending on source; try to detect
                        if isinstance(p, dict):
                            lat = p.get("lat") or p.get("y")
                            lon = p.get("lon") or p.get("x")
                            if lat is not None and lon is not None:
                                path_list.append([float(lat), float(lon)])
                        elif isinstance(p, (list, tuple)) and len(p) >= 2:
                            lat = float(p[0]); lon = float(p[1])
                            path_list.append([lat, lon])
                    # shelter_point
                    sp = entry.get("shelter_point") or entry.get("centroid") or entry.get("geometry")
                    shelter_ll = _to_latlon_tuple(sp)
                    if shelter_ll is None and path_list:
                        shelter_ll = (path_list[-1][0], path_list[-1][1])
                    results.append({
                        "shelter": str(name),
                        "distance_km": float(dist),
                        "path": path_list,
                        "shelter_point": [float(shelter_ll[0]), float(shelter_ll[1])] if shelter_ll else None
                    })
                else:
                    # shapely geometry or tuple
                    shelter_ll = _to_latlon_tuple(entry)
                    if shelter_ll:
                        results.append({
                            "shelter": "shelter",
                            "distance_km": 0.0,
                            "path": [[shelter_ll[0], shelter_ll[1]]],
                            "shelter_point": [shelter_ll[0], shelter_ll[1]]
                        })
            except Exception:
                continue
        return results

    return results

# -------------------------------------------------------------------------
# Core runner
# -------------------------------------------------------------------------
def run_demo_point(label, lat, lon, force_mock_models=True, forced_mean_prob=None, routing_real=True):
    out_dir = OUT_BASE / f"{label}_{lat:.6f}_{lon:.6f}"
    out_dir.mkdir(parents=True, exist_ok=True)

    # Models (mocked)
    if not force_mock_models:
        try:
            from infer_integrated import IntegratedDisasterAI
            ai = IntegratedDisasterAI()
        except Exception as e:
            print("Could not import real IntegratedDisasterAI, falling back to mock:", e)
            ai = MockIntegrated(forced_mean_prob=forced_mean_prob or 0.05)
    else:
        ai = MockIntegrated(forced_mean_prob=forced_mean_prob or 0.05)

    # Router: real or demo
    router = None
    if routing_real and router_real_available and EvacuationRouter is not None:
        try:
            router = EvacuationRouter()
            # attempt to load local or online OSM for this area (may fetch via Overpass)
            try:
                router.load_osm_data(location_point=(lat, lon), dist=5000)
            except Exception as inner:
                print("Router.load_osm_data issue (continuing):", inner)
        except Exception as e:
            print("Failed to initialize real EvacuationRouter, using demo router:", e)
            router = DemoEvacuationRouter()
    else:
        router = DemoEvacuationRouter()

    # Predict flood probability (mocked)
    flood_prob = ai.predict_flood_extent(None)
    mean_prob = float(np.nanmean(flood_prob))

    # Mock auxiliary inputs tuned by label for demo clarity
    historical_freq = 0.2 if label == "MED" else (0.05 if label == "LOW" else 0.6)
    elev_factor = 0.3 if label == "HIGH" else (0.6 if label == "LOW" else 0.45)
    rainfall = 0.1 if label == "LOW" else (0.5 if label == "MED" else 0.9)
    urban = 0.4

    risk = compute_risk_score(mean_prob, historical_freq, elev_factor, rainfall, urban)

    # Map state logic
    if risk < 0.2:
        state = "CLEAR"
    elif risk < 0.9:
        state = "RAINFALL_STARTED"
    else:
        state = "HIGH_RISK"

    shelters = []
    evac_map_html_path = None
    if state == "HIGH_RISK":
        # get raw shelters from router and normalize
        raw_shelters = None
        try:
            raw_shelters = router.find_nearest_shelters((lat, lon), num_shelters=3)
        except Exception as e:
            print("Warning: router.find_nearest_shelters() failed:", e)
            raw_shelters = None

        shelters = normalize_shelters_for_json(router, (lat, lon), raw_shelters, max_distance_km=10)

        # create/save folium map or HTML
        try:
            map_obj = router.create_evacuation_map((lat, lon), shelters)
            evac_map_path = out_dir / "evacuation_map.html"
            # handle folium.Map objects vs HTML string
            if hasattr(map_obj, "save"):
                map_obj.save(evac_map_path)
            else:
                with open(evac_map_path, "w", encoding="utf-8") as f:
                    f.write(str(map_obj))
            evac_map_html_path = str(evac_map_path)
        except Exception as e:
            print("Failed to create/save evacuation map:", e)

    report = {
        "timestamp": datetime.utcnow().isoformat(),
        "label": label,
        "lat": lat,
        "lon": lon,
        "state": state,
        "mean_unet_prob": mean_prob,
        "risk_score": round(risk, 4),
        "model_stats": {"probability_mean": round(mean_prob, 4)},
        "flood_extent": {"coverage": float(np.mean(flood_prob > 0.5)), "confidence": "demo"},
        "evacuation_map": evac_map_html_path,
        "shelters": shelters,
    }

    with open(out_dir / "emergency_report.json", "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2)

    # Simulate alerts (no real SMS/push)
    if state == "HIGH_RISK":
        alert_txt = f"ALERT (SIMULATED): HIGH RISK at {lat},{lon} (score={report['risk_score']})"
        if SIMULATE_ALERTS:
            with open(out_dir / "alert.txt", "w", encoding="utf-8") as f:
                f.write(alert_txt)
            print("Simulated alert written to alert.txt")
        else:
            print("SIMULATE_ALERTS is False — real alerting integration would run here")

    return report

# -------------------------------------------------------------------------
# Flask app & endpoints
# -------------------------------------------------------------------------
app = Flask(__name__)

@app.route("/run", methods=["GET"])
def run_endpoint():
    which = request.args.get("which", DEFAULT_SELECTION)
    which = which.upper()
    if which == "ALL":
        results = {}
        for label, (lat, lon) in PRESET_POINTS:
            r = run_demo_point(label, lat, lon, force_mock_models=FORCE_MOCK_MODELS, routing_real=USE_REAL_ROUTING)
            results[label] = r
        return jsonify(results)
    else:
        match = [p for p in PRESET_POINTS if p[0] == which]
        if not match:
            return jsonify({"error": "invalid which"}), 400
        label, (lat, lon) = match[0]
        r = run_demo_point(label, lat, lon, force_mock_models=FORCE_MOCK_MODELS, routing_real=USE_REAL_ROUTING)
        return jsonify(r)

@app.route("/report/<label>", methods=["GET"])
def report_endpoint(label):
    label = label.upper()
    dirs = sorted(OUT_BASE.glob(f"{label}_*_*"), key=os.path.getmtime, reverse=True)
    if not dirs:
        return jsonify({"error": "no report found"}), 404
    report_path = dirs[0] / "emergency_report.json"
    if not report_path.exists():
        return jsonify({"error": "report missing"}), 404
    with open(report_path, encoding="utf-8") as f:
        return jsonify(json.load(f))

@app.route("/map/<label>", methods=["GET"])
def map_endpoint(label):
    label = label.upper()
    dirs = sorted(OUT_BASE.glob(f"{label}_*_*"), key=os.path.getmtime, reverse=True)
    if not dirs:
        return jsonify({"error": "no map found"}), 404
    map_path = dirs[0] / "evacuation_map.html"
    if not map_path.exists():
        return jsonify({"error": "map not created for this run"}), 404
    return send_file(map_path)

@app.route("/list", methods=["GET"])
def list_endpoint():
    items = [p.name for p in sorted(OUT_BASE.iterdir(), key=os.path.getmtime, reverse=True)]
    return jsonify(items)

# run demo on startup in separate thread to keep server responsive
def startup_run():
    print(f"Running demo for DEFAULT_SELECTION={DEFAULT_SELECTION}")
    try:
        if DEFAULT_SELECTION == "ALL":
            for label, (lat, lon) in PRESET_POINTS:
                run_demo_point(label, lat, lon, force_mock_models=FORCE_MOCK_MODELS, routing_real=USE_REAL_ROUTING)
        else:
            match = [p for p in PRESET_POINTS if p[0] == DEFAULT_SELECTION]
            if match:
                label, (lat, lon) = match[0]
                run_demo_point(label, lat, lon, force_mock_models=FORCE_MOCK_MODELS, routing_real=USE_REAL_ROUTING)
    except Exception as e:
        print("Startup demo run failed:", e)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--which", choices=["ALL", "LOW", "MED", "HIGH"], help="Override DEFAULT_SELECTION for this run")
    args = parser.parse_args()
    if args.which:
        DEFAULT_SELECTION = args.which
    # run startup demo in thread
    t = threading.Thread(target=startup_run, daemon=True)
    t.start()

    app.run(host="0.0.0.0", port=5000, debug=False)
