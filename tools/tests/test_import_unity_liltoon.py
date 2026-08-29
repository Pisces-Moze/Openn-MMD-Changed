"""Unit tests for the Unity/lilToon material importer's pure helpers.

These cover the parse/format helpers only; any path that would invoke Pillow
(non-PNG texture conversion) is intentionally left out so the suite runs with
the Python standard library alone.
"""
from pathlib import Path

from import_unity_liltoon import (
    build_guid_index,
    collect_prefab_materials,
    parse_material,
    safe_name,
    texture_asset,
    unity_color,
)


# --- safe_name ---------------------------------------------------------------

def test_safe_name_defaults_when_blank():
    assert safe_name("") == "material"
    assert safe_name("   ") == "material"


def test_safe_name_removes_invalid_chars():
    assert safe_name("My Body/Heads!!") == "My_Body_Heads"
    assert safe_name("hair_t") == "hair_t"


def test_safe_name_preserves_unicode_letters():
    assert safe_name("サメトラ") == "サメトラ"


# --- unity_color -------------------------------------------------------------

def test_unity_color_fallback():
    colors = {"_RimColor": [1.0, 2.0, 3.0, 4.0]}
    assert unity_color(colors, "_Missing", [0.0, 0.0, 0.0, 1.0]) == [0.0, 0.0, 0.0, 1.0]
    assert unity_color(colors, "_RimColor", [0.0, 0.0, 0.0, 1.0]) == [1.0, 2.0, 3.0, 4.0]


# --- texture_asset -----------------------------------------------------------

def test_texture_asset_looks_up_index(tmp_path):
    tex = tmp_path / "tex.png"
    tex.write_bytes(b"x")
    index = {"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa": tex}
    assert texture_asset({"guid": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"}, "guid", index) == tex
    assert texture_asset({}, "guid", index) is None
    assert texture_asset({"guid": "ffffffffffffffffffffffffffffffff"}, "guid", index) is None


# --- build_guid_index --------------------------------------------------------

def test_build_guid_index_maps_meta_to_asset(tmp_path):
    (tmp_path / "Body.png.meta").write_text("fileFormatVersion: 2\nguid: 0123456789abcdef0123456789abcdef\n", "utf-8")
    (tmp_path / "Body.png").write_bytes(b"png")
    index = build_guid_index(tmp_path)
    assert index["0123456789abcdef0123456789abcdef"] == tmp_path / "Body.png"


def test_build_guid_index_ignores_missing_guid(tmp_path):
    (tmp_path / "x.meta").write_text("no guid here\n", "utf-8")
    assert build_guid_index(tmp_path) == {}


# --- collect_prefab_materials ------------------------------------------------

def test_collect_prefab_materials_transitive(tmp_path):
    body = tmp_path / "Body.mat"
    body.write_text("mat", "utf-8")
    (tmp_path / "Body.mat.meta").write_text("guid: 11111111111111111111111111111111\n", "utf-8")

    child = tmp_path / "Child.prefab"
    child.write_text("guid: 11111111111111111111111111111111\n", "utf-8")
    (tmp_path / "Child.prefab.meta").write_text("guid: 22222222222222222222222222222222\n", "utf-8")

    root = tmp_path / "Root.prefab"
    root.write_text(
        "guid: 11111111111111111111111111111111\nguid: 22222222222222222222222222222222\n",
        "utf-8",
    )
    (tmp_path / "Root.prefab.meta").write_text("guid: 33333333333333333333333333333333\n", "utf-8")

    index = build_guid_index(tmp_path)
    materials = collect_prefab_materials(root, index)
    assert body.resolve() in materials


def test_collect_prefab_materials_ignores_other_types(tmp_path):
    # A referenced texture must not be returned as a material.
    tex = tmp_path / "body.png"
    tex.write_bytes(b"png")
    (tmp_path / "body.png.meta").write_text("guid: 00112233445566778899aabbccddeeff\n", "utf-8")

    root = tmp_path / "Root.prefab"
    root.write_text("guid: 00112233445566778899aabbccddeeff\n", "utf-8")
    (tmp_path / "Root.prefab.meta").write_text("guid: 33333333333333333333333333333333\n", "utf-8")

    index = build_guid_index(tmp_path)
    assert collect_prefab_materials(root, index) == set()


# --- parse_material ----------------------------------------------------------

def test_parse_material_reads_liltoon_fields(tmp_path):
    assets = tmp_path / "Assets"
    assets.mkdir()
    mat = assets / "body.mat"
    mat.write_text(
        "m_Name: Body\n"
        "  - _UseEmission: 1\n"
        "  - _AsUnlit: 0.4\n"
        "  - _ShadowColor: {r: 0.5, g: 0.6, b: 0.7, a: 1}\n"
        "  - _TransparentMode: 1\n"
        "  - _Cull: 2\n",
        "utf-8",
    )
    name, profile = parse_material(mat, assets, {}, tmp_path / "out", force=False)
    assert name == "Body"
    assert profile["useEmission"] is True
    assert profile["renderMode"] == "cutout"
    assert profile["cull"] == "back"
    assert profile["emissionTexture"] == ""
    assert profile["baseLightFloor"] == 0.0


def test_parse_material_culls_front_for_cull_1(tmp_path):
    assets = tmp_path / "Assets"
    assets.mkdir()
    mat = assets / "m.mat"
    mat.write_text('m_Name: M\n  - _Cull: 1\n', "utf-8")
    name, profile = parse_material(mat, assets, {}, tmp_path / "out", force=False)
    assert profile["cull"] == "front"
