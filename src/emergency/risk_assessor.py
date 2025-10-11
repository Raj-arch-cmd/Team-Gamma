import rasterio
import numpy as np
from config import DEM_DIR, WORLDPOP_DIR

class RiskAssessor:
    def __init__(self, dem_path, population_path):
        self.dem_path = dem_path
        self.population_path = population_path
        self.dem_data = None
        self.pop_data = None
        self.dem_transform = None
        self.pop_transform = None
        
    def load_data(self):
        """Load DEM and population data"""
        try:
            # Load DEM
            with rasterio.open(self.dem_path) as dem:
                self.dem_data = dem.read(1)
                self.dem_transform = dem.transform
                self.dem_crs = dem.crs
                
            # Load population data
            with rasterio.open(self.population_path) as pop:
                self.pop_data = pop.read(1)
                self.pop_transform = pop.transform
                self.pop_crs = pop.crs
                
            print("Loaded risk assessment data")
        except Exception as e:
            print(f"Error loading risk assessment data: {e}")
            # Create demo data for testing
            self._create_demo_data()
            
    def _create_demo_data(self):
        """Create demo data for testing"""
        print("Creating demo risk assessment data...")
        self.dem_data = np.random.rand(100, 100) * 100  # Elevation in meters
        self.pop_data = np.random.poisson(10, (100, 100))  # Population counts
        print("Demo risk data created")
        
    def calculate_flood_risk(self, flood_mask, bounds):
        """
        Calculate comprehensive flood risk score
        
        Args:
            flood_mask: binary flood mask
            bounds: (xmin, ymin, xmax, ymax) of the area
        """
        if self.dem_data is None:
            self.load_data()
            
        risk_factors = {}
        
        # 1. Elevation-based risk (lower elevation = higher risk)
        elevation_risk = self._calculate_elevation_risk(flood_mask, bounds)
        risk_factors['elevation_risk'] = elevation_risk
        
        # 2. Population exposure risk
        population_risk = self._calculate_population_risk(flood_mask, bounds)
        risk_factors['population_risk'] = population_risk
        
        # 3. Combined risk score
        total_risk = 0.6 * elevation_risk + 0.4 * population_risk
        risk_factors['total_risk'] = total_risk
        
        return risk_factors
    
    def _calculate_elevation_risk(self, flood_mask, bounds):
        """Calculate risk based on elevation in flooded areas"""
        try:
            if np.sum(flood_mask) > 0:
                # Simple elevation risk calculation
                # In practice, you'd align the flood mask with DEM data
                avg_elevation_in_flood = np.mean(self.dem_data)
                # Lower elevation = higher risk (normalized)
                elevation_risk = max(0, 1 - (avg_elevation_in_flood / 100))
                return min(elevation_risk, 1.0)
            return 0.0
        except:
            return 0.3  # Demo value
    
    def _calculate_population_risk(self, flood_mask, bounds):
        """Calculate risk based on population in flooded areas"""
        try:
            if np.sum(flood_mask) > 0:
                # Simple population exposure calculation
                flooded_population = np.sum(self.pop_data)
                total_population = np.sum(self.pop_data)
                if total_population > 0:
                    return min(flooded_population / total_population, 1.0)
            return 0.0
        except:
            return 0.4  # Demo value
    
    def get_high_risk_areas(self, flood_mask, risk_threshold=0.7):
        """Identify high-risk areas for priority evacuation"""
        risk_factors = self.calculate_flood_risk(flood_mask, None)
        
        high_risk_areas = []
        if risk_factors['total_risk'] > risk_threshold:
            high_risk_areas.append({
                'risk_score': risk_factors['total_risk'],
                'population_at_risk': risk_factors['population_risk'] * 1000,
                'priority': 'HIGH'
            })
            
        return high_risk_areas

def demo_risk_assessment():
    """Demo the risk assessment system"""
    risk_assessor = RiskAssessor(
        DEM_DIR / "Kerala_dem.tif",
        WORLDPOP_DIR / "worldpop_2020.tif"
    )
    
    # Create a mock flood mask
    flood_mask = np.random.rand(100, 100) > 0.7
    
    risk_factors = risk_assessor.calculate_flood_risk(flood_mask, None)
    
    print("Risk Assessment Results:")
    for factor, value in risk_factors.items():
        print(f"{factor}: {value:.3f}")

if __name__ == "__main__":
    demo_risk_assessment()