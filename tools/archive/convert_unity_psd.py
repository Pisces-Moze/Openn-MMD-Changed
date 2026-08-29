import os
import sys
import bpy


source, output = sys.argv[sys.argv.index("--") + 1:sys.argv.index("--") + 3]
image = bpy.data.images.load(source, check_existing=False)
image.filepath_raw = output
image.file_format = "PNG"
image.save()
print("CONVERTED", source, output, image.size[:])
