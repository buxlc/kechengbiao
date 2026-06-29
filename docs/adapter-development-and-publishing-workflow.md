# Bu课表 — 教务适配脚本开发与云端发布工作流

## 项目概述

**Bu课表**（拾光课程表）是一款 Android 课程表应用（包名 `com.bu.kebiao`），核心功能是通过 WebView 内嵌教务系统页面，执行 JS 适配脚本将课表数据导入本地。

### 参考文档

- **适配教务 JS 编写规范**: https://github.com/XingHeYuZhuan/shiguangschedule/wiki/%E5%A6%82%E4%BD%95%E9%80%82%E9%85%8D%E6%95%99%E5%8A%A1
  （GitHub Wiki，包含适配教务系统的完整指南、接口约定和示例代码）

### 关键目录

```
app/src/main/
├── assets/
│   ├── adapters/{SCHOOL_ID}/         # 各校适配脚本（.js）
│   │   ├── gxu_01.js                 # 广西大学（正方教务）
│   │   ├── lntu_eams_webvpn.js       # 辽宁工大（EAMS+WebVPN）
│   │   └── ...                       # 100+ 学校适配器
│   └── school_index.json             # 学校注册表（本地）
├── java/com/bu/kebiao/
│   └── ui/courseimport/
│       ├── CourseJsBridge.kt         # 原生桥接实现（@JavascriptInterface）
│       ├── JsBridgeHelper.kt         # Promise 包装层
│       └── ImportScreen.kt           # WebView 宿主页面
cloudflare/
└── publish-adapter.mjs               # R2 发布脚本
```

---

## 第一部分：适配脚本开发

### 1.1 脚本运行环境

脚本在 Android WebView 中执行。执行前会自动注入 `JsBridgeHelper.kt` 中的 Promise 包装层，将同步桥接调用包装为 `async/await` 可用的 Promise。

### 1.2 可用桥接方法（白名单）

**只能使用以下方法，调用不存在的方法会导致脚本静默失败（TypeError）：**

#### `window.AndroidBridgePromise`（异步，返回 Promise）

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `showAlert(title, content, confirmText)` | 标题、内容、按钮文字 | `Promise<boolean>` | 弹出提示框 |
| `saveImportedCourses(jsonString)` | JSON 字符串 | `Promise<boolean>` | 保存课程数据（核心） |
| `savePresetTimeSlots(jsonString)` | JSON 字符串 | `Promise<boolean>` | 保存作息时间预设 |
| `saveCourseConfig(jsonString)` | JSON 字符串 | `Promise<boolean>` | 保存课表配置（总周数等） |

#### `AndroidBridge`（同步，无返回值）

| 方法 | 参数 | 说明 |
|------|------|------|
| `showToast(message)` | 文本 | 显示 Toast 提示 |
| `notifyTaskCompletion()` | 无 | 通知导入完成，关闭页面 |

### 1.3 课程数据 JSON 格式

传给 `saveImportedCourses()` 的 JSON 必须是课程数组，支持多种字段名（Kotlin 端有容错解析）：

```json
[
  {
    "name": "高等数学",           // 课程名（必须）
    "teacher": "张三",            // 教师名
    "position": "教学楼A-301",    // 教室
    "day": 1,                     // 星期几，1=周一 ... 7=周日
    "startSection": 1,            // 起始节次
    "endSection": 2,              // 结束节次
    "weeks": [1, 2, 3, 5, 7]     // 上课周次列表（必须为整数数组）
  }
]
```

**备用字段名**（Kotlin 端自动识别）：
- 课程名: `name`, `courseName`, `kcmc`, `title`
- 星期: `day`, `dayOfWeek`, `weekday`, `xqj`
- 教师: `teacher`, `teachers`, `xm`
- 教室: `position`, `location`, `classroom`, `cdmc`
- 节次: `startSection`+`endSection`, 或 `jcs` (如 `"3-4"`)
- 周次: `weeks` (数组), 或 `zcd` (如 `"1-5周,7-11周(单)"`)

### 1.4 作息时间预设格式

传给 `savePresetTimeSlots()`：

```json
[
  { "number": 1, "startTime": "08:00", "endTime": "08:45" },
  { "number": 2, "startTime": "08:55", "endTime": "09:40" },
  { "number": 12, "startTime": "21:45", "endTime": "22:30" }
]
```

### 1.5 课表配置格式

传给 `saveCourseConfig()`：

```json
{
  "semesterTotalWeeks": 20,
  "firstDayOfWeek": 1
}
```

### 1.6 脚本编写规范

1. **禁止调用** `showPrompt`、`showSingleSelection` 等未实现方法
2. 使用 `var` 和 `function` 声明（兼容旧版 WebView），避免 `const`/`let`/箭头函数
3. 入口函数命名为 `runImportFlow()`，脚本末尾调用
4. 用 `AndroidBridge.showToast()` 显示进度
5. 用 `AndroidBridgePromise.showAlert()` 显示错误详情
6. 成功保存后必须调用 `AndroidBridge.notifyTaskCompletion()`
7. 学期检测优先使用日期自动推断，避免弹窗让用户输入

### 1.7 常见教务系统 API 模式

#### 正方教务（使用最广）

- **课表 API**: `POST /jwglxt/kbcx/xskbcx_cxXsKb.html`
- **参数**: `xnm={起始学年}&xqm={学期码}` （学期码: 3=第一学期, 12=第二学期）
- **响应**: JSON，课程在 `kbList` 数组中
- **关键字段**: `kcmc`(课名), `xm`(教师), `cdmc`(教室), `xqj`(星期), `jcs`(节次), `zcd`(周次)
- **参考脚本**: `GXU/gxu_01.js`, `HAUT/haut_01.js`

#### EAMS 教务（树维/正方）

- **课表数据**: 页面内嵌 `new TaskActivity(...)` JS 对象
- **学期 API**: `POST /eams/dataQuery.action` + `semesterBar` 参数
- **参考脚本**: `LNTU/lntu_eams_webvpn.js`

#### 乘方教务

- **课表 API**: `POST /new/student/xsgrkb/getCalendarWeekDatas`
- **参数**: `xnxq={学期代码}` (如 `202502`)
- **参考脚本**: `GXMU/gxmu_01.js`, `CMC/cmc_01.js`

### 1.8 脚本开发流程

1. **在浏览器中打开目标教务系统**，登录后导航到课表页面
2. **用浏览器 DevTools / evaluate_script 探查 API**：
   - 确认请求 URL、参数、响应格式
   - 测试不同学期参数是否能获取数据
3. **编写适配脚本**，存放到 `app/src/main/assets/adapters/{SCHOOL_ID}/{script}.js`
4. **在浏览器中验证**：用 `evaluate_script` 逐步运行脚本逻辑，确认数据解析正确
5. **注册到 `school_index.json`**

---

## 第二部分：注册学校

### 2.1 school_index.json 结构

文件位于 `app/src/main/assets/school_index.json`，是学校的注册表。

```json
{
  "schools": [
    {
      "id": "GXU",                              // 学校唯一 ID
      "name": "广西大学",                         // 显示名
      "initial": "G",                            // 拼音首字母
      "folder": "GXU",                           // 适配脚本所在文件夹名
      "adapters": [
        {
          "adapter_id": "GXU_01",                // 适配器唯一 ID
          "adapter_name": "广西大学正方教务",       // 显示名
          "category": "BACHELOR_AND_ASSOCIATE",   // 类别
          "js_path": "gxu_01.js",                // 脚本文件名（相对 folder）
          "import_url": "https://jwxt2018.gxu.edu.cn/jwglxt/xtgn/login_slogin.html",
          "description": "适配描述...",
          "maintainer": "Qoder"
        }
      ]
    }
  ]
}
```

### 2.2 category 可选值

- `BACHELOR_AND_ASSOCIATE` — 本专科教务
- `GRADUATE` — 研究生教务
- `GENERAL_TOOL` — 通用工具

### 2.3 注册步骤

1. 在 `app/src/main/assets/adapters/` 下创建学校文件夹（大写缩写，如 `GXU`）
2. 将 `.js` 适配脚本放入该文件夹
3. 在 `school_index.json` 的 `schools` 数组末尾添加学校条目
4. 验证 JSON 格式正确（`JSON.parse` 无报错）

---

## 第三部分：发布到 Cloudflare R2

### 3.1 环境配置

| 配置项 | 值 |
|--------|-----|
| Cloudflare Account ID | `<YOUR_CLOUDFLARE_ACCOUNT_ID>` |
| R2 Bucket | `<YOUR_R2_BUCKET>` |
| R2 S3 Endpoint | `https://<YOUR_CLOUDFLARE_ACCOUNT_ID>.r2.cloudflarestorage.com` |
| 公网域名 | `https://<YOUR_PUBLIC_DOMAIN>` |
| API Token | `<YOUR_CLOUDFLARE_API_TOKEN>` |

### 3.2 前置条件

```powershell
# PowerShell 需要先设置执行策略
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

# 设置 API Token
$env:CF_API_TOKEN = "<YOUR_CLOUDFLARE_API_TOKEN>"

# 确保已安装依赖（首次）
npm install @aws-sdk/client-s3 --save-dev
```

### 3.3 发布命令

```powershell
node cloudflare/publish-adapter.mjs `
  --adapter-name <ADAPTER_NAME> `
  --school-id <SCHOOL_ID> `
  --folder <FOLDER> `
  --adapter-id <ADAPTER_ID> `
  --js-path <JS_FILENAME> `
  --version <VERSION> `
  --input <LOCAL_JS_PATH> `
  --notes "<CHANGE_NOTES>"
```

**参数说明：**

| 参数 | 必填 | 说明 | 示例 |
|------|------|------|------|
| `--adapter-name` | 是 | 云端适配器唯一键 | `GXU_ZHENGFANG` |
| `--input` | 是 | 本地脚本路径（相对项目根） | `app/src/main/assets/adapters/GXU/gxu_01.js` |
| `--school-id` | 否 | 学校 ID（默认同 adapter-name） | `GXU` |
| `--folder` | 否 | R2 存储文件夹（默认同 school-id） | `GXU` |
| `--adapter-id` | 否 | 适配器 ID（默认同 adapter-name） | `GXU_01` |
| `--js-path` | 否 | 脚本文件名（默认自动生成） | `gxu_01.js` |
| `--version` | 否 | 版本号（默认 1.0.0） | `1.0.0` |
| `--notes` | 否 | 版本说明 | `"初始版本"` |

### 3.4 发布脚本自动执行的操作

1. 读取本地 `.js` 文件，计算 SHA256
2. 上传脚本到 R2：`adapters/{folder}/{adapterName}-v{version}.js`
3. 从 R2 获取现有 `manifest.json`（不存在则创建新的）
4. 在 manifest 中替换/新增该适配器条目
5. 上传更新后的 `manifest.json`
6. 读取本地 `school_index.json` 并上传到 R2（同步学校目录）

### 3.5 R2 文件结构

```
<YOUR_R2_BUCKET>/
├── manifest.json                              # 适配器清单
├── school_index.json                          # 学校目录
└── adapters/
    ├── LNTU/
    │   └── LNTU_WEBVPN_EAMS-v1.0.0.js
    └── GXU/
        └── GXU_ZHENGFANG-v1.0.0.js
```

### 3.6 manifest.json 格式

```json
{
  "adapters": [
    {
      "adapterName": "GXU_ZHENGFANG",
      "schoolId": "GXU",
      "folder": "GXU",
      "adapterId": "GXU_01",
      "jsPath": "gxu_01.js",
      "version": "1.0.0",
      "downloadUrl": "https://<YOUR_PUBLIC_DOMAIN>/adapters/GXU/GXU_ZHENGFANG-v1.0.0.js",
      "sha256": "6d9edf1ff51c9dec...",
      "enabled": true,
      "notes": "auto semester detect + API fetch + time slots"
    }
  ],
  "updatedAt": "2026-06-29T06:20:20.488Z"
}
```

### 3.7 App 端刷新流程

App 中"刷新脚本"按钮的逻辑：
1. 拉取 `https://<YOUR_PUBLIC_DOMAIN>/manifest.json`
2. 与本地缓存比对 `sha256`
3. 下载新增或变更的脚本文件
4. 显示结果：`此次新增N个脚本`

---

## 第四部分：实战案例

### 案例 A：为正方教务学校新增适配器（以广西大学为例）

```powershell
# 1. 探查 API（在浏览器中打开教务页面，用 evaluate_script 测试）
# POST /jwglxt/kbcx/xskbcx_cxXsKb.html
# body: xnm=2025&xqm=12 → 返回 kbList

# 2. 创建脚本文件
# app/src/main/assets/adapters/GXU/gxu_01.js
# - 自动检测学期 (detectSemester)
# - 调用 API 获取课表 (fetchCourses)
# - 解析 kbList → 标准课程 JSON (parseCourseData)
# - 保存课程 + 配置 + 作息时间

# 3. 注册学校（编辑 school_index.json 末尾添加）

# 4. 发布到 R2
node cloudflare/publish-adapter.mjs `
  --adapter-name GXU_ZHENGFANG `
  --school-id GXU `
  --folder GXU `
  --adapter-id GXU_01 `
  --js-path gxu_01.js `
  --version 1.0.0 `
  --input app/src/main/assets/adapters/GXU/gxu_01.js `
  --notes "初始版本：正方教务API直接获取"
```

### 案例 B：更新已有适配器版本

```powershell
# 修改脚本后，升级版本号重新发布
node cloudflare/publish-adapter.mjs `
  --adapter-name LNTU_WEBVPN_EAMS `
  --school-id LNTU `
  --folder LNTU `
  --adapter-id LNTU_01 `
  --js-path lntu_eams_webvpn.js `
  --version 2.0.0 `
  --input app/src/main/assets/adapters/LNTU/lntu_eams_webvpn.js `
  --notes "修复学期自动选择逻辑"
```

---

## 第五部分：踩坑记录

### 5.1 脚本静默失败

**现象**: 脚本执行后无任何反应，不报错
**原因**: 调用了 `AndroidBridgePromise` 中未实现的方法（如 `showPrompt`、`showSingleSelection`）
**解决**: 只使用 1.2 节白名单中的方法

### 5.2 WebView X-Requested-With 请求头

**现象**: 教务系统/VPN 拒绝 WebView 发出的请求
**原因**: Android WebView 自动添加 `X-Requested-With` 请求头（值为 App 包名）
**解决**: 在 `ImportScreen.kt` 中设置：
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    settings.requestedWithHeaderMode =
        WebSettings.REQUESTED_WITH_HEADER_MODE_NO_HEADER
}
```

### 5.3 EAMS pickSemester 学期匹配错误

**现象**: 自动选择了 2000-2001 学年而非当前学年
**原因**: `indexOf("2")` 在 `"2000-2001 1"` 中匹配到了 "2000" 的 "2"
**解决**: 先精确匹配 `schoolYear === "2025-2026"`，再按学期名筛选

### 5.4 PowerShell 执行 npm 报错

**现象**: `PSSecurityException / UnauthorizedAccess`
**解决**: 运行前先执行 `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass`

### 5.5 .mjs 文件禁止 shebang

**现象**: `SyntaxError: Invalid or unexpected token` 指向 `#!/usr/bin/env node`
**原因**: Node.js ESM 模式下 `.mjs` 文件不支持 shebang 行
**解决**: 删除 `.mjs` 文件首行的 `#!/usr/bin/env node`

---

## 第六部分：完整工作流速查

```
1. 浏览器打开教务 → 探查 API 结构
2. 编写 .js 适配脚本 → 存入 adapters/{ID}/
3. 浏览器中验证脚本 → evaluate_script 逐步测试
4. 注册 school_index.json → 添加学校+适配器条目
5. PowerShell 设置 CF_API_TOKEN → 运行 publish-adapter.mjs
6. 验证云端 manifest.json + school_index.json
7. App 端"刷新脚本"拉取最新预设
```
