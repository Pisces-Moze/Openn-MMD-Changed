import bpy
import json
import sys


path = sys.argv[sys.argv.index("--") + 1]
bpy.ops.preferences.addon_enable(module="bl_ext.user_default.mmd_tools")
bpy.ops.mmd_tools.import_model(filepath=path, scale=1.0)
obj = next(item for item in bpy.data.objects if item.type == "MESH")

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
    for vertex in polygon.vertices[1:]:
        union(polygon.vertices[0], vertex)

components = {}
for vertex in obj.data.vertices:
    components.setdefault(find(vertex.index), set()).add(vertex.index)

uv_layer = obj.data.uv_layers.active.data
rows = []
for indices in components.values():
    points = [obj.matrix_world @ obj.data.vertices[i].co for i in indices]
    min_z, max_z = min(v.z for v in points), max(v.z for v in points)
    if max_z < 11.0:
        continue
    polygons = [p for p in obj.data.polygons if p.vertices[0] in indices]
    materials = {obj.material_slots[p.material_index].material.name for p in polygons}
    if not ({"Body_FirstPersonHead", "clothes"} & materials):
        continue
    uvs = [uv_layer[loop].uv.copy() for p in polygons for loop in p.loop_indices]
    weights = {}
    for index in indices:
        for assignment in obj.data.vertices[index].groups:
            name = obj.vertex_groups[assignment.group].name
            weights[name] = weights.get(name, 0.0) + assignment.weight
    rows.append({
        "vertices": len(indices),
        "bounds": [[min(v[j] for v in points), max(v[j] for v in points)] for j in range(3)],
        "materials": sorted(materials),
        "uv": [[min(v[j] for v in uvs), max(v[j] for v in uvs)] for j in range(2)],
        "weights": sorted(weights.items(), key=lambda item: item[1], reverse=True)[:5],
    })

print("HEAD_ANALYSIS=" + json.dumps(sorted(rows, key=lambda row: row["vertices"], reverse=True), ensure_ascii=False))
