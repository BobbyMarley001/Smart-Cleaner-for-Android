import zipfile, os, json, re
from pathlib import Path
import pandas as pd
import xml.etree.ElementTree as ET

# === SETTINGS ===
project_dir = Path(".")  # Adjust if needed
report_md = Path("smart_cleaner_project_analysis.md")
report_json = Path("smart_cleaner_summary.json")

def safe_read_text(path, max_chars=50000):
    try:
        txt = path.read_text(encoding="utf-8", errors="ignore")
        if len(txt) > max_chars:
            return txt[:max_chars] + "\n...[truncated]"
        return txt
    except Exception as e:
        return f"[Error: {e}]"

# === Inventory ===
rows = []
for root, dirs, files in os.walk(project_dir):
    for f in files:
        p = Path(root) / f
        rel = p.relative_to(project_dir)
        rows.append({"path": str(rel), "ext": p.suffix, "size_bytes": p.stat().st_size})
df = pd.DataFrame(rows)
print(f"Total files: {len(df)}")
print(df.groupby("ext").size())

# === Manifest parsing ===
manifest_info = []
for manifest in project_dir.rglob("AndroidManifest.xml"):
    txt = safe_read_text(manifest, 20000)
    try:
        root = ET.fromstring(txt)
        pkg = root.attrib.get("package", "(in Gradle)")
        ns = {'android': 'http://schemas.android.com/apk/res/android'}
        app = root.find("application")
        label = app.attrib.get("{http://schemas.android.com/apk/res/android}label") if app is not None else None
        permissions = [perm.attrib.get("{http://schemas.android.com/apk/res/android}name") for perm in root.findall("uses-permission")]
        manifest_info.append({"file": str(manifest), "package": pkg, "label": label, "permissions": permissions})
    except Exception as e:
        manifest_info.append({"file": str(manifest), "error": str(e)})

# === Kotlin/Java overview ===
notable = []
for ext in [".kt", ".java"]:
    for file in project_dir.rglob(f"*{ext}"):
        txt = safe_read_text(file, 20000)
        classes = re.findall(r'^\s*(?:class|object|data\s+class|interface)\s+([A-Za-z0-9_]+)', txt, re.M)
        funs = re.findall(r'^\s*fun\s+([A-Za-z0-9_]+)\s*\(', txt, re.M)
        notable.append({
            "path": str(file.relative_to(project_dir)),
            "classes": classes[:5],
            "functions": funs[:5]
        })

# === Save outputs ===
with open(report_md, "w") as f:
    f.write("# Project Analysis\n")
    for m in manifest_info:
        f.write(f"## {m['file']}\n")
        for k,v in m.items():
            if k != "file":
                f.write(f"- {k}: {v}\n")

with open(report_json, "w") as f:
    json.dump(notable, f, indent=2)

print("Analysis complete!")
