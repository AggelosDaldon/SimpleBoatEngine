#!/usr/bin/env python3
"""Mirror generated 1.20.1 assets into the new registry namespace.
Leave the old assets present for legacy NBT/resource references while all
registered game objects use daldonsboatadditions.
"""
from pathlib import Path
import json, shutil
root=Path("src/main/resources")
old="simpleboatengine"
new="daldonsboatadditions"
for category in ("assets","data"):
    source=root/category/old
    target=root/category/new
    if not source.exists():continue
    for file in source.rglob("*"):
        if not file.is_file():continue
        relative=file.relative_to(source)
        dst=target/relative
        dst.parent.mkdir(parents=True,exist_ok=True)
        if file.suffix in (".json",".mcmeta"):
            content=file.read_text(encoding="utf-8").replace(old,new)
            dst.write_text(content,encoding="utf-8")
        else:
            shutil.copy2(file,dst)
for lang in ("en_us","el_gr"):
    path=root/"assets"/new/"lang"/(lang+".json")
    if not path.exists():continue
    translations=json.loads(path.read_text(encoding="utf-8"))
    defaults={
      "item.daldonsboatadditions.boat_radio":"Boat Radio",
      "item.daldonsboatadditions.yellow_lift_buoy":"Yellow Buoy",
      "item.daldonsboatadditions.white_lift_buoy":"White Buoy",
      "item.daldonsboatadditions.lift_buoy":"Orange Buoy",
      "item.daldonsboatadditions.boat_builder":"Boat Builder",
      "item.daldonsboatadditions.boat_customizer":"Marine Paint Workshop",
    }
    for key,value in defaults.items():
        translations[key]=value
    path.write_text(json.dumps(translations,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print("Generated all modern registry namespace assets; legacy resources kept for migration.")
