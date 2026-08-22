#!/usr/bin/env python3
"""Export named PSD layers/groups as one UV-aligned transparent emission PNG.

Requires Pillow and psd-tools. The source PSD is opened read-only. Layer paths are
top-to-bottom names separated by '/'; repeat --layer to merge several selections.
"""

from __future__ import annotations

import argparse
from pathlib import Path
import unicodedata

from PIL import Image
from psd_tools import PSDImage


def normalized_name(value: str) -> str:
    """Allow documentation to omit the space in names such as '图层 5'."""
    return "".join(unicodedata.normalize("NFKC", value).split()).casefold()


def resolve_layer(root, path: str):
    current = root
    resolved = []
    for component in (part for part in path.split("/") if part):
        matches = [
            child for child in current
            if normalized_name(child.name) == normalized_name(component)
        ]
        if not matches:
            available = ", ".join(repr(child.name) for child in current)
            raise ValueError(
                f"找不到图层路径 {path!r} 的 {component!r}；当前层包含：{available}"
            )
        if len(matches) > 1:
            raise ValueError(f"图层路径 {path!r} 的 {component!r} 不唯一，请整理 PSD 图层名")
        current = matches[0]
        resolved.append(current.name)
    if not resolved:
        raise ValueError("--layer 不能是空路径")
    return current, "/".join(resolved)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--psd", required=True, type=Path, help="只读源 PSD")
    parser.add_argument("--output", required=True, type=Path, help="输出 RGBA PNG")
    parser.add_argument(
        "--layer", action="append", required=True,
        help="要导出的图层/图层组路径；可重复，例如 サメトラ/目/瞳",
    )
    parser.add_argument("--force", action="store_true", help="允许覆盖已有输出")
    args = parser.parse_args()

    if args.output.exists() and not args.force:
        raise FileExistsError(f"输出已存在：{args.output}（确需覆盖时添加 --force）")

    psd = PSDImage.open(args.psd)
    selected = []
    for requested_path in args.layer:
        node, actual_path = resolve_layer(psd, requested_path)
        selected.append((node, actual_path))

    # The PSD collection order is front-to-back. Draw selections back-to-front.
    canvas = Image.new("RGBA", psd.size, (0, 0, 0, 0))
    for node, _ in reversed(selected):
        rendered = node.composite()
        if rendered is None:
            continue
        positioned = Image.new("RGBA", psd.size, (0, 0, 0, 0))
        positioned.alpha_composite(rendered.convert("RGBA"), (node.left, node.top))
        canvas = Image.alpha_composite(canvas, positioned)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(args.output, format="PNG", optimize=True)
    print(f"源文件：{args.psd}")
    for _, actual_path in selected:
        print(f"包含：{actual_path}")
    print(f"输出：{args.output} ({canvas.width}x{canvas.height}, RGBA)")


if __name__ == "__main__":
    main()
