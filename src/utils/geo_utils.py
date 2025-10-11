import rasterio
import numpy as np
from rasterio.transform import from_bounds
import cv2

def reproject_mask_to_geotiff(mask, bounds, crs, output_path):
    """
    Reproject a flood mask to GeoTIFF format
    
    Args:
        mask: numpy array with flood probabilities
        bounds: (xmin, ymin, xmax, ymax) in target CRS
        crs: target coordinate reference system
        output_path: path to save GeoTIFF
    """
    height, width = mask.shape
    
    transform = from_bounds(bounds[0], bounds[1], bounds[2], bounds[3], width, height)
    
    with rasterio.open(
        output_path,
        'w',
        driver='GTiff',
        height=height,
        width=width,
        count=1,
        dtype=mask.dtype,
        crs=crs,
        transform=transform,
    ) as dst:
        dst.write(mask, 1)

def load_dem(dem_path):
    """Load DEM and return data with metadata"""
    try:
        with rasterio.open(dem_path) as dem:
            return dem.read(1), dem.transform, dem.crs
    except Exception as e:
        print(f"Error loading DEM: {e}")
        return None, None, None

def load_worldpop(pop_path):
    """Load WorldPop data"""
    try:
        with rasterio.open(pop_path) as pop:
            return pop.read(1), pop.transform, pop.crs
    except Exception as e:
        print(f"Error loading WorldPop data: {e}")
        return None, None, None

def align_rasters(source_data, source_transform, target_transform, target_shape):
    """Align raster data to target grid"""
    if source_data.shape != target_shape:
        resized = cv2.resize(source_data, (target_shape[1], target_shape[0]))
        return resized
    return source_data

def calculate_area(bounds):
    """Calculate area from bounds in square kilometers"""
    xmin, ymin, xmax, ymax = bounds
    # Rough approximation - in practice use proper geodesic calculation
    width_km = (xmax - xmin) * 111.32  # approx km per degree longitude
    height_km = (ymax - ymin) * 111.32  # approx km per degree latitude
    return width_km * height_km

def demo_geo_utils():
    """Demo the geo utilities"""
    from config import DEM_DIR
    
    dem_path = DEM_DIR / "Kerala_dem.tif"
    dem_data, transform, crs = load_dem(dem_path)
    
    if dem_data is not None:
        print(f"DEM loaded: {dem_data.shape}")
        print(f"Min elevation: {np.min(dem_data):.1f}")
        print(f"Max elevation: {np.max(dem_data):.1f}")
        print(f"CRS: {crs}")

if __name__ == "__main__":
    demo_geo_utils()