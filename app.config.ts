import "./scripts/load-env.js";
import type { ExpoConfig } from "expo/config";

const config: ExpoConfig = {
  name: "Image to 3D Extrusion",
  slug: "image-to-3d-extrusion-mobile",
  version: "1.0.0",
  orientation: "portrait",
  icon: "./assets/images/icon.png",
  scheme: "img3ddxf",
  userInterfaceStyle: "dark",
  newArchEnabled: true,
  ios: { supportsTablet: true, bundleIdentifier: "com.alish10e.img3ddxf", infoPlist: { ITSAppUsesNonExemptEncryption: false } },
  android: {
    adaptiveIcon: { backgroundColor: "#07111A", foregroundImage: "./assets/images/android-icon-foreground.png", backgroundImage: "./assets/images/android-icon-background.png", monochromeImage: "./assets/images/android-icon-monochrome.png" },
    edgeToEdgeEnabled: true,
    predictiveBackGestureEnabled: false,
    package: "com.alish10e.img3ddxf",
    permissions: ["POST_NOTIFICATIONS", "CAMERA", "READ_MEDIA_IMAGES"],
  },
  web: { bundler: "metro", output: "static", favicon: "./assets/images/favicon.png" },
  plugins: ["expo-router", ["expo-image-picker", { photosPermission: "اسمح للتطبيق باختيار صور لإنشاء نموذج ثلاثي الأبعاد.", cameraPermission: "اسمح للتطبيق بالتقاط صور لإنشاء نموذج ثلاثي الأبعاد." }], ["expo-splash-screen", { image: "./assets/images/splash-icon.png", imageWidth: 200, resizeMode: "contain", backgroundColor: "#07111A" }], ["expo-build-properties", { android: { buildArchs: ["armeabi-v7a", "arm64-v8a"], minSdkVersion: 24 } }]],
  experiments: { typedRoutes: true, reactCompiler: true },
};

export default config;
