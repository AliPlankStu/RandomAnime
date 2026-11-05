# 随机番名

一款 Android 番剧随机发现应用。支持按年份、月份和完结状态筛选番剧，并随机展示名称与封面。

## 功能
- 2000 年至当前年份，年份选择自动随系统年份扩展
- 全年或指定月份筛选
- 已完结 / 未完结两种模式
- AniList GraphQL 在线获取番剧资料与封面
- 本地缓存，减少重复请求
- GitHub Actions 自动构建 APK

## 构建
使用 Android Studio 打开项目，或在 GitHub Actions 中运行 **Build Android APK** 工作流。

最低 Android 版本：8.0（API 26）。

## 数据说明
数据来自 AniList。按番剧开始日期的月份归类；数据库收录范围受 AniList 数据完整性及接口限制影响。未完结模式包含非 COMPLETED 状态的作品。
