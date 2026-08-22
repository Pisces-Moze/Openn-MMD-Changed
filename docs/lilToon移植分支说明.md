# lilToon 移植分支说明

## 分支与基线

- 稳定基线：`main`
- 实验分支：`feature/liltoon-renderer`
- lilToon 源码：`third_party/lilToon` Git 子模块
- 当前固定提交：由 `.gitmodules` 和子模块提交记录共同确定

切换回 `main` 不会包含 lilToon 子模块和本分支后续的渲染实验。

首次取得本仓库后，使用下面的命令取得第三方源码：

```shell
git submodule update --init --recursive
```

## 为什么不能直接加载 lilToon Shader

lilToon 面向 Unity 的 ShaderLab/HLSL、Unity 材质属性和 Unity 渲染管线。
本项目运行于 Minecraft Forge、Mojang RenderSystem 和 OpenGL/GLSL，运行时没有
Unity 的 ShaderLab 编译器、宏、灯光结构、摄像机变量与材质序列化系统。因此，
`third_party/lilToon` 是实现行为对照和公式来源，不会加入 Java/Forge 编译路径。

移植层需要把目标效果逐项重新实现为 GLSL 和 Java 材质状态。

## 第一阶段功能映射

| lilToon 概念 | Minecraft/MMD 移植目标 |
| --- | --- |
| Main Color | PMX 基础贴图与材质颜色 |
| Shadow / Shadow Border | 两段式卡通明暗和可调软边 |
| Emission / Fluorescence | 独立发光贴图与遮罩，不受方块光照压暗 |
| Outline | 背面扩张描边，排除眼睛等面部材质 |
| MatCap | 视图空间法线采样的头发/衣服高光 |
| Rim Light | 视线与法线夹角驱动的边缘光 |
| Transparent / Cutout | 分材质透明排序、裁剪阈值和深度写入 |
| Facial layers | 眼白、虹膜、高光分层及正反面/深度控制 |

## 当前实现状态

运行时包含两条路径：

- `src/main/java/com/shiroha/mmdskin/render/shader/LilToonShaderCpu.java`
- `src/main/java/com/shiroha/mmdskin/render/backend/opengl/MinecraftBufferedModelRenderer.java`
- `src/main/java/com/shiroha/mmdskin/render/backend/opengl/PmxRenderTypes.java`

世界、实体、玩家和背包模型优先把 CPU 蒙皮后的 PMX 三角形交给 Minecraft
`VertexConsumer/RenderType`。基础层沿用当前 Minecraft/Iris/Oculus 实体着色器，
最低亮度层和 Emission 层分别提交。这样不会把原始 `glDrawElements` 注入未知的
光影程序，也不会污染其他实体、手持物品或地面的矩阵和阴影状态。旧式 GPU 直绘
暂时禁用，直到它也能输出到相同的 Minecraft 顶点管线。

兼容路径已实现光影包可识别的基础光照与投影、分材质最低亮度、`_AsUnlit`、
Cutout/Transparent、双面材质、独立发光贴图和受伤变红。普通材质仍接收环境光、
物体遮挡与光影包阴影；发光贴图使用独立全亮加法层，黑底不会覆盖基础颜色。
卡通阴影、Rim、MatCap 和 Outline 的精确 lilToon 公式仍保留在无光影实验程序中，
不同光影包下优先保证正确几何、基础色、阴影和发光，而不是强行替换光影包程序。

尚未实现的 Unity 专属功能包括多层 Main2nd/Main3rd、法线贴图、各向异性、
AudioLink、距离淡出、宝石/毛发专用 pass 和 Unity 光照探针。它们需要按
Minecraft 的资源及绘制生命周期逐项移植。

## Unity prefab 通用导入器

`tools/import_unity_liltoon.py` 会以只读方式扫描 Unity 的 `Assets` 目录，建立
`.meta` GUID 索引，递归解析 prefab 引用，并读取 lilToon `.mat` 参数。它不会
修改 Unity 工程，只会在指定的 MMD 模型目录生成：

- `liltoon_materials.json`
- 材质实际启用的 emission 贴图副本
- 材质实际启用的 normal 贴图副本

通用用法：

```powershell
python tools/import_unity_liltoon.py `
  --unity-assets "D:\UnityProject\Assets" `
  --prefab "D:\UnityProject\Assets\Avatar\Avatar.prefab" `
  --output "D:\MmdModelDirectory"
```

如果一个 prefab 包含衣服、道具和多个换色版本，可以限定当前材质目录：

```powershell
python tools/import_unity_liltoon.py `
  --unity-assets "D:\UnityProject\Assets" `
  --prefab "D:\UnityProject\Assets\Avatar\Avatar.prefab" `
  --material-dir "D:\UnityProject\Assets\Avatar\Materials\Blue" `
  --material-dir-only `
  --output "D:\MmdModelDirectory"
```

已有输出默认不会被覆盖；确认更新时显式添加 `--force`。如果 Unity 材质名与
PMX 材质名不同，在生成的 JSON 对应材质的 `aliases` 数组中填写 PMX 名称。

运行时会按“PMX 材质名 -> aliases -> 主贴图文件名”依次匹配配置。没有配置文件
的旧模型继续使用兼容默认值，不需要重新导入。

当前运行时已使用每材质的 Shadow、Rim、MatCap 开关和精确 emission 贴图。
Normal 贴图已经导入和记录，但必须等渲染器增加 tangent/bitangent 通道后才能
安全启用。

### 导入命令与文件安全

导入器只读取 `--unity-assets` 和 `--prefab`，不会改写 Unity 工程。输出目录必须
放在本分支的模型资源目录或独立工作目录，不要指向 Unity 的 `Assets`：

```powershell
python tools/import_unity_liltoon.py `
  --unity-assets "F:\UnityProject\Assets" `
  --prefab "F:\UnityProject\Assets\Avatar\Prefab\Avatar.prefab" `
  --material-dir "F:\UnityProject\Assets\Avatar\Materials\Blue" `
  --material-dir-only `
  --output "src\main\resources\assets\openmmdchanged\mmd\avatar_id" `
  --force
```

导入完成后，把新增 PNG 和 `liltoon_materials.json` 全部加入同目录的
`assets.list`。启动游戏后检查日志中的 `Loaded ... lilToon material profiles`；
没有这条日志通常表示文件未安装、JSON 格式错误或模型文件夹名不一致。

### `liltoon_materials.json` 参数

| 参数 | 作用 | 当前兼容情况 |
| --- | --- | --- |
| `aliases` | PMX 材质名与 Unity 材质名不同时的别名 | 完整 |
| `baseLightFloor` | 光影计算后仍保留的基础贴图最低比例，范围 `0`～`1` | 完整，Minecraft 扩展 |
| `unlitStrength` | Unity lilToon `_AsUnlit`，导入器自动读取 | 完整，叠加为最低亮度 |
| `useShadow` / `shadowBorder` / `shadowBlur` / `shadowColor` | 卡通阴影及软边 | 光影包兼容路径由光影包决定 |
| `useRim` / `rimBorder` / `rimBlur` / `rimFresnelPower` / `rimIntensity` / `rimColor` | 边缘光 | 近似 |
| `useMatCap` / `matCapStrength` | 视图空间程序化高光 | 近似；尚不采样 Unity MatCap 图 |
| `useEmission` / `emissionTexture` / `emissionStrength` | 独立全亮发光图层 | 支持，加法混合 |
| `emissionLayers` | 一个材质叠加多个明确贴图或颜色筛选发光层 | 完整，推荐新模型使用 |
| `cyanEmissionStrength` | 旧实验程序的按颜色提取 | 仅兼容旧模型；新模型应制作明确的 Emission PNG |
| `normalTexture` / `normalScale` | 法线贴图记录 | 已导入，尚未渲染 |
| `cull` / `renderMode` / `alphaCutoff` | 剔除、透明模式、裁剪 | 部分兼容，复杂透明排序仍有限制 |
| `useOutline` / `outlineWidth` / `outlineColor` | 描边 | 基础支持；面部材质自动排除 |

通用模型不要依赖“青色等于发光”的颜色猜测。不同角色的基础色、色彩空间和压缩
方式不同，容易把皮肤或衣服全部判成发光。每个需要发光的材质必须提供同 UV、同
尺寸的独立 RGBA PNG，规范名为 `<PMX材质名>_emission.png`，例如：

运行时采用显式启用原则：`ModelMaterial` 默认 `useEmission=false`，不再扫描或自动
加载 `<主贴图名>_emi.png`。一个模型只有在自己的 `liltoon_materials.json` 中明确
列出发光材质和发光贴图时才会发光，避免模型之间互相依赖命名猜测。

```json
"Body": {
  "useEmission": true,
  "emissionTexture": "Body_emission.png",
  "emissionStrength": 1.0,
  "baseLightFloor": 0.35,
  "unlitStrength": 0.0
}
```

同一个材质需要让不同语义层使用不同强度时，应先分别从 PSD 导出，再叠加：

```json
"emissionLayers": [
  {
    "texture": "Body_emission_eyes.png",
    "maskMode": "texture",
    "strength": 0.7
  },
  {
    "texture": "Body_emission_markings.png",
    "maskMode": "texture",
    "strength": 0.85,
    "color": [1.0, 1.0, 1.0, 1.0]
  }
]
```

- `texture`：相对模型目录的 PNG；`$base` 表示当前 PMX 材质的主贴图。
- `maskMode: texture`：完全相信独立蒙版；黑色/透明区域不发光。
- `maskMode: cyan`：只保留明亮、高饱和且接近青色的像素。
- `maskMode: color`：使用 `maskColor: [r,g,b]` 筛选任意目标颜色。
- `maskTolerance`、`minBrightness`、`maxBrightness`、`minSaturation` 控制筛选范围。
- `uvRects`：可选的归一化 UV 矩形数组，每项为 `[minU,minV,maxU,maxV]`；用于同色
  区域只有一部分应发光的模型，不能用来替代正确的独立蒙版。
- `preserveSourceColor: false`：把筛选结果转为白色蒙版，最终发光色由该层 `color`
  决定；默认 `true`，保留源贴图颜色。

颜色筛选仅适合迁移阶段和色块清晰的贴图；正式发布必须优先使用独立蒙版，这与 Changed
原版 `EmissiveBodyLayer + RenderType.eyes` 的资源组织方式一致。

`baseLightFloor` 推荐从 `0` 开始：深色风格化身体可用 `0.05`～`0.25`，只在确有
需要时继续提高；设为 `1` 会接近无光照材质并削弱阴影。它通过提高基础层最低
lightmap 亮度实现，不再重复绘制全亮基础贴图，因此不会形成全身发光附加层。
独立 Emission 使用 fullbright 附加层，在 Iris/Oculus 光影下仍
保持可见，但不会作为真实点光源照亮方块。基础层使用真正的 PMX 三角形
`RenderType`，因此由当前光影包决定接收和投射阴影的具体效果。

### 发光层处理规则

1. 优先使用 Unity 材质实际引用的 `_EmissionMap`；没有现成 Emission Map 时，从
   PSD 中明确列出的图层/图层组生成，不能从 Base Color 猜颜色。
2. 正式文件统一命名为 `<材质>_emission.png`；拆分强度时使用
   `<材质>_emission_<用途>.png`。全部加入 `assets.list`。
3. 发光 PNG 与 Base Color 必须同尺寸、同 UV、同原点，不能裁边或翻转。推荐
   RGBA PNG；非发光像素为 `RGBA(0,0,0,0)`，发光像素保留源颜色与 Alpha。
4. PSD 推荐使用 `EMISSION/EMI_<序号>_<用途>` 命名。第三方 PSD 不能改名时，在
   模型接入文档中记录完整路径，并用 `tools/extract_psd_emission_layers.py` 只读导出。
5. 不要把发光结果烘焙回 Base Color，否则白天基础色会变浅；也不要把整张主贴图
   设为发光，否则暗处会全身发亮。
6. `maskMode: texture` 是新资产默认值。旧的 `cyanEmissionStrength`、`cyan`、
   `color` 和 `$base` 筛选只作无源文件时的临时兼容。
7. `emissionStrength` 控制独立发光图强度。发光是视觉全亮，不会像火把一样照亮
   方块和周围实体。
8. 必须显式写出 `useEmission: true`、`emissionTexture` 和至少一个
   `emissionLayers` 项。前者兼容原生/GPU 路径，后者用于 Minecraft 缓冲渲染路径；
   单张合成发光图时两处填写同一文件，保持后端结果一致。

### 可维护的模型清单

发光图层路径属于模型资产数据，不属于通用渲染器代码。每个模型的接入记录至少应
维护：PSD 相对路径、变体顶层组、完整发光层路径、输出 PNG、对应 PMX 材质名和默认
强度。PSD 能整理时统一使用 `EMISSION/EMI_<序号>_<用途>`；第三方 PSD 不能修改时
保留原名并记录完整路径。更换模型或换色时只替换该模型的 PNG/JSON，不新增角色名
判断、材质序号判断或固定 UV 坐标到 Java/Rust 渲染器。

## 模型物理的通用兼容

lilToon 只描述材质，不包含 VRChat PhysBone 的运行数据。导入 Prefab 时应同时检查
可动骨链；正式资产优先在 PMX 中制作刚体与 Joint，让内置 Bullet 按标准 MMD 规则
计算。没有任何 PMX 刚体时，运行时会按常见的头发、耳朵、尾巴、衣摆和装饰骨骼名
建立保守的弹簧链回退。它适用于大多数规范命名模型，但不会复刻 PhysBone 的抓取、
碰撞体、拉伸、限制曲线等全部功能。具体命名、制作和测试方法见通用开发手册的
“耳朵、头发、尾巴和衣摆的通用物理”，不要为单一角色硬编码材质名或骨骼序号。

当前不会复刻 AudioLink、Main2nd/Main3rd、Glitter、Dissolve、各向异性、
Unity 光照探针和完整透明队列。导入器保留通用材质结构，但这些效果需要另行
烘焙到贴图或继续扩展 GLSL。

## 安全原则

1. 新渲染器先以可关闭的实验后端接入，默认不替换稳定后端。
2. 每个着色器只写入 Minecraft 当前实际绑定的颜色附件。
3. 所有绘制阶段恢复深度、混合、剔除、活动纹理和着色器状态。
4. 眼睛、透明头发和发光层分别处理，不用统一的深度偏移解决遮挡。
5. 每一阶段都同时验证世界、第一人称、生存背包和受伤变红效果。

## 许可

lilToon 使用 MIT License。原始版权与许可证位于：

- `third_party/lilToon/LICENSE`
- `src/main/resources/META-INF/LICENSE_lilToon.txt`

移植或改写自 lilToon 的实质性代码必须继续保留该声明。
