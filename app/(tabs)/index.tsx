import { useMemo, useState } from "react";
import { Alert, Image, Platform, Pressable, ScrollView, Share, StyleSheet, Text, View } from "react-native";
import * as ImagePicker from "expo-image-picker";
import * as ImageManipulator from "expo-image-manipulator";
import * as FileSystem from "expo-file-system/legacy";
import * as Sharing from "expo-sharing";
import * as Haptics from "expo-haptics";
import Svg, { Defs, LinearGradient, Polygon, Stop } from "react-native-svg";
import { ScreenContainer } from "@/components/screen-container";
import { IconSymbol } from "@/components/ui/icon-symbol";
import { buildDxf, buildHeightMap, makeSampleMap, reliefSvgPath, type HeightMap, type ReliefSettings } from "@/lib/vision";

const initialSettings: ReliefSettings = { depth: 12, contrast: 1.15, smoothing: 1, edgeBoost: 0.55 };
const presets = [
  { label: "ناعـم", depth: 8, contrast: 0.9, edgeBoost: 0.25 },
  { label: "متوازن", depth: 12, contrast: 1.15, edgeBoost: 0.55 },
  { label: "حاد", depth: 18, contrast: 1.45, edgeBoost: 0.85 },
];

export default function HomeScreen() {
  const [imageUri, setImageUri] = useState<string | null>(null);
  const [imageBase64, setImageBase64] = useState<string | null>(null);
  const [mime, setMime] = useState("image/jpeg");
  const [settings, setSettings] = useState(initialSettings);
  const [map, setMap] = useState<HeightMap>(makeSampleMap());
  const [processing, setProcessing] = useState(false);
  const [activeTab, setActiveTab] = useState<"studio" | "history">("studio");
  const [saved, setSaved] = useState<string[]>([]);

  const paths = useMemo(() => reliefSvgPath(map, 18, 320, 260), [map]);
  const stats = useMemo(() => [
    { label: "الدقة", value: `${map.width}×${map.height}` },
    { label: "العمق", value: `${settings.depth.toFixed(0)} mm` },
    { label: "الحواف", value: `${Math.round(map.edgeEnergy * 100)}%` },
  ], [map, settings.depth]);

  const feedback = async () => {
    if (Platform.OS !== "web") await Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
  };

  const processAsset = async (asset: ImagePicker.ImagePickerAsset) => {
    setProcessing(true);
    try {
      const optimized = await ImageManipulator.manipulateAsync(asset.uri, [{ resize: { width: 960 } }], { compress: 0.92, format: ImageManipulator.SaveFormat.JPEG, base64: true });
      const nextMime = asset.mimeType ?? "image/jpeg";
      setMime(nextMime);
      setImageUri(optimized.uri);
      setImageBase64(optimized.base64 ?? null);
      if (optimized.base64) setMap(buildHeightMap(optimized.base64, nextMime, settings));
    } catch (error) {
      Alert.alert("تعذر تحليل الصورة", "تأكد من أن الملف صورة صالحة ثم حاول مرة أخرى.");
    } finally {
      setProcessing(false);
    }
  };

  const pickImage = async () => {
    await feedback();
    const result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ImagePicker.MediaTypeOptions.Images, quality: 1, base64: true, allowsEditing: false });
    if (!result.canceled) await processAsset(result.assets[0]);
  };

  const takePhoto = async () => {
    await feedback();
    const permission = await ImagePicker.requestCameraPermissionsAsync();
    if (!permission.granted) return Alert.alert("إذن الكاميرا مطلوب", "اسمح للتطبيق باستخدام الكاميرا لالتقاط صورة للمجسم.");
    const result = await ImagePicker.launchCameraAsync({ quality: 1, base64: true });
    if (!result.canceled) await processAsset(result.assets[0]);
  };

  const applyPreset = (preset: typeof presets[number]) => {
    setSettings((current) => ({ ...current, depth: preset.depth, contrast: preset.contrast, edgeBoost: preset.edgeBoost }));
    if (imageBase64) setMap(buildHeightMap(imageBase64, mime, { ...settings, depth: preset.depth, contrast: preset.contrast, edgeBoost: preset.edgeBoost }));
  };

  const exportDxf = async () => {
    await feedback();
    const dxf = buildDxf(map, settings);
    const filename = `relief-${Date.now()}.dxf`;
    if (Platform.OS === "web") {
      await Share.share({ message: dxf, title: filename });
      return;
    }
    const uri = `${FileSystem.documentDirectory}${filename}`;
    await FileSystem.writeAsStringAsync(uri, dxf, { encoding: FileSystem.EncodingType.UTF8 });
    setSaved((items) => [filename, ...items.filter((item) => item !== filename)].slice(0, 5));
    if (await Sharing.isAvailableAsync()) await Sharing.shareAsync(uri, { mimeType: "application/dxf", dialogTitle: "تصدير نموذج البروز" });
    else Alert.alert("تم التصدير", `تم حفظ الملف في ${uri}`);
  };

  const changeSetting = (key: keyof ReliefSettings, delta: number) => {
    const next = { ...settings, [key]: Math.max(key === "smoothing" ? 0 : 0.2, settings[key] + delta) };
    setSettings(next);
    if (imageBase64) setMap(buildHeightMap(imageBase64, mime, next));
  };

  return (
    <ScreenContainer edges={["top", "left", "right"]} containerClassName="bg-[#07111A]" className="px-5">
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.content}>
        <View style={styles.header}>
          <View>
            <Text style={styles.eyebrow}>VISION LAB / ANDROID</Text>
            <Text style={styles.title}>Image to 3D</Text>
            <Text style={styles.subtitle}>حوّل الصورة إلى بروز قابل للتصنيع</Text>
          </View>
          <View style={styles.badge}><View style={styles.liveDot} /><Text style={styles.badgeText}>محرك نشط</Text></View>
        </View>

        <View style={styles.tabs}>
          {(["studio", "history"] as const).map((tab) => <Pressable key={tab} onPress={() => setActiveTab(tab)} style={[styles.tab, activeTab === tab && styles.activeTab]}><Text style={[styles.tabText, activeTab === tab && styles.activeTabText]}>{tab === "studio" ? "الاستوديو" : "المشاريع المحفوظة"}</Text></Pressable>)}
        </View>

        {activeTab === "history" ? <View style={styles.historyCard}><Text style={styles.sectionTitle}>آخر عمليات التصدير</Text>{saved.length ? saved.map((file) => <View key={file} style={styles.historyRow}><IconSymbol name="checkmark.circle.fill" size={20} color="#55D6BE" /><Text style={styles.historyText}>{file}</Text><Text style={styles.historyType}>DXF</Text></View>) : <Text style={styles.muted}>ستظهر هنا الملفات التي تم تصديرها من الجهاز.</Text>}</View> : <>
          <View style={styles.previewCard}>
            <View style={styles.previewHeader}><Text style={styles.sectionTitle}>المعاينة ثلاثية الأبعاد</Text><Text style={styles.previewHint}>{processing ? "جارٍ التحليل..." : "ISOMETRIC / RELIEF"}</Text></View>
            <View style={styles.preview}>
              {imageUri ? <Image source={{ uri: imageUri }} style={styles.sourceImage} resizeMode="cover" /> : <View style={styles.emptyPreview}><IconSymbol name="photo.fill" size={38} color="#55D6BE" /><Text style={styles.emptyTitle}>ابدأ بصورة واضحة</Text><Text style={styles.muted}>صورة أمامية بإضاءة متوازنة تعطي نتيجة أفضل</Text></View>}
              <View style={styles.reliefOverlay}><Svg width="100%" height="100%" viewBox="0 0 320 260"><Defs><LinearGradient id="depth" x1="0" y1="0" x2="1" y2="1"><Stop offset="0" stopColor="#55D6BE" stopOpacity="0.48" /><Stop offset="0.55" stopColor="#79A7FF" stopOpacity="0.22" /><Stop offset="1" stopColor="#07111A" stopOpacity="0.78" /></LinearGradient></Defs>{paths.map((path, index) => <Polygon key={index} points={path.replace(/[MLZ]/g, " ")} fill="url(#depth)" stroke="#B7FFF1" strokeOpacity={0.14} strokeWidth={0.5} />)}</Svg></View>
            </View>
            <View style={styles.stats}>{stats.map((item) => <View key={item.label} style={styles.stat}><Text style={styles.statValue}>{item.value}</Text><Text style={styles.statLabel}>{item.label}</Text></View>)}</View>
          </View>

          <View style={styles.actionRow}><Pressable onPress={pickImage} style={({ pressed }) => [styles.primaryButton, pressed && styles.pressed]}><IconSymbol name="photo.fill" size={19} color="#06121A" /><Text style={styles.primaryText}>اختيار صورة</Text></Pressable><Pressable onPress={takePhoto} style={({ pressed }) => [styles.secondaryButton, pressed && styles.pressed]}><IconSymbol name="camera.fill" size={19} color="#D7E9FF" /><Text style={styles.secondaryText}>الكاميرا</Text></Pressable></View>

          <View style={styles.panel}><View style={styles.panelHeader}><View><Text style={styles.sectionTitle}>معالجة الرؤية الحاسوبية</Text><Text style={styles.muted}>إضاءة → حواف → شبكة ارتفاعات</Text></View><View style={styles.engineTag}><Text style={styles.engineTagText}>LOCAL CV</Text></View></View>
            <View style={styles.presetRow}>{presets.map((preset) => <Pressable key={preset.label} onPress={() => applyPreset(preset)} style={styles.preset}><Text style={styles.presetText}>{preset.label}</Text><Text style={styles.presetValue}>{preset.depth}mm</Text></Pressable>)}</View>
            {[{ key: "depth", label: "عمق البروز", unit: "mm", step: 2 }, { key: "contrast", label: "تباين الارتفاع", unit: "×", step: 0.1 }, { key: "edgeBoost", label: "تقوية الحواف", unit: "%", step: 0.1 }].map((item) => <View key={item.key} style={styles.control}><View style={styles.controlLabel}><Text style={styles.controlName}>{item.label}</Text><Text style={styles.controlValue}>{item.key === "edgeBoost" ? `${Math.round(settings[item.key as keyof ReliefSettings] * 100)}${item.unit}` : `${settings[item.key as keyof ReliefSettings].toFixed(item.key === "depth" ? 0 : 1)}${item.unit}`}</Text></View><View style={styles.stepper}><Pressable onPress={() => changeSetting(item.key as keyof ReliefSettings, -item.step)} style={styles.step}><Text style={styles.stepText}>−</Text></Pressable><View style={styles.track}><View style={[styles.trackFill, { width: `${Math.min(100, Math.max(8, Number(settings[item.key as keyof ReliefSettings]) * (item.key === "depth" ? 4.5 : 58)))}%` }]} /></View><Pressable onPress={() => changeSetting(item.key as keyof ReliefSettings, item.step)} style={styles.step}><Text style={styles.stepText}>＋</Text></Pressable></View></View>)}
          </View>

          <Pressable onPress={exportDxf} style={({ pressed }) => [styles.exportButton, pressed && styles.pressed]}><IconSymbol name="arrow.down.doc.fill" size={20} color="#07111A" /><View><Text style={styles.exportTitle}>تصدير النموذج إلى DXF</Text><Text style={styles.exportSubtitle}>شبكة 3D Face جاهزة لـ CAD / CNC</Text></View><IconSymbol name="chevron.right" size={20} color="#07111A" /></Pressable>
          <Text style={styles.footer}>المعالجة تتم محليًا على الجهاز • لا يتم رفع صورك</Text>
        </>}
      </ScrollView>
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  content: { paddingTop: 18, paddingBottom: 32, gap: 16 }, header: { flexDirection: "row", justifyContent: "space-between", alignItems: "flex-start" }, eyebrow: { color: "#55D6BE", letterSpacing: 2, fontSize: 10, fontWeight: "800" }, title: { color: "#F6FBFF", fontSize: 34, fontWeight: "800", letterSpacing: -1 }, subtitle: { color: "#8BA2B7", fontSize: 14, marginTop: 4 }, badge: { borderWidth: 1, borderColor: "#1C665E", backgroundColor: "#0E292E", borderRadius: 99, paddingHorizontal: 10, paddingVertical: 7, flexDirection: "row", alignItems: "center", gap: 6 }, liveDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: "#55D6BE" }, badgeText: { color: "#9DF5E2", fontSize: 11, fontWeight: "700" }, tabs: { flexDirection: "row", backgroundColor: "#0D1C29", borderRadius: 12, padding: 4 }, tab: { flex: 1, paddingVertical: 10, alignItems: "center", borderRadius: 9 }, activeTab: { backgroundColor: "#193C4B" }, tabText: { color: "#71899E", fontSize: 12, fontWeight: "700" }, activeTabText: { color: "#D7F8F2" }, previewCard: { backgroundColor: "#0C1C2A", borderColor: "#1A3343", borderWidth: 1, borderRadius: 20, padding: 12 }, previewHeader: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }, sectionTitle: { color: "#E5F0F8", fontSize: 16, fontWeight: "800" }, previewHint: { color: "#55D6BE", fontSize: 9, letterSpacing: 1.2, fontWeight: "800" }, preview: { height: 260, backgroundColor: "#09131D", borderRadius: 14, overflow: "hidden", alignItems: "center", justifyContent: "center" }, sourceImage: { ...StyleSheet.absoluteFillObject, opacity: 0.28 }, reliefOverlay: { ...StyleSheet.absoluteFillObject }, emptyPreview: { alignItems: "center", gap: 8, paddingHorizontal: 28 }, emptyTitle: { color: "#D7E9FF", fontSize: 16, fontWeight: "800" }, muted: { color: "#6F879B", fontSize: 12, lineHeight: 18 }, stats: { flexDirection: "row", justifyContent: "space-around", paddingTop: 12 }, stat: { alignItems: "center", gap: 2 }, statValue: { color: "#B9D8EC", fontSize: 13, fontWeight: "800" }, statLabel: { color: "#668198", fontSize: 10 }, actionRow: { flexDirection: "row", gap: 10 }, primaryButton: { flex: 1, backgroundColor: "#55D6BE", borderRadius: 13, minHeight: 50, alignItems: "center", justifyContent: "center", flexDirection: "row", gap: 9 }, primaryText: { color: "#06121A", fontWeight: "800", fontSize: 14 }, secondaryButton: { flex: 0.72, backgroundColor: "#142A3A", borderColor: "#26485A", borderWidth: 1, borderRadius: 13, minHeight: 50, alignItems: "center", justifyContent: "center", flexDirection: "row", gap: 8 }, secondaryText: { color: "#D7E9FF", fontWeight: "800", fontSize: 14 }, panel: { backgroundColor: "#0B1926", borderColor: "#1A3343", borderWidth: 1, borderRadius: 18, padding: 16, gap: 16 }, panelHeader: { flexDirection: "row", justifyContent: "space-between" }, engineTag: { backgroundColor: "#132E3A", borderRadius: 7, paddingHorizontal: 8, paddingVertical: 5, alignSelf: "flex-start" }, engineTagText: { color: "#55D6BE", fontSize: 9, fontWeight: "800", letterSpacing: 1 }, presetRow: { flexDirection: "row", gap: 8 }, preset: { flex: 1, backgroundColor: "#102435", borderColor: "#21465A", borderWidth: 1, borderRadius: 10, paddingVertical: 9, alignItems: "center" }, presetText: { color: "#C9D9E7", fontWeight: "700", fontSize: 12 }, presetValue: { color: "#6F9DB4", fontSize: 10, marginTop: 3 }, control: { gap: 8 }, controlLabel: { flexDirection: "row", justifyContent: "space-between" }, controlName: { color: "#9DB5C9", fontSize: 12 }, controlValue: { color: "#55D6BE", fontSize: 12, fontWeight: "800" }, stepper: { flexDirection: "row", alignItems: "center", gap: 9 }, step: { width: 30, height: 30, borderRadius: 8, backgroundColor: "#142B3D", alignItems: "center", justifyContent: "center" }, stepText: { color: "#C8E6F5", fontSize: 17 }, track: { flex: 1, height: 5, backgroundColor: "#1D3547", borderRadius: 5, overflow: "hidden" }, trackFill: { height: "100%", backgroundColor: "#55D6BE", borderRadius: 5 }, exportButton: { backgroundColor: "#B7FFF1", minHeight: 66, borderRadius: 15, paddingHorizontal: 16, flexDirection: "row", alignItems: "center", gap: 12 }, exportTitle: { color: "#07111A", fontWeight: "900", fontSize: 14 }, exportSubtitle: { color: "#24545A", fontSize: 10, marginTop: 3 }, footer: { color: "#526B7C", textAlign: "center", fontSize: 10 }, pressed: { opacity: 0.78, transform: [{ scale: 0.985 }] }, historyCard: { backgroundColor: "#0C1C2A", borderColor: "#1A3343", borderWidth: 1, borderRadius: 18, padding: 16, gap: 14 }, historyRow: { flexDirection: "row", alignItems: "center", gap: 10, borderBottomColor: "#173044", borderBottomWidth: 1, paddingBottom: 12 }, historyText: { color: "#CDE1EF", flex: 1, fontSize: 12 }, historyType: { color: "#55D6BE", fontSize: 10, fontWeight: "800" },
});
