# Bundled MMD assets

完整的模型转换、实体、Variant、刷怪蛋、针剂、转化箭、盔甲与创造栏教程见：
[`docs/附属模组开发与模型接入指南.md`](docs/附属模组开发与模型接入指南.md)。

The first hard-coded form is `openmmdchanged:mmd_latex`. Its runtime model
folder is always `openmmdchanged.mmd_latex`; players cannot select or replace
that mapping through an in-game selector.

Place the future PMX/PMD model, textures, and VMD files under:

```text
src/main/resources/assets/openmmdchanged/mmd/mmd_latex/
├── model.pmx                 # or model.pmd
├── textures...
├── model.properties         # optional MMDSkin scale/settings
├── animations.json          # optional explicit animation mapping
└── anims/
    ├── idle.vmd
    ├── walk.vmd
    ├── sprint.vmd
    └── ...
```

Every bundled file must also be listed, one relative path per line, in
`assets.list` in the same directory. At client startup the listed files are
copied to the private MMDSkin-compatible cache below:

```text
.minecraft/3d-skin/EntityPlayer/openmmdchanged.mmd_latex/
```

The cache is overwritten from the mod on every launch, so it is not a model
selection or user customization location. The JAR remains the source of truth.

To add another form, create a new Changed entity and transfur variant, assign a
unique fixed model folder, add a matching resource manifest, and register a
renderer using the same Changed/MMD bridge.
