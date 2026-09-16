import { toByteArray } from "base64-js";
import jpeg from "jpeg-js";
import { PNG } from "pngjs/browser";

export type HeightMap = {
  width: number;
  height: number;
  values: number[];
  min: number;
  max: number;
  mean: number;
  edgeEnergy: number;
};

export type ReliefSettings = {
  depth: number;
  contrast: number;
  smoothing: number;
  edgeBoost: number;
};

type Raster = { width: number; height: number; data: Uint8Array | Buffer };

function decodeRaster(base64: string, mime = "image/jpeg"): Raster {
  const bytes = toByteArray(base64);
  if (mime.toLowerCase().includes("png")) {
    const png = PNG.sync.read(Buffer.from(bytes));
    return { width: png.width, height: png.height, data: png.data };
  }
  const decoded = jpeg.decode(bytes, { useTArray: true });
  return { width: decoded.width, height: decoded.height, data: decoded.data };
}

function luminance(data: Uint8Array | Buffer, index: number) {
  return (0.2126 * data[index] + 0.7152 * data[index + 1] + 0.0722 * data[index + 2]) / 255;
}

export function buildHeightMap(base64: string, mime: string, settings: ReliefSettings, resolution = 32): HeightMap {
  const raster = decodeRaster(base64, mime);
  const gridW = Math.min(resolution, Math.max(12, raster.width));
  const gridH = Math.min(resolution, Math.max(12, Math.round((raster.height / raster.width) * gridW)));
  const raw: number[] = [];
  const sample = (gx: number, gy: number) => {
    const px = Math.min(raster.width - 1, Math.floor((gx / (gridW - 1)) * (raster.width - 1)));
    const py = Math.min(raster.height - 1, Math.floor((gy / (gridH - 1)) * (raster.height - 1)));
    return luminance(raster.data, (py * raster.width + px) * 4);
  };

  for (let y = 0; y < gridH; y += 1) {
    for (let x = 0; x < gridW; x += 1) {
      const center = sample(x, y);
      const left = sample(Math.max(0, x - 1), y);
      const right = sample(Math.min(gridW - 1, x + 1), y);
      const up = sample(x, Math.max(0, y - 1));
      const down = sample(x, Math.min(gridH - 1, y + 1));
      const gradient = Math.sqrt((right - left) ** 2 + (down - up) ** 2);
      const shaped = Math.pow(Math.max(0, Math.min(1, center)), 1 / Math.max(0.25, settings.contrast));
      raw.push(shaped * (1 - settings.edgeBoost * 0.35) + gradient * settings.edgeBoost);
    }
  }

  const radius = Math.round(Math.max(0, Math.min(2, settings.smoothing)));
  const values = raw.map((_, index) => {
    if (!radius) return raw[index];
    const x = index % gridW;
    const y = Math.floor(index / gridW);
    let total = 0;
    let count = 0;
    for (let oy = -radius; oy <= radius; oy += 1) {
      for (let ox = -radius; ox <= radius; ox += 1) {
        const nx = x + ox;
        const ny = y + oy;
        if (nx >= 0 && nx < gridW && ny >= 0 && ny < gridH) {
          total += raw[ny * gridW + nx];
          count += 1;
        }
      }
    }
    return total / count;
  });

  const min = Math.min(...values);
  const max = Math.max(...values);
  const normalized = values.map((value) => (value - min) / Math.max(0.0001, max - min));
  const mean = normalized.reduce((sum, value) => sum + value, 0) / normalized.length;
  const edgeEnergy = normalized.reduce((sum, value, index) => {
    const x = index % gridW;
    const y = Math.floor(index / gridW);
    return sum + (x ? Math.abs(value - normalized[index - 1]) : 0) + (y ? Math.abs(value - normalized[index - gridW]) : 0);
  }, 0) / normalized.length;
  return { width: gridW, height: gridH, values: normalized, min, max, mean, edgeEnergy };
}

export function reliefSvgPath(map: HeightMap, depth: number, width: number, height: number) {
  const paths: string[] = [];
  const cellW = width / (map.width - 1);
  const cellH = height / (map.height - 1);
  for (let y = 0; y < map.height - 1; y += 1) {
    for (let x = 0; x < map.width - 1; x += 1) {
      const a = map.values[y * map.width + x];
      const b = map.values[y * map.width + x + 1];
      const c = map.values[(y + 1) * map.width + x + 1];
      const d = map.values[(y + 1) * map.width + x];
      const z = (a + b + c + d) / 4;
      const ox = z * depth * 0.55;
      const oy = z * depth * 0.28;
      const p1 = `${x * cellW + ox},${y * cellH - oy}`;
      const p2 = `${(x + 1) * cellW + ox},${y * cellH - oy}`;
      const p3 = `${(x + 1) * cellW + ox},${(y + 1) * cellH - oy}`;
      const p4 = `${x * cellW + ox},${(y + 1) * cellH - oy}`;
      paths.push(`M ${p1} L ${p2} L ${p3} L ${p4} Z`);
    }
  }
  return paths;
}

export function buildDxf(map: HeightMap, settings: ReliefSettings, modelWidth = 100) {
  const scaleX = modelWidth / Math.max(1, map.width - 1);
  const scaleY = (modelWidth * map.height) / Math.max(1, map.width - 1) / map.height;
  const lines = ["0", "SECTION", "2", "ENTITIES"];
  const add3dFace = (points: Array<[number, number, number]>) => {
    lines.push("0", "3DFACE", "8", "RELIEF");
    const groups = [10, 11, 12, 13];
    points.slice(0, 4).forEach(([x, y, z], index) => {
      const group = groups[index];
      lines.push(String(group), x.toFixed(3), String(group + 10), y.toFixed(3), String(group + 20), z.toFixed(3));
    });
    if (points.length === 3) {
      lines.push("13", points[2][0].toFixed(3), "23", points[2][1].toFixed(3), "33", points[2][2].toFixed(3));
    }
  };
  for (let y = 0; y < map.height - 1; y += 1) {
    for (let x = 0; x < map.width - 1; x += 1) {
      const index = y * map.width + x;
      const z = (value: number) => value * settings.depth;
      const p = (px: number, py: number) => [px * scaleX, (map.height - 1 - py) * scaleY, z(map.values[py * map.width + px])] as [number, number, number];
      const a = p(x, y); const b = p(x + 1, y); const c = p(x + 1, y + 1); const d = p(x, y + 1);
      add3dFace([a, b, c, d]);
      if (index === 0) add3dFace([[0, 0, 0], [modelWidth, 0, 0], [modelWidth, modelWidth, 0], [0, modelWidth, 0]]);
    }
  }
  lines.push("0", "ENDSEC", "0", "EOF");
  return lines.join("\n");
}

export function makeSampleMap(): HeightMap {
  const width = 18;
  const height = 18;
  const values = Array.from({ length: width * height }, (_, i) => {
    const x = (i % width) / (width - 1) - 0.5;
    const y = Math.floor(i / width) / (height - 1) - 0.5;
    return Math.max(0, Math.min(1, 0.65 - Math.hypot(x, y) * 0.95 + Math.sin(x * 10) * 0.04));
  });
  return { width, height, values, min: 0, max: 1, mean: 0.46, edgeEnergy: 0.18 };
}
