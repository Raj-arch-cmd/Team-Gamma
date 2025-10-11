import os
import cv2
import numpy as np
import torch
from torch.utils.data import Dataset
from pathlib import Path
import albumentations as A
from albumentations.pytorch import ToTensorV2
from config import FLOODNET_DIR

class FloodNetSegDataset(Dataset):
    def __init__(self, root_dir, split="Train", transform=None, image_size=(512, 512)):
        self.root_dir = Path(root_dir)
        self.split = split
        self.transform = transform
        self.image_size = image_size
        
        # Set paths based on split
        if split == "Train":
            self.image_dir = self.root_dir / "Train" / "Labeled" 
            self.mask_dir = self.root_dir / "Train" / "Labeled"
        elif split == "Validation":
            self.image_dir = self.root_dir / "Validation" / "image"
            self.mask_dir = self.root_dir / "Validation" / "mask"
        elif split == "Test":
            self.image_dir = self.root_dir / "Test" / "image"
            self.mask_dir = None
            
        # Collect image paths
        self.image_paths = []
        if split == "Train":
            # Train has Flooded and Unflooded subdirectories
            flooded_dir = self.image_dir / "Flooded" / "image"
            unflooded_dir = self.image_dir / "Unflooded" / "image"
            
            if flooded_dir.exists():
                self.image_paths.extend(list(flooded_dir.glob("*.jpg")))
            if unflooded_dir.exists():
                self.image_paths.extend(list(unflooded_dir.glob("*.jpg")))
        else:
            if self.image_dir.exists():
                self.image_paths = list(self.image_dir.glob("*.jpg"))
        
        print(f"Found {len(self.image_paths)} images in {split} split")
        
    def __len__(self):
        return len(self.image_paths)
    
    def get_mask_path(self, img_path):
        """Get corresponding mask path for an image"""
        if self.split == "Train":
            if "Flooded" in str(img_path):
                mask_dir = self.mask_dir / "Flooded" / "mask"
            else:
                mask_dir = self.mask_dir / "Unflooded" / "mask"
            mask_path = mask_dir / f"{img_path.stem}_lab.png"
        elif self.split == "Validation":
            mask_path = self.mask_dir / f"{img_path.stem}_lab.png"
        else:
            return None
            
        return mask_path if mask_path.exists() else None
    
    def __getitem__(self, idx):
        img_path = self.image_paths[idx]
        
        # Load image
        image = cv2.imread(str(img_path))
        image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        image = cv2.resize(image, self.image_size)
        
        # Load mask
        mask_path = self.get_mask_path(img_path)
        if mask_path and mask_path.exists():
            mask = cv2.imread(str(mask_path), cv2.IMREAD_GRAYSCALE)
            mask = cv2.resize(mask, self.image_size)
            
            # Convert to binary flood mask (assuming class 1 is flood)
            mask_binary = (mask == 1).astype(np.float32)
        else:
            # Create empty mask for test or if mask not found
            mask_binary = np.zeros(self.image_size, dtype=np.float32)
        
        # Apply transformations
        if self.transform:
            augmented = self.transform(image=image, mask=mask_binary)
            image = augmented['image']
            mask_binary = augmented['mask']
        else:
            # Default transform
            image = torch.from_numpy(image.transpose(2, 0, 1)).float() / 255.0
            mask_binary = torch.from_numpy(mask_binary).unsqueeze(0).float()
        
        return {
            'image': image,
            'mask': mask_binary,
            'image_path': str(img_path)
        }

def get_transforms(image_size=(512, 512)):
    train_transform = A.Compose([
        A.Resize(image_size[0], image_size[1]),
        A.HorizontalFlip(p=0.5),
        A.VerticalFlip(p=0.5),
        A.RandomRotate90(p=0.5),
        A.ShiftScaleRotate(p=0.5),
        A.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
        ToTensorV2(),
    ])
    
    val_transform = A.Compose([
        A.Resize(image_size[0], image_size[1]),
        A.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
        ToTensorV2(),
    ])
    
    return train_transform, val_transform

# Test the dataset
if __name__ == "__main__":
    from config import verify_paths
    verify_paths()
    
    dataset = FloodNetSegDataset(FLOODNET_DIR, split="Train")
    print(f"Dataset length: {len(dataset)}")
    
    sample = dataset[0]
    print(f"Image shape: {sample['image'].shape}")
    print(f"Mask shape: {sample['mask'].shape}")
    print(f"Unique mask values: {torch.unique(sample['mask'])}")