from fastapi import FastAPI, UploadFile, File, Form
from fastapi.responses import JSONResponse
import uvicorn
import os
from infer_integrated import IntegratedDisasterAI

app = FastAPI(title="Hyperlocal Flood AI API")

# Initialize once at startup
ai_system = IntegratedDisasterAI()

@app.post("/predict/")
async def predict(
    latitude: float = Form(...),
    longitude: float = Form(...),
    flood_image: UploadFile = File(...)
):
    """Receive location + image from frontend, run AI pipeline, and return risk results."""

    # Step 1: Save uploaded image temporarily
    image_path = f"temp_{flood_image.filename}"
    with open(image_path, "wb") as f:
        f.write(await flood_image.read())

    try:
        # Step 2: Run your integrated AI system
        location = (latitude, longitude)
        flood_mask = image_path  # or load it as array if your model expects that
        report = ai_system.generate_emergency_report(location, flood_mask)

        # Step 3: Return structured response
        return JSONResponse(content={
            "location": {"lat": latitude, "lon": longitude},
            "total_risk_score": report["total_risk_score"],
            "nearest_shelter": report["nearest_shelter"]["name"],
            "distance_km": report["nearest_shelter"]["distance_km"],
            "recommendations": report["recommendations"]
        })

    finally:
        # Cleanup
        if os.path.exists(image_path):
            os.remove(image_path)


if __name__ == "__main__":
    uvicorn.run("api_server:app", host="0.0.0.0", port=8000, reload=True)
