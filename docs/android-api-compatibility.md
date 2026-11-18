# Android API 兼容性检查

应用最低支持 Android 8.0（`minSdk = 26`）。`compileSdk = 37` 提供编译时的 API 定义，当前 AndroidX Core 1.19.1 的 AAR 元数据要求该版本；`targetSdk = 37` 声明应用采用的系统行为。降低 `minSdk` 不会补齐旧系统缺少的接口，降低 `compileSdk` 也不能保证所有调用都兼容 Android 8.0。

## 发布时阻断不兼容 API

`app` 和 `weewa_lib` 启用 `NewApi`、`InlinedApi` 检查，并将它们设为 `fatal`；`checkDependencies`、`checkReleaseBuilds` 和 `abortOnError` 均为 `true`。普通的 Release 构建会运行 Lint Vital，遇到未保护的新系统 API 时失败。应用检查同时包含本项目的依赖库代码。

新增平台 API 时，使用兼容实现，或者在调用处通过 `Build.VERSION.SDK_INT` 判断后提供旧系统分支。只允许新系统调用的成员可以声明 `@RequiresApi`，调用者仍需满足该版本条件。不要用全文件 `SuppressLint("NewApi")` 或 API 错误基线绕过检查。

编译 SDK 35 及以上时，Kotlin `MutableList.removeFirst()` / `removeLast()` 可能解析为 Android 15 的平台方法。兼容列表使用 `removeAt(0)` / `removeAt(lastIndex)`。最终 APK 另有检查脚本，防止该调用重新进入安装包。

```bash
./gradlew :app:lintRewooUserProdRelease :weewa_lib:lintRelease
./gradlew :app:assembleRewooUserProdRelease
python3 tools/tests/check_release_camera_api_compat.py \
  app/build/outputs/apk/rewooUserProd/release/hotme_rewooUserProdRelease_1.2.7.apk
```

静态检查不能替代旧系统上的业务验证。下载、保存相册、播放、权限申请，以及第三方 SDK 的反射和原生代码路径，需要在最低支持版本和实际用户设备上继续测试。

## 2026-10-11 验证

- 应用（含依赖库）及 `weewa_lib` 的完整 API 检查均为零问题。首次检查报告的三个 Android 12 蓝牙权限常量已有版本判断，已补充成员的 `RequiresApi` 声明。
- 在 `weewa_lib` 临时加入未保护的 `MutableList.removeFirst()`：普通 `assembleRewooUserProdRelease` 构建被 `lintVitalRewooUserProdRelease` 阻断，报告要求 API 35，而最低支持版本为 API 26。样例已删除。
- 删除样例后，完整 Lint 和普通用户 1.2.7 Release 构建通过；签名、最终 APK 集合接口兼容检查、下载器类保留检查和套餐模型检查均通过。
- 此次验证未包含故障 vivo 手机的原机测试。

参考：[Android 构建 SDK 配置](https://developer.android.com/build#module-level)、[Lint 配置](https://developer.android.com/studio/write/lint)、[Android 15 集合接口兼容说明](https://developer.android.com/about/versions/15/behavior-changes-15#openjdk-api-changes)。
