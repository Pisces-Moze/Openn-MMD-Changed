#!/usr/bin/env python3
"""Read-only Unity prefab/lilToon material importer.

The importer never writes inside the Unity project. It resolves GUID references,
copies referenced textures into the selected MMD model directory, and writes the
portable liltoon_materials.json consumed by the Java renderer.
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
import sys
from pathlib import Path

GUID_RE = re.compile(r"guid:\s*([0-9a-fA-F]{32})")
FLOAT_RE = re.compile(r"^\s*-\s+(_[A-Za-z0-9]+):\s*([-+0-9.eE]+)\s*$", re.MULTILINE)
COLOR_RE = re.compile(
    r"^\s*-\s+(_[A-Za-z0-9]+):\s*\{r:\s*([-+0-9.eE]+),\s*g:\s*([-+0-9.eE]+),"
    r"\s*b:\s*([-+0-9.eE]+),\s*a:\s*([-+0-9.eE]+)\}\s*$",
    re.MULTILINE,
)
TEXTURE_RE = re.compile(
    r"^\s*-\s+(_[A-Za-z0-9]+):\s*\r?\n\s*m_Texture:\s*"
    r"\{fileID:\s*[^,}]+(?:,\s*guid:\s*([0-9a-fA-F]{32}))?[^}]*\}",
    re.MULTILINE,
)
NAME_RE = re.compile(r"^\s*m_Name:\s*(.*?)\s*$", re.MULTILINE)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Import Unity lilToon materials for OpenMMD Changed")
    parser.add_argument("--unity-assets", required=True, type=Path, help="Unity project's Assets directory")
    parser.add_argument("--prefab", required=True, type=Path, help="Prefab below the Assets directory")
    parser.add_argument("--output", required=True, type=Path, help="MMD model directory receiving config/textures")
    parser.add_argument(
        "--material-dir", action="append", default=[], type=Path,
        help="Optional material directory to include (repeatable), useful for prefab variants",
    )
    parser.add_argument(
        "--material-dir-only", action="store_true",
        help="Import only --material-dir contents while retaining prefab provenance",
    )
    parser.add_argument("--force", action="store_true", help="Replace generated config and copied textures")
    return parser.parse_args()


def build_guid_index(assets: Path) -> dict[str, Path]:
    result: dict[str, Path] = {}
    for meta in assets.rglob("*.meta"):
        try:
            head = meta.read_text(encoding="utf-8", errors="ignore")[:4096]
        except OSError:
            continue
        match = GUID_RE.search(head)
        if match:
            result[match.group(1).lower()] = Path(str(meta)[:-5])
    return result


def collect_prefab_materials(prefab: Path, index: dict[str, Path]) -> set[Path]:
    materials: set[Path] = set()
    visited: set[Path] = set()

    def visit(asset: Path) -> None:
        asset = asset.resolve()
        if asset in visited or not asset.is_file():
            return
        visited.add(asset)
        try:
            text = asset.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            return
        for guid in GUID_RE.findall(text):
            target = index.get(guid.lower())
            if target is None:
                continue
            suffix = target.suffix.lower()
            if suffix == ".mat":
                materials.add(target.resolve())
            elif suffix == ".prefab":
                visit(target)

    visit(prefab)
    return materials


def safe_name(value: str) -> str:
    cleaned = re.sub(r"[^0-9A-Za-z._\-\u0080-\uffff]+", "_", value.strip())
    return cleaned.strip("._") or "material"


def unity_color(colors: dict[str, list[float]], name: str, default: list[float]) -> list[float]:
    return colors.get(name, default)


def texture_asset(textures: dict[str, str], name: str, index: dict[str, Path]) -> Path | None:
    guid = textures.get(name, "").lower()
    return index.get(guid) if guid else None


def copy_texture(source: Path | None, output: Path, material_name: str,
                 semantic: str, force: bool) -> str | None:
    if source is None or not source.is_file():
        return None
    filename = f"liltoon_{safe_name(material_name)}_{semantic}{source.suffix.lower()}"
    target = output / filename
    if target.exists() and not force:
        if target.read_bytes() != source.read_bytes():
            raise FileExistsError(f"Refusing to replace {target}; pass --force")
    else:
        shutil.copy2(source, target)
    return filename


def parse_material(path: Path, assets: Path, index: dict[str, Path],
                   output: Path, force: bool) -> tuple[str, dict]:
    text = path.read_text(encoding="utf-8", errors="ignore")
    name_match = NAME_RE.search(text)
    name = name_match.group(1).strip() if name_match else path.stem
    floats = {key: float(value) for key, value in FLOAT_RE.findall(text)}
    colors = {key: [float(r), float(g), float(b), float(a)] for key, r, g, b, a in COLOR_RE.findall(text)}
    textures = {key: guid for key, guid in TEXTURE_RE.findall(text) if guid}

    use_emission = floats.get("_UseEmission", 0.0) > 0.5
    use_normal = floats.get("_UseBumpMap", 0.0) > 0.5
    emission_source = texture_asset(textures, "_EmissionMap", index) if use_emission else None
    normal_source = texture_asset(textures, "_BumpMap", index) if use_normal else None
    emission_name = copy_texture(emission_source, output, name, "emission", force)
    normal_name = copy_texture(normal_source, output, name, "normal", force)

    cull_value = int(round(floats.get("_Cull", 2.0)))
    cull = {0: "off", 1: "front", 2: "back"}.get(cull_value, "back")
    transparent = int(round(floats.get("_TransparentMode", 0.0)))
    render_mode = {0: "opaque", 1: "cutout", 2: "transparent"}.get(transparent, "opaque")
    rim_color = unity_color(colors, "_RimColor", [1.0, 1.0, 1.0, 1.0])
    emission_color = unity_color(colors, "_EmissionColor", [1.0, 1.0, 1.0, 1.0])

    profile = {
        "aliases": [],
        "useShadow": floats.get("_UseShadow", 0.0) > 0.5,
        "shadowBorder": floats.get("_ShadowBorder", 0.5),
        "shadowBlur": floats.get("_ShadowBlur", 0.1),
        "shadowColor": unity_color(colors, "_ShadowColor", [0.82, 0.76, 0.85, 1.0]),
        "useRim": floats.get("_UseRim", 0.0) > 0.5,
        "rimBorder": floats.get("_RimBorder", 0.5),
        "rimBlur": floats.get("_RimBlur", 0.65),
        "rimFresnelPower": floats.get("_RimFresnelPower", 3.5),
        "rimIntensity": rim_color[3],
        "rimColor": rim_color,
        "useMatCap": floats.get("_UseMatCap", 0.0) > 0.5,
        "matCapStrength": floats.get("_MatCapBlend", 0.0),
        "useEmission": use_emission,
        "emissionStrength": floats.get("_EmissionBlend", 1.0),
        # Opt-in Minecraft extension. Set this above zero in the generated JSON
        # when saturated cyan areas of the main texture are fluorescent.
        "cyanEmissionStrength": 0.0,
        "emissionTexture": emission_name or "",
        "emissionColor": emission_color,
        "normalTexture": normal_name or "",
        "normalScale": floats.get("_BumpScale", 1.0),
        "cull": cull,
        "renderMode": render_mode,
        "alphaCutoff": floats.get("_Cutoff", 0.5),
        "useOutline": floats.get("_OutlineWidth", 0.0) > 0.0,
        "outlineWidth": floats.get("_OutlineWidth", 0.0),
        "outlineColor": unity_color(colors, "_OutlineColor", [0.0, 0.0, 0.0, 1.0]),
        "unityMaterial": path.relative_to(assets).as_posix(),
    }
    return name, profile


def main() -> int:
    args = parse_args()
    assets = args.unity_assets.resolve()
    prefab = args.prefab.resolve()
    output = args.output.resolve()
    if not assets.is_dir() or not prefab.is_file():
        print("Unity Assets directory or prefab does not exist", file=sys.stderr)
        return 2
    if assets not in prefab.parents:
        print("Prefab must be located below --unity-assets", file=sys.stderr)
        return 2

    output.mkdir(parents=True, exist_ok=True)
    index = build_guid_index(assets)
    materials = set() if args.material_dir_only else collect_prefab_materials(prefab, index)
    for directory in args.material_dir:
        resolved = directory.resolve()
        if assets not in resolved.parents and resolved != assets:
            print(f"Ignoring material directory outside Assets: {resolved}", file=sys.stderr)
            continue
        materials.update(path.resolve() for path in resolved.rglob("*.mat"))

    profiles: dict[str, dict] = {}
    duplicate_names: list[str] = []
    for material in sorted(materials, key=lambda item: str(item).lower()):
        name, profile = parse_material(material, assets, index, output, args.force)
        if name in profiles:
            duplicate_names.append(name)
            continue
        profiles[name] = profile

    config = {
        "schemaVersion": 1,
        "sourcePrefab": prefab.relative_to(assets).as_posix(),
        "materials": profiles,
    }
    target = output / "liltoon_materials.json"
    if target.exists() and not args.force:
        raise FileExistsError(f"Refusing to replace {target}; pass --force")
    target.write_text(json.dumps(config, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Imported {len(profiles)} materials into {target}")
    if duplicate_names:
        print("Duplicate material names skipped; use --material-dir more narrowly or edit aliases: "
              + ", ".join(sorted(set(duplicate_names))), file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
