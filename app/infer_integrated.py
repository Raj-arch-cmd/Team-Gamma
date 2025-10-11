import torch
import cv2
import numpy as np
import matplotlib.pyplot as plt
from pathlib import Path
import json
from datetime import datetime

from models.unet import UNet
from models.damage_classifier import SiameseDamageClassifier
from src.datasets.floodnet_dataset import get_transforms
from src.routing.evacuation_router import EvacuationRouter
from src.emergency.risk_assessor import RiskAssessor
from config import *

# 🔹 Added: helper to make JSON safe for numpy / torch types
def make_json_serializable(obj):
    """Recursively convert numpy / torch types to native Python types for JSON saving"""
    import numpy as np
    import torch
    
    if isinstance(obj, dict):
        return {k: make_json_serializable(v) for k, v in obj.items()}
    elif isinstance(obj, list):
        return [make_json_serializable(v) for v in obj]
    elif isinstance(obj, (np.float32, np.float64, np.int32, np.int64)):
        return obj.item()
    elif isinstance(obj, torch.Tensor):
        return obj.item() if obj.numel() == 1 else obj.tolist()
    else:
        return obj


class IntegratedDisasterAI:
    def __init__(self):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        
        # Load flood segmentation model
        self.flood_model = UNet(in_channels=3, out_channels=1).to(self.device)
        if SEGMENTATION_MODEL_PATH.exists():
            self.flood_model.load_state_dict(torch.load(SEGMENTATION_MODEL_PATH, map_location=self.device))
            self.flood_model.eval()
            print("Flood segmentation model loaded")
        else:
            print("Flood segmentation model not found")
            
        # Load damage classification model
        self.damage_model = SiameseDamageClassifier(num_classes=2).to(self.device)
        if DAMAGE_MODEL_PATH.exists():
            self.damage_model.load_state_dict(torch.load(DAMAGE_MODEL_PATH, map_location=self.device))
            self.damage_model.eval()
            print("Damage classification model loaded")
        else:
            print("Damage classification model not found")
        
        # Initialize routing and risk assessment
        # ⚙️ Note: If you see an OSMnx error like "no attribute geometries_from_point",
        # open src/routing/evacuation_router.py and replace `geometries_from_point` with `features_from_point`
        self.router = EvacuationRouter(OSM_DIR / "southern_india.osm.pbf")
        self.risk_assessor = RiskAssessor(
            DEM_DIR / "Kerala_dem.tif",
            WORLDPOP_DIR / "worldpop_2020.tif"
        )
    
    def predict_flood_extent(self, image):
        """Predict flood extent from satellite image"""
        # Preprocess image
        if isinstance(image, str) or isinstance(image, Path):
            image = cv2.imread(str(image))
            image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        
        original_size = image.shape[:2]
        image_resized = cv2.resize(image, (512, 512))
        
        # Convert to tensor
        image_tensor = torch.from_numpy(image_resized.transpose(2, 0, 1)).float() / 255.0
        image_tensor = image_tensor.unsqueeze(0).to(self.device)
        
        # Predict
        with torch.no_grad():
            output = self.flood_model(image_tensor)
            prediction = torch.sigmoid(output).squeeze().cpu().numpy()
        
        # Resize back to original
        prediction_resized = cv2.resize(prediction, (original_size[1], original_size[0]))
        
        return prediction_resized
    
    def assess_building_damage(self, pre_image, post_image):
        """Assess building damage from pre and post disaster images"""
        # Resize images
        pre_resized = cv2.resize(pre_image, (224, 224))
        post_resized = cv2.resize(post_image, (224, 224))
        
        # Convert to tensors
        pre_tensor = torch.from_numpy(pre_resized.transpose(2, 0, 1)).float() / 255.0
        post_tensor = torch.from_numpy(post_resized.transpose(2, 0, 1)).float() / 255.0
        
        pre_tensor = pre_tensor.unsqueeze(0).to(self.device)
        post_tensor = post_tensor.unsqueeze(0).to(self.device)
        
        # Predict damage
        with torch.no_grad():
            outputs = self.damage_model(pre_tensor, post_tensor)
            probabilities = torch.softmax(outputs, dim=1)
            damage_prob = probabilities[0, 1].item()
        
        return damage_prob
    
    def generate_emergency_report(self, location, flood_mask=None):
        """Generate comprehensive emergency report"""
        report = {
            "timestamp": datetime.now().isoformat(),
            "location": location,
            "risk_assessment": {},
            "evacuation_routes": [],
            "damage_assessment": {},
            "recommendations": []
        }
        
        # Get evacuation routes
        shelters = self.router.find_nearest_shelters(location, num_shelters=3)
        report["evacuation_routes"] = [
            {
                "shelter": shelter['shelter'],
                "distance_km": shelter['distance_km'],
                "priority": "HIGH" if shelter['distance_km'] < 2 else "MEDIUM"
            }
            for shelter in shelters
        ]
        
        # Risk assessment
        if flood_mask is not None:
            risk_factors = self.risk_assessor.calculate_flood_risk(flood_mask, None)
            report["risk_assessment"] = risk_factors
            
            # Generate recommendations
            if risk_factors.get('total_risk', 0) > 0.7:
                report["recommendations"].append("IMMEDIATE EVACUATION REQUIRED")
                report["recommendations"].append("Avoid flooded areas")
                report["recommendations"].append("Head to nearest shelter immediately")
            else:
                report["recommendations"].append("Monitor situation")
                report["recommendations"].append("Prepare evacuation kit")
        
        return report
    
    def visualize_integrated_results(self, image, flood_mask, location):
        """Create comprehensive visualization"""
        fig, axes = plt.subplots(2, 2, figsize=(15, 12))
        
        # Original image
        axes[0, 0].imshow(image)
        axes[0, 0].set_title('Satellite Image')
        axes[0, 0].axis('off')
        
        # Flood mask
        axes[0, 1].imshow(flood_mask, cmap='jet')
        axes[0, 1].set_title('Flood Probability Map')
        axes[0, 1].axis('off')
        
        # Overlay
        axes[1, 0].imshow(image)
        axes[1, 0].imshow(flood_mask, cmap='jet', alpha=0.5)
        axes[1, 0].set_title('Flood Overlay')
        axes[1, 0].axis('off')
        
        # Risk assessment (text)
        report = self.generate_emergency_report(location, flood_mask)
        risk_text = f"Risk Level: {report['risk_assessment'].get('total_risk', 0):.2f}\n"
        risk_text += f"Population at Risk: {report['risk_assessment'].get('population_risk', 0)*100:.1f}%\n"
        risk_text += f"Nearest Shelter: {report['evacuation_routes'][0]['shelter'] if report['evacuation_routes'] else 'None'}"
        
        axes[1, 1].text(0.1, 0.5, risk_text, fontsize=12, verticalalignment='center')
        axes[1, 1].set_title('Risk Assessment')
        axes[1, 1].axis('off')
        
        plt.tight_layout()
        plt.savefig('integrated_analysis.png', dpi=150, bbox_inches='tight')
        plt.close()
        
        return report


def demo_integrated_system():
    """Demo the complete integrated system"""
    ai_system = IntegratedDisasterAI()
    
    # Test with sample data
    print("Running Integrated Disaster AI System...")
    
    # Example location (Kerala)
    location = (9.9312, 76.2673)
    
    # Create mock satellite image
    sample_image = np.random.rand(512, 512, 3)
    
    # Predict flood extent
    flood_mask = ai_system.predict_flood_extent(sample_image)
    print("Flood extent prediction completed")
    
    # Generate comprehensive report
    report = ai_system.generate_emergency_report(location, flood_mask)
    print("Emergency report generated")
    
    # Create visualization
    ai_system.visualize_integrated_results(sample_image, flood_mask, location)
    print("Visualization created")
    
    # Print key findings
    print("\nKEY FINDINGS:")
    print(f"Total Risk Score: {report['risk_assessment'].get('total_risk', 0):.2f}")
    if report['evacuation_routes']:
        print(f"Nearest Shelter: {report['evacuation_routes'][0]['shelter']}")
        print(f"Distance: {report['evacuation_routes'][0]['distance_km']:.1f} km")
    print(f"Recommendations: {', '.join(report['recommendations'])}")
    
    # 🔹 Fixed: convert before saving to JSON
    report_serializable = make_json_serializable(report)
    with open('emergency_report.json', 'w') as f:
        json.dump(report_serializable, f, indent=2)
    
    print("Report saved to emergency_report.json")


if __name__ == "__main__":
    demo_integrated_system()
