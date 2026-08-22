import bpy
import os
import sys


args = sys.argv[sys.argv.index("--") + 1:]
source, destination = args[0], args[1]
bpy.ops.preferences.addon_enable(module="bl_ext.user_default.mmd_tools")
bpy.ops.mmd_tools.import_model(filepath=source, scale=1.0)
obj = next(item for item in bpy.data.objects if item.type == "MESH" and item.name == "model_mesh")

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

horn_vertices = set()
for indices in components.values():
    polygons = [polygon for polygon in obj.data.polygons if polygon.vertices[0] in indices]
    materials = {obj.material_slots[p.material_index].material.name for p in polygons}
    points = [obj.matrix_world @ obj.data.vertices[index].co for index in indices]
    if ("clothes" in materials and 350 <= len(indices) <= 400
            and max(point.z for point in points) > 12.5):
        horn_vertices.update(indices)

if not (350 <= len(horn_vertices) <= 400):
    raise RuntimeError(f"Expected the forehead horn mesh, found {len(horn_vertices)} vertices")

deform_groups = [group for group in obj.vertex_groups if group.name not in {"mmd_edge_scale", "mmd_vertex_order"}]
for group in deform_groups:
    group.remove(list(horn_vertices))
obj.vertex_groups["頭"].add(list(horn_vertices), 1.0, "REPLACE")

bpy.context.view_layer.objects.active = obj
obj.select_set(True)
bpy.ops.mmd_tools.export_pmx(
    filepath=destination,
    scale=1.0,
    copy_textures_mode="NONE",
    sort_materials=False,
    disable_specular=False,
    visible_meshes_only=False,
)
print(f"HEAD_FIX vertices={len(horn_vertices)} output={os.path.abspath(destination)}")
