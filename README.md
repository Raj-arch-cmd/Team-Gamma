# ResQTech: AI-Powered Disaster Management System

<div align="center">
  <img src="your_logo_url_here.png" alt="ResQTech Logo" width="150"/>
</div>

<p align="center">
  An AI-powered mobile application for disaster preparedness and response.
  <br />
  A hackathon project by Team Gamma.
</p>


## Problem Statement

During disasters, access to reliable and immediate information is crucial. Existing systems can be slow, leading to the spread of misinformation and leaving individuals without clear guidance on safety procedures, first aid, and available resources.



## Project Overview

**ResQTech** is a mobile platform designed to address these challenges. It uses AI and crowdsourcing to deliver verified information, AI-driven guidance, and real-time alerts directly to users, improving situational awareness and personal safety.



## Core Features

* **Instant SOS:** A floating action button allows users to transmit an SOS message with their current GPS coordinates to predefined emergency contacts.
* **AI Assistant:** A chatbot powered by the Gemini Pro API, providing answers to critical questions. It includes a local database (RoomDB) for offline access to essential FAQs on topics like first aid and emergency kits.
* **Crowdsourced Local Reports:** A feed where users can submit and view geolocated reports about local incidents, hazards, and safe zones.
* **Interactive Disaster Map:** A live map that visualizes official alerts and user-submitted reports, providing a real-time overview of the situation.
* **Preparedness Hub:** A central resource containing guides for various disaster scenarios and checklists for emergency kits.
* **ML Hotspot Prediction (Backend):** A machine learning model designed to analyze data and predict areas with a high risk of being impacted by a disaster.


## Technology Stack

| Component | Technology |
|---|---|
| **Android (Frontend)** | Kotlin, Jetpack Compose, Material 3, ViewModel, Coroutines, Navigation |
| **AI & Machine Learning** | Google Gemini Pro, Python, Flask/FastAPI |
| **Local Database** | RoomDB |
| **Cloud Database** | Firebase Firestore |
| **APIs** | Google Maps SDK |


## Setup and Installation

### Prerequisites
- Android Studio Iguana or later
- Android device or emulator (API 24+)

### Installation

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/Raj-arch-cmd/Team-Gamma.git](https://github.com/Raj-arch-cmd/Team-Gamma.git)
    ```

2.  **Configure API Keys:**
    Create a `local.properties` file in the project's root directory and add the necessary API keys:
    ```properties
    GEMINI_API_KEY="YOUR_GEMINI_API_KEY"// I cant sahre mines
    MAPS_API_KEY="YOUR_GOOGLE_MAPS_API_KEY"//// I cant sahre mines
    ```

3.  **Build and Run:**
    Open the project in Android Studio, allow Gradle to sync, and then build and run the application.



## Contributors

This project was developed by:

| Name | Role | GitHub Profile |
|---|---|---|
| **Raj (arch-cmd)** | Android Developer | [Raj-arch-cmd](https://github.com/Raj-arch-cmd) |
| **[Pranav-Patil3666]** | ML & Backend Developer | [Pranav-Patil3666] |

---

## License

This project is licensed under the MIT License. See the `LICENSE` file for more details.
