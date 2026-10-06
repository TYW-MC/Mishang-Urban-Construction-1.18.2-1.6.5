# 归档：与 Sinytra Connector 环境不兼容的 Mixin

本目录存放**在 Forge + Sinytra Connector 环境下会导致游戏崩溃**的 mixin 源码归档。
文件保留为 `.archived` 后缀，不参与编译，对应条目也已从 `mishanguc.mixins.json` 移除。

---

## `PortalForcerMixin`

**原用途**：让下界传送门的生成逻辑（`PortalForcer#method_22389`）也接受
`mishanguc:nether_portal` 这个自定义兴趣点，以支持「彩色下界传送门」功能。

**崩溃现象**（Forge 1.18.2 + Connector 1.0.0-beta.1）：

```
[Server thread/FATAL] [mixin/]: Mixin apply failed mishanguc.mixins.json:PortalForcerMixin
  -> net.minecraft.world.level.portal.PortalForcer:
  org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException
  @ModifyReturnValue return value modifier method
  net/minecraft/world/level/portal/PortalForcer::acceptColoredPortal
  has an invalid signature.
  Found unexpected argument type net.minecraft.core.Holder at index 1,
  expected net.minecraft.world.entity.ai.village.poi.PoiType.
  Handler signature: (ZLnet/minecraft/core/Holder;)Z
  Expected signature: (ZLnet/minecraft/world/entity/ai/village/poi/PoiType;)Z
```

随后 `MixinApplyError` → `MixinTransformerError` → 服务器 tick 循环异常 → **整个存档无法进入**。

### 根因分析

| 侧 | 成员名 | 参数类型 |
| --- | --- | --- |
| Yarn 1.18.2（编译期） | `method_22389` | `PointOfInterestType`（= 中介名 `class_4158`） |
| 本模组 refmap 产出 | `class_1946;method_22389(Lnet/minecraft/class_4158;)Z` | `class_4158` |
| **Connector 运行时** | 同方法 | **`net.minecraft.core.Holder`** |

Connector 把这个兴趣点查询从「直接持有 `PoiType`」改成了 **`Holder<PoiType>`**，
因此 Mixin 在做 `@ModifyReturnValue` 的**参数签名校验**（`Injector.validateParams`）时，
拿到的目标描述符与 refmap 声明不符，**在校验阶段就直接抛异常**——此时
`require = 0` 尚未生效（它在校验之后才被检查），所以无法用软失败兜住。

> 通用结论：当目标方法参数的**类型本身**在 Connector 下被替换（`PoiType` → `Holder`）时，
> 这不是「注解参数写错」，而是**机制形状变了**，属于应当归档的类别，
> 而不是通过修改 handler 签名去凑（改成 `RegistryEntry<?>`、`Object` 等同样无效，
> 因为校验比对的是声明描述符与目标描述符）。

### 处置

- 移入本归档目录，从 `mishanguc.mixins.json` 删除 `"PortalForcerMixin"` 条目。
- 同时把 `mishanguc.mixins.json` 的 `injectors.defaultRequire` 由 `1` 调整为 `0`，
  使其余 mixin 在遇到无法解析的目标时**降级为无操作**，而不是让整个游戏崩溃。

### 功能影响

仅影响「彩色下界传送门」在**传送门寻路/生成**阶段被识别为有效传送门这一处。
其余相关部分仍然生效：

- `AreaHelperMixin` —— 传送门框架方块判定（`validStateInsidePortal` / `method_30487`）
- `NetherPortalBlockMixin` —— 相邻方块更新时的有效方块判定
- `Mishanguc.registerColoredBlocks` 中的 `colored_nether_portal` 兴趣点注册
- `ColoredBlocks.COLORED_NETHER_PORTAL` 方块本身与其标签

即：**彩色传送门仍然可以正常搭建与点燃**，只是已存在的原生下界传送门在
「寻找/创建返回传送门」的匹配环节，不会把彩色传送门纳入候补。

### 若日后要恢复

建议方向：改为注入 `Holder` 形状的目标（`javap` 确认 Connector 侧真实描述符），
或在 `connector` 专属配置中单独启用一个 Forge 版 mixin，而不要复用 Fabric 版签名。
恢复前务必在 Forge + Connector 环境下实测启动到「进入世界」而非仅编译通过。
