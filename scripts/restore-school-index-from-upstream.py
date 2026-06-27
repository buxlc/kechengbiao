import argparse
import json
from pathlib import Path

import yaml


TEST_ADAPTER_IDS = {"GENERAL_TOOL_01", "GENERAL_TOOL_02"}
TEST_NAME_MARKERS = ("组件测试", "适配代码测试")


def is_test_adapter(adapter: dict) -> bool:
    adapter_id = str(adapter.get("adapter_id", ""))
    adapter_name = str(adapter.get("adapter_name", ""))
    return adapter_id in TEST_ADAPTER_IDS or any(marker in adapter_name for marker in TEST_NAME_MARKERS)


def normalize_adapter(adapter: dict) -> dict:
    return {
        "adapter_id": str(adapter.get("adapter_id", "")),
        "adapter_name": str(adapter.get("adapter_name", "")),
        "category": str(adapter.get("category", "")),
        "js_path": str(adapter.get("asset_js_path", "")),
        "import_url": str(adapter.get("import_url", "")),
        "description": str(adapter.get("description", "")),
        "maintainer": str(adapter.get("maintainer", "")),
    }


def load_yaml(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as fh:
        return yaml.safe_load(fh) or {}


def build_index(upstream_root: Path) -> dict:
    root_index = load_yaml(upstream_root / "index" / "root_index.yaml")
    resources_root = upstream_root / "resources"
    schools = []

    for school in root_index.get("schools", []):
        folder = str(school.get("resource_folder", school.get("id", "")))
        adapters_yaml = resources_root / folder / "adapters.yaml"
        if not adapters_yaml.exists():
            continue

        adapters_doc = load_yaml(adapters_yaml)
        adapters = [
            normalize_adapter(adapter)
            for adapter in adapters_doc.get("adapters", [])
            if not is_test_adapter(adapter)
        ]
        if not adapters:
            continue

        schools.append(
            {
                "id": str(school.get("id", "")),
                "name": str(school.get("name", "")),
                "initial": str(school.get("initial", "")),
                "folder": folder,
                "adapters": adapters,
            }
        )

    return {"schools": schools}


def main() -> None:
    parser = argparse.ArgumentParser(description="Restore school_index.json from shiguang_warehouse.")
    parser.add_argument("--upstream", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    index = build_index(args.upstream)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(index, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print(f"Restored {len(index['schools'])} school presets to {args.output}")


if __name__ == "__main__":
    main()
