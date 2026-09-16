import { describe, expect, it } from "vitest";
import { buildDxf, makeSampleMap } from "../lib/vision";

describe("relief vision pipeline", () => {
  it("creates a normalized sample height map", () => {
    const map = makeSampleMap();
    expect(map.values).toHaveLength(map.width * map.height);
    expect(Math.min(...map.values)).toBeGreaterThanOrEqual(0);
    expect(Math.max(...map.values)).toBeLessThanOrEqual(1);
    expect(map.edgeEnergy).toBeGreaterThan(0);
  });

  it("exports valid DXF 3DFACE entities with distinct coordinate groups", () => {
    const dxf = buildDxf(makeSampleMap(), { depth: 12, contrast: 1.15, smoothing: 1, edgeBoost: 0.55 });
    expect(dxf).toContain("3DFACE");
    expect(dxf).toContain("10\n0.000\n20");
    expect(dxf).toContain("11\n");
    expect(dxf).toContain("13\n");
    expect(dxf.endsWith("EOF")).toBe(true);
  });
});
