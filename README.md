# CarVision AI — Android Studio

Kotlin + Jetpack Compose sample app for:
- Selecting a car photo
- On-device car detection using Google ML Kit Image Labeling
- AI vision make/model identification
- Coarse color detection fallback

## Open in Android Studio
1. Extract the ZIP.
2. Open the `CarVisionAI` folder in Android Studio.
3. Let Gradle sync.
4. Run on an Android 8.0+ device/emulator.

## Configure AI make/model recognition
The project expects an AI vision endpoint compatible with the Google Gemini `generateContent` request shape.

Create/edit `local.properties` in the project root and add:
CARVISION_AI_URL=https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent
CARVISION_AI_KEY=YOUR_API_KEY

The Gradle file exposes these as BuildConfig values.

IMPORTANT:
Do not ship a private production API key directly inside a public APK. For production, replace the direct AI call with your own secure backend that stores the provider key server-side.

## If AI is not configured
The app still performs the on-device car check and provides a coarse color estimate, while showing that make/model AI is not configured.

## Notes
Make/model accuracy depends heavily on image quality, vehicle visibility, market/trim ambiguity, and the selected vision model. The AI prompt explicitly asks the model not to invent an exact make/model when it cannot determine it.
