import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader
import matplotlib.pyplot as plt
import numpy as np
from tqdm import tqdm
import os

from models.unet import UNet
from src.datasets.floodnet_dataset import FloodNetSegDataset, get_transforms
from config import SEGMENTATION_CONFIG, FLOODNET_DIR, SEGMENTATION_MODEL_PATH

# Set device
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
print(f"Using device: {device}")

def train_model():
    # Create model directory
    os.makedirs(SEGMENTATION_MODEL_PATH.parent, exist_ok=True)
    
    # Get transforms
    train_transform, val_transform = get_transforms(SEGMENTATION_CONFIG["image_size"])
    
    # Create datasets
    train_dataset = FloodNetSegDataset(
        FLOODNET_DIR, 
        split="Train", 
        transform=train_transform,
        image_size=SEGMENTATION_CONFIG["image_size"]
    )
    
    val_dataset = FloodNetSegDataset(
        FLOODNET_DIR,
        split="Validation", 
        transform=val_transform,
        image_size=SEGMENTATION_CONFIG["image_size"]
    )
    
    # Create data loaders
    train_loader = DataLoader(
        train_dataset,
        batch_size=SEGMENTATION_CONFIG["batch_size"],
        shuffle=True,
        num_workers=2
    )
    
    val_loader = DataLoader(
        val_dataset,
        batch_size=SEGMENTATION_CONFIG["batch_size"],
        shuffle=False,
        num_workers=2
    )
    
    print(f"Training samples: {len(train_dataset)}")
    print(f"Validation samples: {len(val_dataset)}")
    
    # Initialize model
    model = UNet(in_channels=3, out_channels=1).to(device)
    
    # Loss and optimizer
    criterion = nn.BCEWithLogitsLoss()
    optimizer = optim.Adam(model.parameters(), lr=SEGMENTATION_CONFIG["learning_rate"])
    
    # Training history
    train_losses = []
    val_losses = []
    
    best_val_loss = float('inf')
    
    for epoch in range(SEGMENTATION_CONFIG["num_epochs"]):
        # Training phase
        model.train()
        train_loss = 0.0
        
        train_pbar = tqdm(train_loader, desc=f'Epoch {epoch+1}/{SEGMENTATION_CONFIG["num_epochs"]} [Train]')
        for batch in train_pbar:
            images = batch['image'].to(device)
            masks = batch['mask'].unsqueeze(1).to(device)
            
            # Forward pass
            optimizer.zero_grad()
            outputs = model(images)
            loss = criterion(outputs, masks)
            
            # Backward pass
            loss.backward()
            optimizer.step()
            
            train_loss += loss.item()
            train_pbar.set_postfix({'loss': f'{loss.item():.4f}'})
        
        # Validation phase
        model.eval()
        val_loss = 0.0
        
        with torch.no_grad():
            val_pbar = tqdm(val_loader, desc=f'Epoch {epoch+1}/{SEGMENTATION_CONFIG["num_epochs"]} [Val]')
            for batch in val_pbar:
                images = batch['image'].to(device)
                masks = batch['mask'].unsqueeze(1).to(device)
                
                outputs = model(images)
                loss = criterion(outputs, masks)
                
                val_loss += loss.item()
                val_pbar.set_postfix({'loss': f'{loss.item():.4f}'})
        
        # Calculate average losses
        avg_train_loss = train_loss / len(train_loader)
        avg_val_loss = val_loss / len(val_loader)
        
        train_losses.append(avg_train_loss)
        val_losses.append(avg_val_loss)
        
        print(f'Epoch {epoch+1}/{SEGMENTATION_CONFIG["num_epochs"]}:')
        print(f'  Train Loss: {avg_train_loss:.4f}, Val Loss: {avg_val_loss:.4f}')
        
        # Save best model
        if avg_val_loss < best_val_loss:
            best_val_loss = avg_val_loss
            torch.save(model.state_dict(), SEGMENTATION_MODEL_PATH)
            print(f'  Saved best model with val_loss: {avg_val_loss:.4f}')
    
    # Plot training history
    plt.figure(figsize=(10, 5))
    plt.plot(train_losses, label='Training Loss')
    plt.plot(val_losses, label='Validation Loss')
    plt.xlabel('Epoch')
    plt.ylabel('Loss')
    plt.legend()
    plt.title('Training History')
    plt.savefig('training_history.png')
    plt.close()
    
    print("Training completed!")
    print(f"Best model saved to: {SEGMENTATION_MODEL_PATH}")

if __name__ == "__main__":
    train_model()