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

移植层需要把目标效果逐项重新实现为 GLSL 和 Java 材质状态，不能把 `.shader`
或 `.hlsl` 文件直接复制到 Minecraft 资源目录。

## 第一阶段功能映射

| lilToon 概念 | Minecraft/MMD 移植目标 |
| --- | --- |
| Main Color | PMX 基础贴图与材质颜色，保持原始蓝绿色 |
| Shadow / Shadow Border | 两段式卡通明暗和可调软边 |
| Emission / Fluorescence | 独立发光贴图与遮罩，不受方块光照压暗 |
| Outline | 背面扩张描边，排除眼睛等面部材质 |
| MatCap | 视图空间法线采样的头发/衣服高光 |
| Rim Light | 视线与法线夹角驱动的边缘光 |
| Transparent / Cutout | 分材质透明排序、裁剪阈值和深度写入 |
| Facial layers | 眼白、虹膜、高光分层及正反面/深度控制 |

## 当前实现状态

第一版运行时移植位于：

- `src/main/java/com/shiroha/mmdskin/render/shader/LilToonShaderCpu.java`
- `src/main/resources/assets/mmdskin/shader/liltoon_compat_main.frag.glsl`

启用原有 Toon Rendering 开关时，CPU/OpenGL 与 GPU skinning 后端都会实例化
`LilToonShaderCpu`。初始化失败时仍沿用现有标准渲染回退机制。

当前已经实现卡通阴影边界、环境明暗、程序化 MatCap、边缘光、分材质
unlit、显式启用的青色荧光、独立发光贴图和受伤变红。普通材质会随 Minecraft
环境变暗；只有 `emissionTexture` 和非零 `cyanEmissionStrength` 产生全亮效果。
发光贴图使用加法混合，因此 Unity 常见的不透明黑底不会覆盖基础颜色。

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
| `useShadow` / `shadowBorder` / `shadowBlur` / `shadowColor` | 卡通阴影及软边 | 近似 |
| `useRim` / `rimBorder` / `rimBlur` / `rimFresnelPower` / `rimIntensity` / `rimColor` | 边缘光 | 近似 |
| `useMatCap` / `matCapStrength` | 视图空间程序化高光 | 近似；尚不采样 Unity MatCap 图 |
| `useEmission` / `emissionTexture` / `emissionStrength` | 独立全亮发光图层 | 支持，加法混合 |
| `cyanEmissionStrength` | 从该材质基础贴图中仅提取高饱和青色作为荧光蒙版 | Minecraft 扩展；默认 `0` |
| `normalTexture` / `normalScale` | 法线贴图记录 | 已导入，尚未渲染 |
| `cull` / `renderMode` / `alphaCutoff` | 剔除、透明模式、裁剪 | 部分兼容，复杂透明排序仍有限制 |
| `useOutline` / `outlineWidth` / `outlineColor` | 描边 | 基础支持；面部材质自动排除 |

`cyanEmissionStrength` 推荐范围为 `0.3`～`1.0`。它必须按材质显式开启，不会对
整个模型全局扫描。例如：

```json
"Body": {
  "useEmission": true,
  "emissionTexture": "liltoon_Body_emission.png",
  "emissionStrength": 1.0,
  "cyanEmissionStrength": 0.8
}
```

### 发光层处理规则

1. 优先使用 Unity 材质实际引用的 `_EmissionMap`，导入器会复制为
   `liltoon_<材质名>_emission.png`。
2. 发光图中黑色代表“不增加光”，不要求 Alpha 透明；渲染器使用加法混合。
3. 不要把发光结果烘焙回 Base Color，否则白天基础色会变浅。
4. 只有青色涂层需要发光而 Unity 没有单独蒙版时，对该材质设置
   `cyanEmissionStrength`；不要给头发、衣服等无关材质开启。
5. `emissionStrength` 控制独立发光图强度，`cyanEmissionStrength` 只控制青色
   涂层，两者互不替代。
6. 发光是视觉全亮，不会像火把一样照亮方块和周围实体。

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
