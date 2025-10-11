"""
Run point-based flood inference using post-flood Sentinel-2 SAFE (optical-only).
Example:
python infer_point.py --lat 9.9312 --lon 76.2673 --post_safe "data/sentinel2_post/S2B_MSIL2A_20180822T050649_N0500_R019_T43PFL_20230629T103326.SAFE" --window 512 --out_dir outputs/point_runs/run1
"""
import argparse, json, warnings, numpy as np, rasterio, cv2, matplotlib.pyplot as plt
from pathlib import Path
from datetime import datetime
from rasterio.warp import reproject, Resampling, transform
warnings.filterwarnings("ignore", category=UserWarning)

# Import your existing integrated model
from infer_integrated import IntegratedDisasterAI


# --------------------------------------------------------------------
# ----------------------- Helper Functions ---------------------------
# --------------------------------------------------------------------
def find_file_loose(safe_dir: Path, substrings):
    """Search recursively for any file containing all given substrings (case-insensitive)."""
    safe_dir = Path(safe_dir)
    for p in safe_dir.rglob("*.jp2"):
        name = p.name.lower()
        if all(s.lower() in name for s in substrings):
            return str(p)
    return None


def read_band(path):
    with rasterio.open(path) as src:
        arr = src.read(1)
        meta = src.meta.copy()
    return arr, meta


def reproject_if_needed(src_arr, src_meta, dst_meta, resampling=Resampling.bilinear):
    if (
        src_meta["crs"] == dst_meta["crs"]
        and src_meta["transform"] == dst_meta["transform"]
        and src_meta["width"] == dst_meta["width"]
        and src_meta["height"] == dst_meta["height"]
    ):
        return src_arr
    dst = np.empty((dst_meta["height"], dst_meta["width"]), dtype=src_arr.dtype)
    reproject(
        source=src_arr,
        destination=dst,
        src_transform=src_meta["transform"],
        src_crs=src_meta["crs"],
        dst_transform=dst_meta["transform"],
        dst_crs=dst_meta["crs"],
        resampling=resampling,
    )
    return dst


def scl_cloud_mask(scl_arr):
    """More conservative cloud masking - only high confidence clouds/shadows."""
    # SCL values: 0=nodata, 1=saturated/defective, 2=dark_area, 3=cloud_shadow, 
    # 4=vegetation, 5=bare_soil, 6=water, 7=unclassified, 8=cloud_medium_prob, 
    # 9=cloud_high_prob, 10=thin_cirrus, 11=snow
    return np.isin(scl_arr, [0, 1, 3, 8, 9, 10])  # Include nodata and defective pixels


def percentile_stretch_uint8(band):
    bandf = band.astype("float32")
    valid = bandf[bandf > 0]
    if valid.size == 0:
        return np.zeros_like(band, dtype="uint8")
    p2, p98 = np.percentile(valid, (2, 98))
    scaled = np.clip((bandf - p2) / (p98 - p2 + 1e-6), 0, 1)
    return (scaled * 255).astype("uint8")


def lonlat_to_raster_xy(lon, lat, dst_crs):
    xs, ys = transform("EPSG:4326", dst_crs, [lon], [lat])
    return xs[0], ys[0]


def crop_window(src, x, y, window_px):
    row, col = src.index(x, y)
    half = window_px // 2
    row0, row1 = max(0, row - half), min(src.height, row + half)
    col0, col1 = max(0, col - half), min(src.width, col + half)
    window = rasterio.windows.Window(col0, row0, col1 - col0, row1 - row0)
    arr = src.read(1, window=window)
    meta = src.meta.copy()
    meta.update({"height": arr.shape[0], "width": arr.shape[1], "transform": rasterio.windows.transform(window, src.transform)})
    return arr, meta, window


def prepare_rgb_for_model(rgb_array, clear_mask):
    """Prepare RGB for model inference with better handling."""
    # Create a masked array for model input
    model_input = rgb_array.copy().astype("float32")
    
    # Normalize to 0-1 range
    model_input = model_input / 255.0
    
    # For cloudy pixels, use a neutral value instead of zero
    # This helps the model handle partial cloud coverage
    cloud_pixels = ~clear_mask
    if cloud_pixels.any():
        # Use median of clear pixels for cloudy areas
        for channel in range(3):
            clear_values = model_input[clear_mask, channel]
            if len(clear_values) > 0:
                median_val = np.median(clear_values)
                model_input[cloud_pixels, channel] = median_val
    
    return model_input


def enhance_contrast(rgb_array):
    """Enhance contrast for better visualization in cloudy conditions."""
    # Convert to HSV color space
    hsv = cv2.cvtColor(rgb_array, cv2.COLOR_RGB2HSV)
    
    # Enhance saturation and value
    hsv[:, :, 1] = cv2.equalizeHist(hsv[:, :, 1])  # Enhance saturation
    hsv[:, :, 2] = cv2.equalizeHist(hsv[:, :, 2])  # Enhance brightness
    
    # Convert back to RGB
    enhanced = cv2.cvtColor(hsv, cv2.COLOR_HSV2RGB)
    return enhanced


# --------------------------------------------------------------------
# ------------------------- Main Logic -------------------------------
# --------------------------------------------------------------------
def process_point(lat, lon, post_safe, window_px=512, out_dir="outputs/run", unet_prob_thresh=0.3):
    post_safe = Path(post_safe)
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    # --- Find Sentinel-2 bands ---
    post_b02 = find_file_loose(post_safe, ["b02", "10m"])
    post_b03 = find_file_loose(post_safe, ["b03", "10m"])
    post_b04 = find_file_loose(post_safe, ["b04", "10m"])
    post_scl = find_file_loose(post_safe, ["scl"])  # 10m or 20m both fine

    print("📁 Found files:")
    print("B02:", post_b02)
    print("B03:", post_b03)
    print("B04:", post_b04)
    print("SCL:", post_scl)

    if not all([post_b02, post_b03, post_b04, post_scl]):
        raise RuntimeError("❌ Missing required bands/SCL in post SAFE. Check your folder structure.")

    post_b02_arr, post_meta = read_band(post_b02)
    post_b03_arr, _ = read_band(post_b03)
    post_b04_arr, _ = read_band(post_b04)
    post_scl_arr, scl_meta = read_band(post_scl)

    if scl_meta["crs"] != post_meta["crs"] or scl_meta["transform"] != post_meta["transform"]:
        post_scl_arr = reproject_if_needed(post_scl_arr, scl_meta, post_meta, Resampling.nearest)

    x, y = lonlat_to_raster_xy(lon, lat, post_meta["crs"])

    with rasterio.open(post_b04) as src:
        try:
            row, col = src.index(x, y)
        except Exception:
            row, col = -1, -1
        inside = 0 <= row < src.height and 0 <= col < src.width

    report = {
        "timestamp": datetime.utcnow().isoformat(),
        "lat": lat,
        "lon": lon,
        "inside_post_tile": inside,
        "notes": [],
    }

    if not inside:
        report["notes"].append("Point outside post tile.")
        with open(out_dir / "emergency_report.json", "w") as f:
            json.dump(report, f, indent=2)
        print("⚠️ Point outside tile. Report written.")
        return report

    # --- Crop window around point ---
    with rasterio.open(post_b04) as s04:
        b04_win, win_meta, window = crop_window(s04, x, y, window_px)
    with rasterio.open(post_b02) as s02:
        b02_win = s02.read(1, window=window)
    with rasterio.open(post_b03) as s03:
        b03_win = s03.read(1, window=window)

    scl_win = reproject_if_needed(post_scl_arr, post_meta, win_meta, resampling=Resampling.nearest)
    cloud_mask = scl_cloud_mask(scl_win)
    clear_mask = ~cloud_mask
    clear_frac = np.count_nonzero(clear_mask) / clear_mask.size
    
    print(f"🔍 Cloud stats: Total pixels: {clear_mask.size}, Clear pixels: {np.count_nonzero(clear_mask)}")
    print(f"🔍 Clear fraction: {clear_frac:.3f}")

    # Check if we have sufficient clear pixels
    if clear_frac < 0.05:  # At least 5% clear pixels
        report["notes"].append(f"Insufficient clear pixels: {clear_frac:.3f}")
        report["flood_extent"] = {
            "coverage": 0.0,
            "confidence": "very_low",
            "source": "optical",
            "nodata_pct": 1.0,
            "clear_fraction": clear_frac,
            "status": "insufficient_clear_data"
        }
        with open(out_dir / "emergency_report.json", "w") as f:
            json.dump(report, f, indent=2)
        print("❌ Insufficient clear pixels for analysis.")
        return report

    rgb = np.dstack([
        percentile_stretch_uint8(b04_win),
        percentile_stretch_uint8(b03_win),
        percentile_stretch_uint8(b02_win),
    ])
    
    # Create masked RGB for visualization
    rgb_masked = rgb.copy()
    rgb_masked[cloud_mask] = 0

    # Prepare enhanced input for model in cloudy conditions
    if clear_frac < 0.3:
        print("🌥️ Low clear fraction - using enhanced processing")
        rgb_enhanced = enhance_contrast(rgb)
        model_input = prepare_rgb_for_model(rgb_enhanced, clear_mask)
    else:
        model_input = prepare_rgb_for_model(rgb, clear_mask)

    # --- Model inference ---
    print("🚀 Running UNet model...")
    ai = IntegratedDisasterAI()
    unet_prob = ai.predict_flood_extent(model_input)

    # Debug model output
    print(f"🤖 Model output range: {unet_prob.min():.3f} to {unet_prob.max():.3f}")
    print(f"🤖 Model output stats - Min: {unet_prob.min():.3f}, Max: {unet_prob.max():.3f}, Mean: {unet_prob.mean():.3f}")

    # Adjust threshold based on cloud coverage
    if clear_frac < 0.3:
        adjusted_threshold = max(0.2, unet_prob_thresh - 0.1)  # Lower threshold for cloudy scenes
    else:
        adjusted_threshold = unet_prob_thresh
        
    print(f"🎯 Using probability threshold: {adjusted_threshold:.2f}")

    unet_bin = (unet_prob >= adjusted_threshold).astype("uint8")
    print(f"🤖 Binary mask - Flood pixels: {np.count_nonzero(unet_bin)}, Total: {unet_bin.size}")

    # Create final mask
    final_mask = np.zeros_like(unet_prob, dtype="float32")
    final_mask[unet_bin == 1] = 1.0  # Flood areas
    final_mask[cloud_mask] = np.nan  # Cloud areas as NaN

    # Calculate statistics on valid (non-cloud) pixels only
    valid_mask = ~np.isnan(final_mask)
    valid_pixel_count = np.count_nonzero(valid_mask)
    flooded_pixel_count = np.count_nonzero(final_mask[valid_mask] > 0.5)

    if valid_pixel_count > 0:
        coverage = flooded_pixel_count / valid_pixel_count
        nodata_pct = (cloud_mask.sum() / final_mask.size)  # Actual cloud percentage
    else:
        coverage = 0.0
        nodata_pct = 1.0

    print(f"📊 Statistics: Valid pixels: {valid_pixel_count}, Flooded: {flooded_pixel_count}")
    print(f"📊 Coverage: {coverage:.3f}, Nodata %: {nodata_pct:.3f}")

    # --- Save outputs ---
    single_meta = win_meta.copy()
    single_meta.update(count=1, dtype="float32")

    # Save as GeoTIFF
    out_mask = out_dir / "flood_mask_window.tif"

    # Convert flood probability (float 0–1) → 0–255 uint8
    mask_uint8 = (np.nan_to_num(final_mask, nan=0) * 255).astype("uint8")

    # Update metadata for GeoTIFF and uint8
    single_meta.update({
        "dtype": "uint8",
        "driver": "GTiff",
        "nodata": 0
    })

    with rasterio.open(out_mask, "w", **single_meta) as dst:
        dst.write(mask_uint8, 1)

    # --- Overlay image ---
    overlay = (rgb.astype("float32") / 255.0)
    mask_color = plt.cm.jet(np.nan_to_num(final_mask))[:, :, :3]
    overlay[~np.isnan(final_mask)] = 0.5 * overlay[~np.isnan(final_mask)] + 0.5 * mask_color[~np.isnan(final_mask)]
    cv2.imwrite(str(out_dir / "overlay_window.png"), cv2.cvtColor((overlay * 255).astype("uint8"), cv2.COLOR_RGB2BGR))

    # --- Create debug visualization ---
    plt.figure(figsize=(15, 5))
    plt.subplot(1, 4, 1)
    plt.imshow(rgb)
    plt.title('RGB Input')
    plt.axis('off')
    
    plt.subplot(1, 4, 2)
    plt.imshow(cloud_mask, cmap='hot')
    plt.title(f'Cloud Mask (clear: {clear_frac:.1%})')
    plt.axis('off')
    
    plt.subplot(1, 4, 3)
    plt.imshow(unet_prob, cmap='RdBu_r', vmin=0, vmax=1)
    plt.title('Model Probability')
    plt.axis('off')
    plt.colorbar()
    
    plt.subplot(1, 4, 4)
    plt.imshow(final_mask, cmap='coolwarm', vmin=0, vmax=1)
    plt.title(f'Final Mask (flood: {coverage:.1%})')
    plt.axis('off')
    plt.colorbar()
    
    plt.tight_layout()
    plt.savefig(out_dir / 'debug_analysis.png', dpi=150, bbox_inches='tight')
    plt.close()

    # --- Compute confidence level ---
    if clear_frac > 0.7:
        confidence = "high"
    elif clear_frac > 0.4:
        confidence = "medium"
    elif clear_frac > 0.1:
        confidence = "low"
    else:
        confidence = "very_low"

    report.update({
        "clear_fraction": round(clear_frac, 3),
        "mask_tif": str(out_mask),
        "overlay_png": str(out_dir / "overlay_window.png"),
        "debug_png": str(out_dir / "debug_analysis.png"),
        "model_stats": {
            "probability_min": round(float(unet_prob.min()), 3),
            "probability_max": round(float(unet_prob.max()), 3),
            "probability_mean": round(float(unet_prob.mean()), 3),
            "threshold_used": round(adjusted_threshold, 2)
        },
        "flood_extent": {
            "coverage": round(float(coverage), 3),
            "confidence": confidence,
            "source": "optical",
            "nodata_pct": round(float(nodata_pct), 3),
            "valid_pixels": int(valid_pixel_count),
            "flooded_pixels": int(flooded_pixel_count),
            "output_mask": str(out_mask).replace("\\", "/")
        }
    })

    with open(out_dir / "emergency_report.json", "w") as f:
        json.dump(report, f, indent=2)

    print("✅ Done. Outputs in:", out_dir)
    print(f"📋 Summary: Clear: {clear_frac:.1%}, Flood coverage: {coverage:.1%}, Confidence: {confidence}")
    return report


# --------------------------------------------------------------------
# --------------------------- CLI -----------------------------------
# --------------------------------------------------------------------
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Run point-based flood inference (Sentinel-2 SAFE)")
    parser.add_argument("--lat", type=float, required=True)
    parser.add_argument("--lon", type=float, required=True)
    parser.add_argument("--post_safe", type=str, required=True)
    parser.add_argument("--window", type=int, default=512)
    parser.add_argument("--out_dir", type=str, default="outputs/point_runs/run")
    parser.add_argument("--unet_prob_thresh", type=float, default=0.3)  # Lower default threshold
    args = parser.parse_args()

    rpt = process_point(args.lat, args.lon, args.post_safe, args.window, args.out_dir, args.unet_prob_thresh)
    
    print(json.dumps(rpt, indent=2))