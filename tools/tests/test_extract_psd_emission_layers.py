"""Unit tests for the PSD emission-layer exporter's pure helpers.

These only need the Python standard library: Pillow and psd-tools are imported
lazily inside main() and are never required here.
"""
import pytest

from extract_psd_emission_layers import normalized_name, resolve_layer


class Node:
    """Minimal stand-in for a psd-tools PSD layer/group."""

    def __init__(self, name: str, children=None):
        self.name = name
        self._children = children or []

    def __iter__(self):
        return iter(self._children)


# --- normalized_name ---------------------------------------------------------

def test_normalized_name_ignores_whitespace_and_case():
    assert normalized_name("图层 5") == normalized_name("图层5")
    assert normalized_name("EMI_eyes") == normalized_name("emi_eyes")


# --- resolve_layer -----------------------------------------------------------

def test_resolve_layer_nested_path():
    tree = [
        Node("サメトラ", [
            Node("目", [Node("瞳"), Node("黒")]),
            Node("图层 5"),
        ])
    ]
    node, actual = resolve_layer(tree, "サメトラ/目/瞳")
    assert actual == "サメトラ/目/瞳"
    assert node.name == "瞳"


def test_resolve_layer_whitespace_tolerant_component():
    tree = [Node("サメトラ", [Node("图层 5")])]
    node, actual = resolve_layer(tree, "サメトラ/图层5")
    assert actual == "サメトラ/图层 5"


def test_resolve_layer_missing_raises():
    tree = [Node("A", [Node("B")])]
    with pytest.raises(ValueError, match="找不到图层路径"):
        resolve_layer(tree, "A/Missing")


def test_resolve_layer_ambiguous_raises():
    tree = [Node("A", [Node("B"), Node("B")])]
    with pytest.raises(ValueError, match="不唯一"):
        resolve_layer(tree, "A/B")


def test_resolve_layer_empty_path_raises():
    tree = [Node("A")]
    with pytest.raises(ValueError, match="不能是空路径"):
        resolve_layer(tree, "")
