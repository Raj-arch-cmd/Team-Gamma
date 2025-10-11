import matplotlib.pyplot as plt
import numpy as np
import folium
import cv2

def plot_flood_prediction(image, flood_mask, save_path=None):
    """Plot flood prediction results"""
    fig, axes = plt.subplots(1, 3, figsize=(15, 5))
    
    # Original image
    axes[0].imshow(image)
    axes[0].set_title('Original Image')
    axes[0].axis('off')
    
    # Flood mask
    im = axes[1].imshow(flood_mask, cmap='jet', vmin=0, vmax=1)
    axes[1].set_title('Flood Probability')
    axes[1].axis('off')
    plt.colorbar(im, ax=axes[1])
    
    # Overlay
    axes[2].imshow(image)
    axes[2].imshow(flood_mask, cmap='jet', alpha=0.5)
    axes[2].set_title('Flood Overlay')
    axes[2].axis('off')
    
    plt.tight_layout()
    
    if save_path:
        plt.savefig(save_path, dpi=150, bbox_inches='tight')
        print(f"Plot saved to {save_path}")
    
    return fig

def create_risk_heatmap(risk_data, bounds, save_path=None):
    """Create a risk heatmap visualization"""
    fig, ax = plt.subplots(figsize=(10, 8))
    
    im = ax.imshow(risk_data, cmap='RdYlGn_r', extent=bounds)
    ax.set_title('Flood Risk Heatmap')
    ax.set_xlabel('Longitude')
    ax.set_ylabel('Latitude')
    plt.colorbar(im, ax=ax, label='Risk Level')
    
    if save_path:
        plt.savefig(save_path, dpi=150, bbox_inches='tight')
    
    return fig

def plot_training_history(train_losses, val_losses, train_accuracies=None, val_accuracies=None, save_path=None):
    """Plot training history"""
    if train_accuracies is not None and val_accuracies is not None:
        fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 5))
        
        # Loss plot
        ax1.plot(train_losses, label='Training Loss')
        ax1.plot(val_losses, label='Validation Loss')
        ax1.set_xlabel('Epoch')
        ax1.set_ylabel('Loss')
        ax1.legend()
        ax1.set_title('Training and Validation Loss')
        
        # Accuracy plot
        ax2.plot(train_accuracies, label='Training Accuracy')
        ax2.plot(val_accuracies, label='Validation Accuracy')
        ax2.set_xlabel('Epoch')
        ax2.set_ylabel('Accuracy (%)')
        ax2.legend()
        ax2.set_title('Training and Validation Accuracy')
    else:
        fig, ax = plt.subplots(figsize=(10, 5))
        ax.plot(train_losses, label='Training Loss')
        ax.plot(val_losses, label='Validation Loss')
        ax.set_xlabel('Epoch')
        ax.set_ylabel('Loss')
        ax.legend()
        ax.set_title('Training History')
    
    plt.tight_layout()
    
    if save_path:
        plt.savefig(save_path, dpi=150, bbox_inches='tight')
    
    return fig

def create_evacuation_visualization(start_point, shelters, flooded_areas=None, save_path='evacuation_map.html'):
    """Create an interactive evacuation map"""
    m = folium.Map(location=start_point, zoom_start=13)
    
    # Add start point
    folium.Marker(
        start_point,
        popup="Your Location",
        tooltip="Start here",
        icon=folium.Icon(color='red', icon='home')
    ).add_to(m)
    
    # Add shelters with different colors
    colors = ['blue', 'green', 'purple', 'orange', 'darkred']
    
    for i, shelter in enumerate(shelters):
        color = colors[i % len(colors)]
        
        folium.Marker(
            shelter['shelter_point'],
            popup=f"{shelter['shelter']}<br>Distance: {shelter['distance_km']:.1f} km",
            tooltip=shelter['shelter'],
            icon=folium.Icon(color=color, icon='star')
        ).add_to(m)
        
        # Add route line
        folium.PolyLine(
            shelter['path'],
            popup=f"Route to {shelter['shelter']}",
            color=color,
            weight=4,
            opacity=0.7
        ).add_to(m)
    
    # Add flooded areas if provided
    if flooded_areas:
        for i, area in enumerate(flooded_areas):
            folium.Polygon(
                area,
                popup=f"Flooded Area {i+1}",
                color='red',
                fill=True,
                fill_color='red',
                fill_opacity=0.3
            ).add_to(m)
    
    # Save map
    m.save(save_path)
    print(f"Evacuation map saved to {save_path}")
    
    return m

def demo_visualization():
    """Demo visualization functions"""
    # Create sample data
    image = np.random.rand(100, 100, 3)
    flood_mask = np.random.rand(100, 100)
    
    # Plot flood prediction
    plot_flood_prediction(image, flood_mask, 'demo_flood_plot.png')
    
    # Create sample evacuation data
    start_point = (10.0, 76.3)
    shelters = [
        {
            'shelter': 'Hospital A',
            'distance_km': 2.5,
            'path': [(10.0, 76.3), (10.01, 76.31), (10.02, 76.32)],
            'shelter_point': (10.02, 76.32)
        }
    ]
    
    create_evacuation_visualization(start_point, shelters)

if __name__ == "__main__":
    demo_visualization()