# iOS JSB 迁移兼容记录

基准：公开仓库 `tiktok/sparkling` 的 `c4ce8d25c5ea277e13752d68ff1f2a66f5704240`，包含 `packages/methods/*/ios` 及 Playground 的 RouterServiceImpl、StorageServiceImpl。`80d52eb` 是本地框架替换实验提交，不作为原版基准。

## 迁移原则

保留原开源实现的参数、业务分支与回调语义，只适配 SPKMethod 的注册、模型和调用接口。旧实现未生效的选项不在本次补齐；旧问题单独记录。宏注册继续使用 `@SPKGlobalMethod`。

## 本轮恢复

| 方法 | 处理 |
| --- | --- |
| router.open / router.close | 调用前通过现有 invocation hook 将发起请求的 LynxView 弱引用写入参数模型上下文。路由继续使用原 SPKRouter；恢复 replace 的三种时序和先关闭再回调的顺序。移除通过窗口查找当前页面的迁移逻辑。没有调用方上下文时不猜测容器。 |
| storage.setItem / getItem / removeItem | 继续使用原 Playground 的 `com.SPK.custom.userdefault` suite，直接使用原 key。恢复直接写入和原 AnyCodableValue 的返回值过滤规则，不新增校验、TTL、biz 分区。JS 包已有检查保留。 |
| media.chooseMedia | 恢复原 SPKDefaultMediaPicker（UIImagePickerController）、相册/相机权限流程、提示、取消、图片压缩、文件生成及结果字段。移除 PHPicker 和多选实现。恢复原 20 个 JSON 映射；compressImage、needBase64Data、saveToPhotoAlbum 保留原属性但不增加映射；isMultiSelect 继续只是 JS 侧选项。 |
| media.saveDataURL | 恢复原 base64 解析、文件命名/写入、相册授权和回调流程，撤回迁移新增的文件名校验。 |
| media.downloadFile / uploadFile / uploadImage | 撤回新增 formDataBody 处理和 HTTP 非 2xx 自动判失败的外层分支；恢复原参数映射、逐方法响应字段、无扩展名文件命名和下载保存相册流程。仅网络传输保留 URLSession 适配。 |

容器入口恢复旧链路默认主线程、显式 `threadType=CURRENT_THREAD` 使用当前线程的规则。通用运行时没有新增业务分发流程。容器上下文适配在 Sparkling SDK 中，通过 SPKMethodCallRouter.hooksProvider / willInvoke 完成，不修改内部 TikTok 工程。

## 不能声明完全等价的边界

1. 原媒体源码引用 TTNetworkManager、SPKHttpResponse 等，但公开基准没有对应网络实现。当前使用 URLSession；请求编码、超时、错误码、公共参数注入无法与缺失后端逐项对照。needCommonParams 仅保留参数，不伪造公共参数。原外层代码没有按 HTTP 状态分支，但旧网络后端是否会把非 2xx 转成 error 无法确认。
2. 原媒体代码使用的 `spk_stringByStrippingSandboxPath` / `spk_stringFromProcessFile` 在公开基准没有实现。当前仍使用可读取的绝对路径，上传保留已有 file URL 解析；不自行猜测原路径协议。
3. 原服务查找通过旧框架 DIProvider；本实验保留原 Playground 的具体路由和存储实现，直接接到新 SPKMethod。没有恢复旧框架的服务替换 API。
4. SPKMethod 模型现在由 Mantle 处理，通用参数错误、空值序列化和状态码仍服从新框架。业务流程恢复不等于两个框架的所有边界输入输出完全一致。

## 保留的原版问题

`SPKResponder.isTopViewController(viewController:)` 在公开基准中忽略传入参数，比较自身的 topViewController。覆盖页面场景因此可能仍关闭导航栈顶。本轮只修复迁移遗漏的调用方上下文，不改原 SPKRouter / SPKResponder。回归测试核对上下文身份，并将结果与直接调用原 SPKRouter 做差分比较，不把旧路由行为写成已修复。

## 验证

`SparklingGoTests/JSBParameterCompatibilityTests.swift` 覆盖调用方页面被另一页覆盖时的上下文和旧路由行为一致性、上下文弱引用、缺失上下文不猜测页面、原 picker 类型和未映射字段、参数默认值、压缩、已有存储数据、base64、响应字段及 URLSession 适配请求。

运行 SparklingGo scheme 的 `-only-testing:SparklingGoTests/JSBParameterCompatibilityTests`。网络测试只访问本机回环服务，不访问外部网络。真机相机和权限提示的手工验证仍需在设备上完成。
