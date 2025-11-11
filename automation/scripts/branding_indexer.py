#!/usr/bin/env python3
"""Generate a JSON index for LuckySky branding assets."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple

PNG_IHDR = b"IHDR"
CATEGORY_PARTS = ("banner", "logo", "icons", "styling", "docs")
DESCRIPTION_OVERRIDES: Dict[str, str] = {
    "Banner-001.png": "Primary LuckySky promotional banner artwork in a wide format.",
    "LuckySky-Catch.mp4": "Gameplay capture clip showcasing the LuckySky experience.",
    "readme.md": "Placeholder README for branding documentation assets.",
    "Command-Block.png": "Command block themed LuckySky icon at 128x128 resolution.",
    "Icon-Herz.png": "Heart-shaped LuckySky icon rendered at 128x128 pixels.",
    "Icon-Rad.png": "Gear wheel LuckySky icon sized for 128x128 usage.",
    "Icon-Tool-Click-Magic.png": "Magic click tool LuckySky icon optimized for 128x128 displays.",
    "Icon-off.png": "Power toggle LuckySky icon at 128x128 resolution.",
    "LuckySky-Logo2.png": "Square LuckySky emblem and logotype combination logo.",
    "LuckySky-Logo4.png": "Wide rectangular LuckySky logotype variant.",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--asset-root",
        type=Path,
        default=Path("docs/images/luckysky/branding"),
        help="Root directory that contains the branding assets.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=None,
        help="Destination JSON file. Defaults to <asset_root>/branding_index.json.",
    )
    parser.add_argument(
        "--indent",
        type=int,
        default=2,
        help="JSON indentation level (default: 2).",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print the manifest to stdout instead of writing to a file.",
    )
    return parser.parse_args()


def png_dimensions(path: Path) -> tuple[Optional[int], Optional[int]]:
    """Read PNG dimensions without requiring external libraries."""
    with path.open("rb") as stream:
        header = stream.read(24)
    if len(header) < 24 or header[12:16] != PNG_IHDR:
        return None, None
    width = int.from_bytes(header[16:20], "big")
    height = int.from_bytes(header[20:24], "big")
    return width, height


def detect_category(parts: Iterable[str]) -> Optional[str]:
    for candidate in CATEGORY_PARTS:
        if candidate in parts:
            return candidate
    return None


def detect_type(path: Path, rel_parts: tuple[str, ...]) -> str:
    if path.name == ".keep":
        return "placeholder"
    suffix = path.suffix.lower()
    if suffix == ".png":
        if "banner" in rel_parts:
            return "banner"
        if "logo" in rel_parts:
            return "logo"
        if "icons" in rel_parts:
            return "icon"
        return "image"
    if suffix == ".mp4":
        return "video"
    if suffix == ".md":
        return "doc"
    return "file"


def asset_metadata(asset_root: Path, display_root: Path) -> list[dict[str, object]]:
    records: list[dict[str, object]] = []
    for candidate_path in sorted(asset_root.rglob("*")):
        if candidate_path.is_dir() or candidate_path == asset_root / "branding_index.json":
            continue
        rel = candidate_path.relative_to(asset_root)
        rel_parts = rel.parts
        asset_type = detect_type(candidate_path, rel_parts)
        width = height = None
        if candidate_path.suffix.lower() == ".png":
            width, height = png_dimensions(candidate_path)
        records.append(
            {
                "path": (display_root / rel).as_posix(),
                "relative_path": rel.as_posix(),
                "category": detect_category(rel_parts),
                "type": asset_type,
                "bytes": candidate_path.stat().st_size,
                "dimensions": {"width": width, "height": height},
                "description": DESCRIPTION_OVERRIDES.get(candidate_path.name),
            }
        )
    return records


def main() -> None:
    args = parse_args()
    asset_root = args.asset_root.resolve()
    if not asset_root.exists():
        raise SystemExit(f"Asset root not found: {asset_root}")

    try:
        relative_root = asset_root.relative_to(Path.cwd())
        root_repr = relative_root.as_posix()
    except ValueError:
        root_repr = str(asset_root)

    display_root = Path(root_repr)
    manifest = {
        "asset_root": root_repr,
        "asset_count": 0,
        "generated_by": "automation/scripts/branding_indexer.py",
        "assets": asset_metadata(asset_root, display_root),
    }
    manifest["asset_count"] = len(manifest["assets"])

    output_path = args.output or (asset_root / "branding_index.json")
    payload = json.dumps(manifest, indent=args.indent, ensure_ascii=False) + "\n"
    if args.dry_run:
        print(payload, end="")
    else:
        output_path.write_text(payload)


def _entry() -> None:
    main()


if __name__ == "__main__":
    _entry()
