import csv
import sys
from pathlib import Path


def barycentric(px, py, tri):
    x0, y0, *_ = tri[0]
    x1, y1, *_ = tri[1]
    x2, y2, *_ = tri[2]
    den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
    if abs(den) < 1.0e-10:
        return None
    a = ((y1 - y2) * (px - x2) + (x2 - x1) * (py - y2)) / den
    b = ((y2 - y0) * (px - x2) + (x0 - x2) * (py - y2)) / den
    c = 1.0 - a - b
    if min(a, b, c) < -1.0e-6:
        return None
    return a, b, c


def sample(triangles, x, y):
    hits = []
    for tri_id, tri in enumerate(triangles):
        weights = barycentric(x, y, tri)
        if weights is None:
            continue
        z = sum(weights[i] * tri[i][2] for i in range(3))
        u = sum(weights[i] * tri[i][3] for i in range(3))
        v = sum(weights[i] * tri[i][4] for i in range(3))
        hits.append((z, u, v, tri_id, tri))
    # Looking from negative Z toward positive Z: the most negative surface wins.
    return min(hits) if hits else None


source = Path(sys.argv[1])
with source.open(newline="", encoding="utf-8") as handle:
    rows = csv.DictReader(handle)
    triangles = []
    for row in rows:
        triangles.append(tuple(
            tuple(float(row[f"{field}{corner}"]) for field in "xyzuv")
            for corner in range(3)
        ))

for side in (-1, 1):
    print("left" if side < 0 else "right")
    for y in (12.02, 11.94, 11.86, 11.78, 11.70, 11.62, 11.54):
        line = []
        for absolute_x in (0.22, 0.30, 0.38, 0.46, 0.54, 0.62):
            hit = sample(triangles, side * absolute_x, y)
            if hit is None:
                line.append("--")
            else:
                z, u, v, tri_id, _ = hit
                line.append(f"t{tri_id}:z{z:.3f}:uv{u:.3f},{v:.3f}")
        print(f"y={y:.2f} " + " | ".join(line))
