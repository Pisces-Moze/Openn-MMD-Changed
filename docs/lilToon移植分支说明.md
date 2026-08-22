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

当前已经实现卡通阴影边界、稳定基础色、程序化 MatCap、边缘光、分材质
unlit、青色荧光识别和受伤变红。MatCap 强度按 PMX 材质名/贴图名自动分组：
头发较强、衣服中等、身体较弱、面部最弱。独立 `_emi.png` 发光贴图仍由
现有 fullbright pass 负责。

尚未实现的 Unity 专属功能包括多层 Main2nd/Main3rd、法线贴图、各向异性、
AudioLink、距离淡出、宝石/毛发专用 pass 和 Unity 光照探针。它们需要按
Minecraft 的资源及绘制生命周期逐项移植。

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
