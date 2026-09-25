# VideoForge Android MVP

An Android-first animated explainer-video engine built independently from the same general workflow: scenes → live preview → deterministic MP4 export.

## MVP
- Native Android app
- 1280×720 / 16:9
- Five animated inflation scenes
- Preview and scene navigation
- H.264 MP4 export with Android MediaCodec + MediaMuxer
- No coding agent required
- No FFmpeg dependency for the first renderer

## Build
Open in Android Studio and sync Gradle. Then build/install the debug APK.

## Roadmap
Prompt/script → scene plan → style system → timeline → assets → voiceover → word timing → audio mixing → MP4.
