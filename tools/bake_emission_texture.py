from PIL import Image, ImageChops
import sys


base_path, emission_path, output_path = sys.argv[1:4]
base = Image.open(base_path).convert("RGBA")
emission = Image.open(emission_path).convert("RGBA")
if base.size != emission.size:
    raise RuntimeError(f"Texture sizes differ: {base.size} != {emission.size}")

# Unity's shader adds this map after normal lighting. Baking with screen blend keeps
# the original fur colour while restoring eye/highlight information in plain PMX.
rgb = ImageChops.screen(base.convert("RGB"), emission.convert("RGB"))
rgb.putalpha(base.getchannel("A"))
rgb.save(output_path, optimize=True)
