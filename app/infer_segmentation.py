import torch
import cv2
import numpy as np
import matplotlib.pyplot as plt
from pathlib import Path

from models.unet import UNet
from src.datasets.floodnet_dataset import FloodNetSegDataset, get_transforms
from config import SEGMENTATION_MODEL_PATH, SEGMENTATION_CONFIG, FLOODNET_DIR

class FloodSegmenter:
    def __init__(self, model_path):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = UNet(in_channels=3, out_channels=1).to(self.device)
        self.model.load_state_dict(torch.load(model_path, map_location=self.device))
        self.model.eval()
        
        _, self.transform = get_transforms(SEGMENTATION_CONFIG["image_size"])
        
    def predict(self, image):
        """Predict flood mask for input image"""
        # Preprocess image
        if isinstance(image, str) or isinstance(image, Path):
            image = cv2.imread(str(image))
            image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        
        original_size = image.shape[:2]
        image_resized = cv2.resize(image, SEGMENTATION_CONFIG["image_size"])
        
        # Transform
        transformed = self.transform(image=image_resized)
        input_tensor = transformed['image'].unsqueeze(0).to(self.device)
        
        # Predict
        with torch.no_grad():
            output = self.model(input_tensor)
            prediction = torch.sigmoid(output).squeeze().cpu().numpy()
        
        # Resize back to original size
        prediction_resized = cv2.resize(prediction, (original_size[1], original_size[0]))
        
        return prediction_resized

def demo_inference():
    """Run inference on sample FloodNet images"""
    if not SEGMENTATION_MODEL_PATH.exists():
        print("Model not found. Please train the model first.")
        return
    
    segmenter = FloodSegmenter(SEGMENTATION_MODEL_PATH)
    
    # Load test dataset
    test_dataset = FloodNetSegDataset(
        FLOODNET_DIR,
        split="Test",
        transform=None,
        image_size=SEGMENTATION_CONFIG["image_size"]
    )
    
    # Run inference on first few test images
    for i in range(min(3, len(test_dataset))):
        sample = test_dataset[i]
        image = sample['image'].permute(1, 2, 0).numpy() * 255
        image = image.astype(np.uint8)
        
        # Predict
        prediction = segmenter.predict(image)
        
        # Visualize
        fig, axes = plt.subplots(1, 2, figsize=(12, 6))
        axes[0].imshow(image)
        axes[0].set_title('Input Image')
        axes[0].axis('off')
        
        axes[1].imshow(prediction, cmap='jet', vmin=0, vmax=1)
        axes[1].set_title('Flood Probability')
        axes[1].axis('off')
        
        plt.tight_layout()
        plt.savefig(f'inference_result_{i}.png', dpi=150, bbox_inches='tight')
        plt.close()
        
        print(f"Saved inference result {i}")

if __name__ == "__main__":
    demo_inference()