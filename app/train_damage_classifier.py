import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader
import matplotlib.pyplot as plt
import numpy as np
from tqdm import tqdm
import os

from models.damage_classifier import SiameseDamageClassifier
from src.datasets.xbd_dataset import XBBDamageDataset, get_xbd_transforms
from config import DAMAGE_CONFIG, XBD_DIR, DAMAGE_MODEL_PATH

def train_damage_model():
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Using device: {device}")
    
    # Create model directory
    os.makedirs(DAMAGE_MODEL_PATH.parent, exist_ok=True)
    
    # Get transforms
    train_transform, val_transform = get_xbd_transforms(DAMAGE_CONFIG["image_size"])
    
    # Create datasets
    train_dataset = XBBDamageDataset(
        XBD_DIR,
        split="train",
        transform=train_transform,
        image_size=DAMAGE_CONFIG["image_size"]
    )
    
    val_dataset = XBBDamageDataset(
        XBD_DIR,
        split="val", 
        transform=val_transform,
        image_size=DAMAGE_CONFIG["image_size"]
    )
    
    # Create data loaders
    train_loader = DataLoader(
        train_dataset,
        batch_size=DAMAGE_CONFIG["batch_size"],
        shuffle=True,
        num_workers=2
    )
    
    val_loader = DataLoader(
        val_dataset,
        batch_size=DAMAGE_CONFIG["batch_size"],
        shuffle=False,
        num_workers=2
    )
    
    print(f"Training samples: {len(train_dataset)}")
    print(f"Validation samples: {len(val_dataset)}")
    
    # Initialize model
    model = SiameseDamageClassifier(num_classes=DAMAGE_CONFIG["num_classes"]).to(device)
    
    # Loss and optimizer
    criterion = nn.CrossEntropyLoss()
    optimizer = optim.Adam(model.parameters(), lr=DAMAGE_CONFIG["learning_rate"])
    scheduler = optim.lr_scheduler.StepLR(optimizer, step_size=10, gamma=0.1)
    
    # Training history
    train_losses = []
    val_losses = []
    train_accuracies = []
    val_accuracies = []
    
    best_val_accuracy = 0.0
    
    for epoch in range(DAMAGE_CONFIG["num_epochs"]):
        # Training phase
        model.train()
        train_loss = 0.0
        train_correct = 0
        train_total = 0
        
        train_pbar = tqdm(train_loader, desc=f'Epoch {epoch+1}/{DAMAGE_CONFIG["num_epochs"]} [Train]')
        for batch in train_pbar:
            pre_images = batch['pre_image'].to(device)
            post_images = batch['post_image'].to(device)
            labels = batch['damage_level'].to(device)
            
            # Forward pass
            optimizer.zero_grad()
            outputs = model(pre_images, post_images)
            loss = criterion(outputs, labels)
            
            # Backward pass
            loss.backward()
            optimizer.step()
            
            # Calculate accuracy
            _, predicted = torch.max(outputs.data, 1)
            train_total += labels.size(0)
            train_correct += (predicted == labels).sum().item()
            
            train_loss += loss.item()
            train_accuracy = 100 * train_correct / train_total
            
            train_pbar.set_postfix({
                'loss': f'{loss.item():.4f}',
                'acc': f'{train_accuracy:.2f}%'
            })
        
        # Validation phase
        model.eval()
        val_loss = 0.0
        val_correct = 0
        val_total = 0
        
        with torch.no_grad():
            val_pbar = tqdm(val_loader, desc=f'Epoch {epoch+1}/{DAMAGE_CONFIG["num_epochs"]} [Val]')
            for batch in val_pbar:
                pre_images = batch['pre_image'].to(device)
                post_images = batch['post_image'].to(device)
                labels = batch['damage_level'].to(device)
                
                outputs = model(pre_images, post_images)
                loss = criterion(outputs, labels)
                
                _, predicted = torch.max(outputs.data, 1)
                val_total += labels.size(0)
                val_correct += (predicted == labels).sum().item()
                
                val_loss += loss.item()
                val_accuracy = 100 * val_correct / val_total
                
                val_pbar.set_postfix({
                    'loss': f'{loss.item():.4f}',
                    'acc': f'{val_accuracy:.2f}%'
                })
        
        # Calculate averages
        avg_train_loss = train_loss / len(train_loader)
        avg_val_loss = val_loss / len(val_loader)
        avg_train_accuracy = 100 * train_correct / train_total
        avg_val_accuracy = 100 * val_correct / val_total
        
        train_losses.append(avg_train_loss)
        val_losses.append(avg_val_loss)
        train_accuracies.append(avg_train_accuracy)
        val_accuracies.append(avg_val_accuracy)
        
        print(f'Epoch {epoch+1}/{DAMAGE_CONFIG["num_epochs"]}:')
        print(f'  Train Loss: {avg_train_loss:.4f}, Train Acc: {avg_train_accuracy:.2f}%')
        print(f'  Val Loss: {avg_val_loss:.4f}, Val Acc: {avg_val_accuracy:.2f}%')
        
        # Save best model
        if avg_val_accuracy > best_val_accuracy:
            best_val_accuracy = avg_val_accuracy
            torch.save(model.state_dict(), DAMAGE_MODEL_PATH)
            print(f'  Saved best model with val_accuracy: {avg_val_accuracy:.2f}%')
        
        scheduler.step()
    
    # Plot training history
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 5))
    
    ax1.plot(train_losses, label='Training Loss')
    ax1.plot(val_losses, label='Validation Loss')
    ax1.set_xlabel('Epoch')
    ax1.set_ylabel('Loss')
    ax1.legend()
    ax1.set_title('Training and Validation Loss')
    
    ax2.plot(train_accuracies, label='Training Accuracy')
    ax2.plot(val_accuracies, label='Validation Accuracy')
    ax2.set_xlabel('Epoch')
    ax2.set_ylabel('Accuracy (%)')
    ax2.legend()
    ax2.set_title('Training and Validation Accuracy')
    
    plt.tight_layout()
    plt.savefig('damage_training_history.png', dpi=150, bbox_inches='tight')
    plt.close()
    
    print("Damage classification training completed!")
    print(f"Best model saved to: {DAMAGE_MODEL_PATH}")
    print(f"Best validation accuracy: {best_val_accuracy:.2f}%")

if __name__ == "__main__":
    train_damage_model()