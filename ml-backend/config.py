from pathlib import Path
import os

# Base paths
BASE_DIR = Path(__file__).parent
DATA_DIR = BASE_DIR / "data"
MODELS_DIR = BASE_DIR / "models"

# Dataset paths
FLOODNET_DIR = DATA_DIR / "floodnet" / "FloodNet Challenge - Track 1"
XBD_DIR = DATA_DIR / "xbd"
OSM_DIR = DATA_DIR / "osm"
DEM_DIR = DATA_DIR / "dem"
WORLDPOP_DIR = DATA_DIR / "worldpop"

# Model paths
SEGMENTATION_MODEL_PATH = MODELS_DIR / "flood_segmentation_unet.pth"
DAMAGE_MODEL_PATH = MODELS_DIR / "building_damage_classifier.pth"

# Training config
SEGMENTATION_CONFIG = {
    "batch_size": 4,
    "learning_rate": 1e-4,
    "num_epochs": 50,
    "image_size": (512, 512),
    "num_classes": 2
}

DAMAGE_CONFIG = {
    "batch_size": 8,
    "learning_rate": 1e-4,
    "num_epochs": 2,
    "image_size": (224, 224),
    "num_classes": 2
}

# Verify paths exist
def verify_paths():
    paths_to_check = [
        FLOODNET_DIR,
        XBD_DIR,
        OSM_DIR,
        DEM_DIR,
        WORLDPOP_DIR
    ]
    
    for path in paths_to_check:
        if not path.exists():
            print(f"Warning: {path} does not exist")
        else:
            print(f"Found: {path}")
    
    return all(path.exists() for path in paths_to_check)

if __name__ == "__main__":
    verify_paths()