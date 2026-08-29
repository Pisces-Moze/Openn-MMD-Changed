import bpy
import json
import os
import sys


def arg_after_double_dash(index=0):
    args = sys.argv[sys.argv.index("--") + 1:]
    return args[index]


model_path = arg_after_double_dash()

try:
    bpy.ops.preferences.addon_enable(module="bl_ext.user_default.mmd_tools")
except Exception as exc:
    print(f"ADDON_ENABLE_WARNING={exc!r}")

bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)
if os.path.splitext(model_path)[1].lower() == ".fbx":
    bpy.ops.wm.fbx_import(filepath=model_path)
else:
    bpy.ops.mmd_tools.import_model(filepath=model_path, scale=1.0)

report = {"objects": [], "materials": [], "armatures": []}
for obj in bpy.data.objects:
    if obj.type == "MESH":
        bounds = [obj.matrix_world @ obj.data.vertices[i].co for i in range(len(obj.data.vertices))]
        report["objects"].append({
            "name": obj.name,
            "vertices": len(obj.data.vertices),
            "materials": [slot.material.name if slot.material else None for slot in obj.material_slots],
            "bounds": {
                "min": [min(v[j] for v in bounds) for j in range(3)] if bounds else None,
                "max": [max(v[j] for v in bounds) for j in range(3)] if bounds else None,
            },
            "vertex_groups": [group.name for group in obj.vertex_groups],
        })
        if os.path.splitext(model_path)[1].lower() != ".fbx":
            parent = list(range(len(obj.data.vertices)))

            def find(value):
                while parent[value] != value:
                    parent[value] = parent[parent[value]]
                    value = parent[value]
                return value

            def union(left, right):
                left, right = find(left), find(right)
                if left != right:
                    parent[right] = left

            for polygon in obj.data.polygons:
                first = polygon.vertices[0]
                for vertex in polygon.vertices[1:]:
                    union(first, vertex)

            components = {}
            for vertex in obj.data.vertices:
                components.setdefault(find(vertex.index), []).append(vertex.index)

            component_report = []
            for indices in components.values():
                points = [obj.matrix_world @ obj.data.vertices[i].co for i in indices]
                if max(point.z for point in points) < 9.5:
                    continue
                material_counts = {}
                index_set = set(indices)
                for polygon in obj.data.polygons:
                    if polygon.vertices[0] in index_set:
                        name = obj.material_slots[polygon.material_index].material.name
                        material_counts[name] = material_counts.get(name, 0) + 1
                group_weights = {}
                for index in indices:
                    for assignment in obj.data.vertices[index].groups:
                        name = obj.vertex_groups[assignment.group].name
                        group_weights[name] = group_weights.get(name, 0.0) + assignment.weight
                component_report.append({
                    "vertices": len(indices),
                    "bounds": {
                        "min": [min(v[j] for v in points) for j in range(3)],
                        "max": [max(v[j] for v in points) for j in range(3)],
                    },
                    "materials": material_counts,
                    "weights": sorted(group_weights.items(), key=lambda item: item[1], reverse=True)[:8],
                })
            report["head_components"] = sorted(component_report, key=lambda item: item["vertices"], reverse=True)
    elif obj.type == "ARMATURE":
        report["armatures"].append({
            "name": obj.name,
            "bones": [bone.name for bone in obj.data.bones],
        })

for material in bpy.data.materials:
    mmd = getattr(material, "mmd_material", None)
    report["materials"].append({
        "name": material.name,
        "diffuse": list(material.diffuse_color),
        "blend_method": getattr(material, "surface_render_method", None),
        "mmd_diffuse": list(mmd.diffuse_color) if mmd else None,
        "mmd_alpha": mmd.alpha if mmd else None,
        "textures": [node.image.filepath for node in material.node_tree.nodes
                     if node.type == "TEX_IMAGE" and node.image] if material.node_tree else [],
    })

print("MMD_INSPECTION=" + json.dumps(report, ensure_ascii=False))
