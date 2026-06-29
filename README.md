# Bu课表

Bu课表是一款面向大学生日常使用的 Android 课程表应用。应用以本地课程表管理为核心，支持手动维护、教务导入、AI 截图/CSV 导入、学期管理、桌面小组件和上课提醒。

当前版本：v2.0.2

## 功能概览

- 周课表查看：按周展示课程，支持课程名称、时间、地点、教师、单双周和指定周次。
- 今日课程与周视图：可查看当天课程，也可在周视图中切换查看不同周次。
- 课程手动维护：支持新增、编辑、删除课程，并可快速调整课程颜色。
- 课程卡片密度：设置中可选择紧凑、标准、舒展三种排版，字号变化会配套调整间距和信息展示。
- 学期管理：支持创建、切换、删除学期，课程数据按学期独立管理。
- 学期导入/导出：导入时可选择目标学期，也可将指定学期导出为 CSV。
- 教务系统导入：通过学校预设和教务网页脚本导入课程表。
- AI 截图/CSV/文本导入：学校暂未适配时，可截图课表，让 AI 生成标准 CSV 或文本后导入。
- ICS 日历导入：支持从 `.ics` 日历文件导入课程。
- 导入预览确认：写入本地前先预览解析结果，确认无误后再导入。
- 桌面小组件：提供大小两种课程表小组件，可展示今日/明日课程，点击组件可回到应用。
- 后台提醒：支持上课提醒，并探索适配安卓厂商的实时状态展示入口。
- 关于页跳转：设置页关于入口可跳转到本 GitHub 项目。

## 截图展示

<p>
  <img src="docs/images/home-screenshot.jpg" alt="Bu课表周视图" width="300">
  <img src="docs/images/today-screenshot.jpg" alt="Bu课表今日视图" width="300">
  <img src="docs/images/widget-screenshot.png" alt="Bu课表桌面小组件" width="300">
</p>

## 申请适配

如果你的学校暂未支持教务导入，或者现有适配无法正常解析课程表，可以联系申请适配：

- QQ：2794995813
- 备注：申请适配

提交适配时建议提供学校名称、教务系统入口、课表页面截图，以及能说明课程数据位置的页面信息。请勿公开发送账号密码等敏感信息。

## 远程推送机制

项目预留了远程适配器推送机制，用于后续更新学校目录和教务导入脚本。整体流程如下：

1. 维护者将新的适配器脚本发布到自己的远程存储服务。
2. 远程生成或更新 `manifest.json`，记录脚本版本、下载地址、启用状态和 `sha256` 校验值。
3. 可选更新 `school_index.json`，用于新增学校或调整学校预设列表。
4. App 端点击刷新后拉取远程清单。
5. App 对比本地缓存和远程清单，下载新增或变更的脚本。
6. App 使用 `sha256` 校验脚本内容，校验通过后写入本地缓存。

这个机制用于发布教务适配脚本，不会上传用户课程数据，也不会上传用户账号信息。

## 源码隐私说明

公开源码已经去除原作者的 Cloudflare 个人信息，包括账号 ID、个人域名、bucket 名和 API Token 示例。仓库中只保留通用占位符和本地示例代码。

如果你需要自己构建并启用远程推送功能，需要自行准备远程存储服务，并填入自己的配置：

- `app/src/main/java/com/bu/kebiao/data/adapter/cloud/AdapterCloudConfig.kt`
  - `DEFAULT_MANIFEST_URL`
  - `DEFAULT_SCHOOL_INDEX_URL`
- `cloudflare/publish-adapter.mjs` 使用的环境变量
  - `CF_ACCOUNT_ID`
  - `CF_API_TOKEN`
  - `R2_BUCKET`
  - `PUBLIC_BASE_URL`

如果不配置这些信息，App 的本地课程管理、手动导入、内置学校适配和小组件功能仍可正常使用；远程刷新学校预设/脚本时会提示未配置云端地址。

## AI 截图/CSV 导入

如果学校教务系统暂未适配，可以使用“AI 截图/CSV 导入”：

1. 在教务系统或其他课表软件中截图。
2. 在 Bu课表中复制 AI 识别提示词。
3. 将截图和提示词发送给支持图片识别的 AI。
4. 复制 AI 返回的 CSV 或文本。
5. 回到 Bu课表粘贴内容，或选择 `.csv` / `.txt` 文件导入。
6. 预览无误后确认导入。

详细教程和可修改提示词见：

```text
docs/course-import-ai-csv-guide.md
```

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Room
- DataStore
- Hilt
- Navigation Compose
- App Widget
- Gradle / Android Gradle Plugin

## 项目结构

```text
app/src/main/java/com/bu/kebiao/
  data/          数据库、本地存储、适配器加载、远程适配器缓存
  domain/        领域模型与仓库接口
  liveupdate/    上课提醒与系统状态入口
  navigation/    页面导航
  ui/            Compose 页面与组件
  widget/        桌面小组件

app/src/main/assets/
  school_index.json       学校适配器索引
  adapters/               教务导入脚本资源

cloudflare/
  publish-adapter.mjs     远程适配器发布脚本模板
  worker.js               远程存储访问示例

docs/
  course-import-ai-csv-guide.md  AI 截图/CSV 导入教程
  images/                        README 截图
  release-notes/                 版本更新日志
```

## 说明

当前版本以本地课程表管理、学期数据管理和导入能力为核心。不同学校的教务系统差异较大，教务导入适配会持续迭代；AI 截图/CSV/文本导入用于补充暂未适配学校的使用场景。

后续版本计划继续优化课程提醒、小组件体验，并探索支持安卓厂商的灵动岛/实时活动类能力，让上课状态、下一节课等信息更自然地出现在系统级入口中。
