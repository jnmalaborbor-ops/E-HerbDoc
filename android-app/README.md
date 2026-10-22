# E-HerbDoc Android app (Kotlin)

This Android client keeps the existing E-HerbDoc workflow: choose a plant image, identify it through the existing backend, and view saved identification history. It does not replace or modify the website/backend.

The interface has a home screen, camera and gallery actions, identification results, plant information, history, and API connection settings. Camera capture uses the Android camera app; gallery upload uses Android's photo picker.

## Backend API used

- `GET /health`
- `POST /api/identify` with multipart field `file`
- `GET /api/history`

The PlantNet API key belongs in `backend/.env` as `PLANTNET_API_KEY=your_key`. The Android app only calls your E-HerbDoc backend; never put a PlantNet secret in the app.

## Run in Android Studio

1. In Android Studio, open the `android-app` folder (the one containing `settings.gradle.kts`). Let Gradle sync finish. This project uses Android Gradle Plugin 8.13.2 with the Gradle 8.13 wrapper, Android SDK 35, and Java 17. If prompted, install Android SDK Platform 35 and use Gradle JDK 17.
2. Start the existing API from the repository root in a terminal. If the project `.venv` already exists, run:

   ```powershell
   cd C:\Users\FAITH\Desktop\e-herb
   .\.venv\Scripts\python.exe -m uvicorn main:app --app-dir backend --host 0.0.0.0 --port 8000
   ```

   If `.venv` does not exist yet, install Python, then run `py -m venv .venv`, `.\.venv\Scripts\python.exe -m pip install -r backend\requirements.txt`, and the Uvicorn command above. Keep the PlantNet key in `backend\.env`.

3. Android Emulator: leave API URL as `http://10.0.2.2:8000` (this emulator address points to the development computer).
4. Physical Android phone: connect phone and computer to the same Wi-Fi, allow port 8000 through the computer firewall, and set the API URL on the app's Home screen to `http://YOUR_COMPUTER_LAN_IP:8000` (for example `http://192.168.1.20:8000`). Find the computer IP with `ipconfig`. The server must bind to `0.0.0.0` as above. The address is saved automatically.
5. Tap **Run** in Android Studio, select an emulator or connected phone, then choose **Take photo** or **Upload photo** and tap **Identify this plant**. Use **History** to revisit saved results. History uses the existing backend database.

For use outside a trusted development network, deploy the API behind HTTPS and enter its HTTPS base URL. The development manifest permits HTTP for LAN testing.

## Build an installable APK

In Android Studio choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**. The debug APK is normally at `app/build/outputs/apk/debug/app-debug.apk`. Transfer it to the phone and open it to install (Android may ask you to permit installs from that file app). To distribute broadly, use **Build > Generate Signed App Bundle or APK** and create a signed release build.

## API behavior and limitations

Image requests use the backend's `/api/identify`; the backend enforces accepted file types and its 10 MB upload limit. Android sends the selected image as JPEG data. The API URL is saved locally in app preferences. For a real phone, `10.0.2.2` will not work; use the computer LAN address or a deployed HTTPS URL.
