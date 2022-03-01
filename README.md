# KirraTherm

> 作者: @kirraObj (咸蛋)

简单的温泉插件，基于 Kotlin & TabooLib 6 进行开发。

## 指令
```text
  /KirraTherm ...

  thermMode - 切换创造温泉模式。
  create (Name) - 创建一个温泉。
  list - 查看所有已创建的温泉。
```
## 开发接口

### 事件

- PlayerThermGainEvent - 当玩家获得温泉增益时触发事件，无法被取消。

### PlaceholderAPI 变量

```text
  KirraTherm ...

  create_mode - 玩家的创建温泉模式。
  current_therm - 玩家当前所在的温泉名。
  current_therm_is_seat - 玩家所在的温泉是否类型为座椅。
```

## 版本更新

### 1.0.0-SNAPSHOT
完善所有基本功能