# E-HerbDoc

E-HerbDoc is a plant-photo identification app. The Android client is written in Kotlin and uses the project's FastAPI backend to send a selected or captured photo to PlantNet. Users can review candidate matches and revisit identification history.

Photo identification is not proof that a plant is safe or medicinal. The app does not provide a verified medicinal-use or dosage database. Confirm a species with a qualified local expert before using a plant medicinally.

## Android app

Open `android-app/` in Android Studio. Install Android SDK Platform 35 and use Java 17. Let Gradle sync, select an emulator or connected Android phone, and press **Run**. The app includes a home screen, camera capture, photo upload, identification results, plant information, and history.

## Backend

The Android client expects the FastAPI service to be running. Keep the PlantNet key on the server in `backend/.env` as `PLANTNET_API_KEY=your_key`; do not put it in the Android app.

From the repository root, create and install the Python environment, then start the API:

```powershell
py -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r backend\requirements.txt
.\.venv\Scripts\python.exe -m uvicorn main:app --app-dir backend --host 0.0.0.0 --port 8000
```

In the Android app, use `http://10.0.2.2:8000` with an emulator. For a physical phone on the same Wi-Fi network, set the API address on the app's Home screen to `http://YOUR_COMPUTER_LAN_IP:8000`. For public use, deploy the backend at an HTTPS address and configure that URL in the app.

The app calls `GET /health`, `POST /api/identify` (multipart field `file`), and `GET /api/history`.
