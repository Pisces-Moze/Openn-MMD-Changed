import shutil
import struct
import sys
from pathlib import Path


def read_i32(data: bytearray, offset: int) -> tuple[int, int]:
    return struct.unpack_from("<i", data, offset)[0], offset + 4


def skip_text(data: bytearray, offset: int) -> int:
    size, offset = read_i32(data, offset)
    return offset + size


def index_bytes(size: int, count: int) -> int:
    return size * count


source = Path(sys.argv[1]).resolve()
backup = Path(sys.argv[2]).resolve()
data = bytearray(source.read_bytes())

if data[:4] != b"PMX ":
    raise RuntimeError("Not a PMX file")

header_size = data[8]
settings = data[9:9 + header_size]
additional_uv_count = settings[1]
bone_index_size = settings[5]
offset = 9 + header_size

for _ in range(4):
    offset = skip_text(data, offset)

vertex_count, offset = read_i32(data, offset)
changed = 0
for _ in range(vertex_count):
    position_offset = offset
    x, y, z = struct.unpack_from("<3f", data, position_offset)
    uv_offset = position_offset + 24
    u, v = struct.unpack_from("<2f", data, uv_offset)
    offset = uv_offset + 8 + additional_uv_count * 16

    deform_type = data[offset]
    offset += 1
    if deform_type == 0:       # BDEF1
        offset += index_bytes(bone_index_size, 1)
    elif deform_type == 1:     # BDEF2
        offset += index_bytes(bone_index_size, 2) + 4
    elif deform_type in (2, 4):  # BDEF4 / QDEF
        offset += index_bytes(bone_index_size, 4) + 16
    elif deform_type == 3:     # SDEF
        offset += index_bytes(bone_index_size, 2) + 4 + 36
    else:
        raise RuntimeError(f"Unsupported deform type: {deform_type}")
    offset += 4  # edge scale

    # The FBX-to-PMX conversion placed the two outer eyeball UV islands over a
    # dark accessory patch near the top-right of Body.png. Reproject only that
    # unmistakable island onto the composite iris/sclera art. Eyelids, lashes,
    # brows, expression layers and every other mesh keep their original UVs.
    if (
        0.15 < abs(x) < 0.72
        and 11.48 < y < 12.10
        and z < -0.58
        and 0.64 < u < 0.80
        and 0.045 < v < 0.165
    ):
        horizontal = min(1.0, max(0.0, (abs(x) - 0.18) / 0.50))
        if x < 0.0:
            remapped_u = 0.535 - horizontal * 0.090
        else:
            remapped_u = 0.540 + horizontal * 0.090
        vertical = min(1.0, max(0.0, (12.02 - y) / 0.50))
        remapped_v = 0.280 + vertical * 0.105
        struct.pack_into("<2f", data, uv_offset, remapped_u, remapped_v)
        changed += 1

if not 100 <= changed <= 1200:
    raise RuntimeError(f"Unexpected eye vertex count: {changed}")

shutil.copy2(source, backup)
source.write_bytes(data)
print(f"Adjusted {changed} eye-surface vertices; backup={backup}")
