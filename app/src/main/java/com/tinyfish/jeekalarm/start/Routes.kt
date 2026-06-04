package com.tinyfish.jeekalarm.start

import kotlinx.serialization.Serializable

// 类型安全的导航路由：Home/Settings/Edit/RecycleBin。
// 通知屏不在这里——它是由响铃状态驱动的浮层，不进返回栈。
@Serializable
object HomeRoute

@Serializable
object SettingsRoute

@Serializable
object EditRoute

@Serializable
object RecycleBinRoute
