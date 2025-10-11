import numpy as np
import torch
from torch.utils.data import Dataset
from pathlib import Path
import cv2
import albumentations as A
from albumentations.pytorch import ToTensorV2
from config import XBD_DIR

class XBBDamageDataset(Dataset):
    def __init__(self, root_dir, split="train", transform=None, image_size=(1024, 1024)):
        self.root_dir = Path(root_dir)
        self.split = split
        self.transform = transform
        self.image_size = image_size
        
        # Load .npy files based on split
        self.images = []
        self.labels = []
        
        if split == "train":
            file_prefixes = ["train_pre_image_chips", "train_post_image_chips", 
                           "train_pre_label_chips", "train_post_label_chips"]
        elif split == "val":
            file_prefixes = ["val_pre_image_chips", "val_post_image_chips",
                           "val_pre_label_chips", "val_post_label_chips"]
        else:
            file_prefixes = ["test_pre_image_chips", "test_post_image_chips"]
        
        # Load data from .npy files
        self._load_npy_data(file_prefixes)
        
        print(f"Loaded {len(self.images)} {split} samples from xBD dataset")
    
    def _load_npy_data(self, file_prefixes):
        """Load data from .npy files"""
        for prefix in file_prefixes:
            # Find all files matching the prefix
            npy_files = list(self.root_dir.glob(f"{prefix}_*.npy"))
            
            for npy_file in npy_files:
                try:
                    data = np.load(npy_file)
                    
                    if "pre_image" in prefix:
                        # Pre-disaster images
                        for i in range(len(data)):
                            self.images.append({
                                'pre_image': data[i],
                                'post_image': None,
                                'pre_label': None,
                                'post_label': None,
                                'index': len(self.images)
                            })
                    elif "post_image" in prefix:
                        # Post-disaster images - match with pre images
                        for i in range(min(len(data), len(self.images))):
                            if self.images[i]['post_image'] is None:
                                self.images[i]['post_image'] = data[i]
                    elif "pre_label" in prefix:
                        # Pre-disaster labels
                        for i in range(min(len(data), len(self.images))):
                            self.images[i]['pre_label'] = data[i]
                    elif "post_label" in prefix:
                        # Post-disaster labels - damage classification
                        for i in range(min(len(data), len(self.images))):
                            self.images[i]['post_label'] = data[i]
                            
                except Exception as e:
                    print(f"Error loading {npy_file}: {e}")
        
        # Remove incomplete samples
        self.images = [img for img in self.images if img['post_image'] is not None]
    
    def __len__(self):
        return len(self.images)
    
    def __getitem__(self, idx):
        sample = self.images[idx]
        
        # Get pre and post images
        pre_image = sample['pre_image']
        post_image = sample['post_image']
        
        # Ensure images are in correct format (H, W, C)
        if pre_image.shape[0] == 3:
            pre_image = pre_image.transpose(1, 2, 0)
        if post_image.shape[0] == 3:
            post_image = post_image.transpose(1, 2, 0)
        
        # Resize images
        pre_image = cv2.resize(pre_image, self.image_size)
        post_image = cv2.resize(post_image, self.image_size)
        
        # Get damage label
        damage_label = self._get_damage_level(sample)
        
        # Apply transformations
        if self.transform:
            transformed_pre = self.transform(image=pre_image)
            transformed_post = self.transform(image=post_image)
            
            pre_image = transformed_pre['image']
            post_image = transformed_post['image']
        else:
            pre_image = torch.from_numpy(pre_image.transpose(2, 0, 1)).float() / 255.0
            post_image = torch.from_numpy(post_image.transpose(2, 0, 1)).float() / 255.0
        
        return {
            'pre_image': pre_image,
            'post_image': post_image,
            'damage_level': damage_label,
            'index': idx
        }
    
    def _get_damage_level(self, sample):
        """Extract damage level from labels"""
        if sample['post_label'] is not None:
            unique_labels = np.unique(sample['post_label'])
            if len(unique_labels) > 1 or (len(unique_labels) == 1 and unique_labels[0] > 0):
                return 1
        return 0

def get_xbd_transforms(image_size=(1024, 1024)):
    train_transform = A.Compose([
        A.Resize(image_size[0], image_size[1]),
        A.HorizontalFlip(p=0.5),
        A.VerticalFlip(p=0.5),
        A.RandomRotate90(p=0.5),
        A.ColorJitter(brightness=0.2, contrast=0.2, saturation=0.2, hue=0.1, p=0.5),
        A.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
        ToTensorV2(),
    ])
    
    val_transform = A.Compose([
        A.Resize(image_size[0], image_size[1]),
        A.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
        ToTensorV2(),
    ])
    
    return train_transform, val_transform

if __name__ == "__main__":
    from config import verify_paths
    verify_paths()
    
    try:
        dataset = XBBDamageDataset(XBD_DIR, split="train")
        print(f"xBD dataset loaded: {len(dataset)} samples")
        
        if len(dataset) > 0:
            sample = dataset[0]
            print(f"Pre-image shape: {sample['pre_image'].shape}")
            print(f"Post-image shape: {sample['post_image'].shape}")
            print(f"Damage level: {sample['damage_level']}")
    except Exception as e:
        print(f"Error loading xBD dataset: {e}")