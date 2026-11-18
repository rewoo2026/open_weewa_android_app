# 热我 Android

热我相机的 Android 客户端源码，包含相机连接、录像、媒体管理和上传。

## 源码授权

本项目自有代码的权利由其权利人保留。公开查看源码不授予开源许可。客户修改、编译和内部使用的权限，以与权利人签订的书面源码授权协议为准。

第三方代码和组件继续适用各自的许可证，例如 [FileDownloader 的许可证](filedownloader/LICENSE)。

## 本地构建

使用 JDK 25 和 Android SDK 37。将 `ANDROID_HOME` 指向 SDK 目录，补齐从供应商取得授权的 SDK 后，运行：

```sh
./gradlew :app:assembleRewooUserDevDebug
```

项目仍从 `app/libs/` 读取银联、现在支付和 FFmpeg 扩展的本地依赖。原生库按 ABI 放在 `app/src/main/jniLibs/` 和 `library/src/main/jniLibs/`。这些目录由 Git 忽略。

mPaaS Maven 仓库可用 `WEEWA_MPAAS_REPOSITORY_URL`、`WEEWA_MPAAS_REPOSITORY_USER` 和 `WEEWA_MPAAS_REPOSITORY_PASSWORD` 配置，仓库地址应使用 HTTPS。

调试包使用 Android 默认调试签名。需要签名发布包时，可在构建环境设置 `WEEWA_SIGNING_STORE_FILE`、`WEEWA_SIGNING_STORE_PASSWORD`、`WEEWA_SIGNING_KEY_ALIAS` 和 `WEEWA_SIGNING_KEY_PASSWORD`。

云存储、账号、微信和支付功能需要部署方配置自己的服务。云存储访问凭据应由服务端提供。
