/**
 * 拾光课程表适配脚本 - 广西医科大学 (jwxt.gxmu.edu.cn)
 * 教务系统：广州乘方科技有限公司（乘方教务）
 *
 * 注意：由于 Android WebView 的 X-Requested-With 请求头问题，
 * 教务页面在软件内部无法正常显示课表。本脚本完全通过
 * Fetch API 调用后端接口获取数据，不依赖页面 HTML 解析。
 *
 * 桥接方法限制：仅使用 showAlert / saveImportedCourses /
 * savePresetTimeSlots / saveCourseConfig（已确认全部可用）。
 * 学年学期通过日期自动推断 + 接口探测，无需用户交互。
 */

// ---------- 预设作息时间（13 节课，从教务页面提取） ----------
const GxmuTimeSlots = [
    { number: 1,  startTime: "08:00", endTime: "09:00" },
    { number: 2,  startTime: "08:45", endTime: "09:45" },
    { number: 3,  startTime: "09:40", endTime: "10:35" },
    { number: 4,  startTime: "10:30", endTime: "11:25" },
    { number: 5,  startTime: "11:20", endTime: "12:10" },
    { number: 6,  startTime: "14:30", endTime: "15:40" },
    { number: 7,  startTime: "15:15", endTime: "16:30" },
    { number: 8,  startTime: "16:05", endTime: "17:20" },
    { number: 9,  startTime: "16:50", endTime: "18:10" },
    { number: 10, startTime: "19:00", endTime: "20:10" },
    { number: 11, startTime: "19:45", endTime: "21:00" },
    { number: 12, startTime: "20:30", endTime: "21:50" },
    { number: 13, startTime: "21:15", endTime: "22:40" }
];

// ---------- 解析工具函数 ----------

/**
 * 解析周次字符串，例如 "1,2,3,4,5" -> [1,2,3,4,5]
 * 支持范围和逗号混合，例如 "1-5,8,10-12"
 */
function parseWeeks(weekStr) {
    if (!weekStr) return [];
    const weeks = new Set();
    String(weekStr).split(',').forEach(part => {
        const trimmed = part.trim();
        if (!trimmed) return;
        if (trimmed.includes('-')) {
            const [start, end] = trimmed.split('-').map(n => parseInt(n, 10));
            if (!isNaN(start) && !isNaN(end) && start <= end) {
                for (let i = start; i <= end; i++) weeks.add(i);
            }
            return;
        }
        const week = parseInt(trimmed, 10);
        if (!isNaN(week) && week > 0) weeks.add(week);
    });
    return Array.from(weeks).sort((a, b) => a - b);
}

/**
 * 从 jxcdmc2（按周展开的场地字符串）中提取去重后的教室列表
 */
function extractLocationsFromJxcdmc2(jxcdmc2) {
    if (!jxcdmc2) return [];
    const locationSet = new Set();
    String(jxcdmc2).split(",").forEach(item => {
        const trimmed = item.trim();
        if (!trimmed) return;
        const match = trimmed.match(/^(.*?)-(\d+)$/);
        const location = (match ? match[1] : trimmed).trim();
        if (location && location !== "-1") locationSet.add(location);
    });
    return Array.from(locationSet);
}

/**
 * 按优先级生成课程地点文案
 */
function resolvePosition(item) {
    const primary = String(item.jxcdmc || "").trim();
    if (primary) return primary;
    if (String(item.bapjxcd || "") === "1") return "不用场地";
    const fallbackLocations = extractLocationsFromJxcdmc2(item.jxcdmc2);
    if (fallbackLocations.length > 0) return fallbackLocations.join("、");
    return "待定";
}

/**
 * 将教务系统返回的 JSON 转换为标准课程数组
 */
function parseCourseList(apiJson) {
    if (!apiJson || apiJson.code !== 0 || !Array.isArray(apiJson.data)) {
        return [];
    }

    const courseMap = new Map();

    apiJson.data.forEach(item => {
        const day = parseInt(item.xq, 10);
        const startSection = parseInt(item.ps, 10);
        const endSection = parseInt(item.pe, 10);
        const weeks = parseWeeks(item.zc);

        if (
            !item.kcmc ||
            isNaN(day) || isNaN(startSection) || isNaN(endSection) ||
            day < 1 || day > 7 ||
            startSection > endSection ||
            weeks.length === 0
        ) {
            return;
        }

        const teacher = (item.teaxms || item.pkr || "").trim() || "未知";
        const position = resolvePosition(item);
        const key = [
            item.kcmc.trim(), teacher, position,
            day, startSection, endSection, weeks.join(',')
        ].join("__");

        if (!courseMap.has(key)) {
            courseMap.set(key, {
                name: item.kcmc.trim(),
                teacher,
                position,
                day,
                startSection,
                endSection,
                weeks
            });
        }
    });

    return Array.from(courseMap.values()).sort((a, b) =>
        a.day - b.day || a.startSection - b.startSection ||
        a.endSection - b.endSection || a.name.localeCompare(b.name)
    );
}

// ---------- 学期自动检测 ----------

/**
 * 根据当前日期推断最可能的学期代码列表（按优先级排序）。
 * 学期代码格式：{4位起始年份}{01|02}，例如 202502 = 2025-2026学年第二学期。
 *
 * 策略：
 *   - 2~7月 → 优先春季(02)，起始年 = 去年
 *   - 8~1月 → 优先秋季(01)，起始年 = 当年（8~12月）或去年（1月）
 *   - 每种情况再附加另一个学期作为备选
 */
function guessSemesterCodes() {
    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth() + 1; // 1-12

    const candidates = [];

    if (month >= 2 && month <= 7) {
        // 春季学期（第二学期），起始年 = 去年
        candidates.push({ code: (year - 1) + "02", label: (year - 1) + "-" + year + " 春季学期" });
        candidates.push({ code: (year - 1) + "01", label: (year - 1) + "-" + year + " 秋季学期" });
        candidates.push({ code: year + "01",      label: year + "-" + (year + 1) + " 秋季学期" });
    } else if (month >= 8 && month <= 12) {
        // 秋季学期（第一学期），起始年 = 当年
        candidates.push({ code: year + "01",      label: year + "-" + (year + 1) + " 秋季学期" });
        candidates.push({ code: year + "02",      label: year + "-" + (year + 1) + " 春季学期" });
        candidates.push({ code: (year - 1) + "02", label: (year - 1) + "-" + year + " 春季学期" });
    } else {
        // 1月：可能是秋季学期末
        candidates.push({ code: (year - 1) + "01", label: (year - 1) + "-" + year + " 秋季学期" });
        candidates.push({ code: (year - 1) + "02", label: (year - 1) + "-" + year + " 春季学期" });
    }

    return candidates;
}

// ---------- 网络请求 ----------

/**
 * 请求指定学期的课程数据
 */
async function fetchCourseData(xnxqdm) {
    const body = "xnxqdm=" + encodeURIComponent(xnxqdm) +
                 "&zc=&d1=2020-01-01+00:00:00&d2=2040-01-01+00:00:00";

    const response = await fetch("/new/student/xsgrkb/getCalendarWeekDatas", {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
        },
        body: body,
        credentials: "include"
    });

    if (!response.ok) {
        throw new Error("课表请求失败（HTTP " + response.status + "）");
    }

    return await response.json();
}

/**
 * 从校历接口获取学期开始日期
 */
async function fetchSemesterStartDate(xnxqdm) {
    try {
        const response = await fetch("/new/xlxx/data", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
            },
            body: "xnxqdm=" + encodeURIComponent(xnxqdm),
            credentials: "include"
        });

        if (!response.ok) return null;

        const result = await response.json();
        if (!result || !result.data || !Array.isArray(result.data) || result.data.length === 0) {
            return null;
        }

        // 第一个月数据：第0行是表头，第1行是第1周的日期
        const firstMonth = result.data[0];
        if (!firstMonth || firstMonth.length < 2) return null;

        const firstWeekRow = firstMonth[1];
        if (!firstWeekRow || firstWeekRow.length < 2) return null;

        for (let i = 1; i < firstWeekRow.length; i++) {
            const cell = firstWeekRow[i];
            if (cell && cell.type === "day" && cell.fullText && /^\d{4}-\d{2}-\d{2}$/.test(cell.fullText)) {
                return cell.fullText;
            }
        }

        return null;
    } catch (error) {
        console.warn("获取学期开始日期失败:", error);
        return null;
    }
}

// ---------- 主流程 ----------

async function runImportFlow() {
    try {
        // 1. 显示引导公告（showAlert 会自动确认，不会阻塞）
        await window.AndroidBridgePromise.showAlert(
            "广西医科大学教务导入",
            "本脚本将通过接口直接获取课程数据。\n" +
            "请确保已在浏览器中成功登录教务系统。\n" +
            "学年学期将自动检测。",
            "好的，开始导入"
        );

        // 2. 自动检测学期
        AndroidBridge.showToast("正在自动检测当前学期...");
        const candidates = guessSemesterCodes();
        let selectedCode = null;
        let selectedLabel = null;
        let courses = [];

        for (let i = 0; i < candidates.length; i++) {
            const candidate = candidates[i];
            console.log("GXMU: 尝试学期 " + candidate.label + " (" + candidate.code + ")");

            try {
                const apiJson = await fetchCourseData(candidate.code);
                courses = parseCourseList(apiJson);

                if (courses.length > 0) {
                    selectedCode = candidate.code;
                    selectedLabel = candidate.label;
                    console.log("GXMU: 成功匹配 " + candidate.label + "，共 " + courses.length + " 门课程");
                    break;
                }
            } catch (e) {
                console.warn("GXMU: 尝试 " + candidate.label + " 失败:", e.message);
            }
        }

        if (courses.length === 0) {
            await window.AndroidBridgePromise.showAlert(
                "未找到课程数据",
                "已尝试所有可能的学期但未找到课程。\n" +
                "请确认：\n" +
                "1. 已在浏览器中成功登录教务系统\n" +
                "2. 当前学期确实有排课数据",
                "确定"
            );
            return;
        }

        AndroidBridge.showToast("检测到：" + selectedLabel + "，共 " + courses.length + " 门课程");

        // 3. 保存课程
        await window.AndroidBridgePromise.saveImportedCourses(JSON.stringify(courses));

        // 4. 保存预设作息时间
        try {
            await window.AndroidBridgePromise.savePresetTimeSlots(JSON.stringify(GxmuTimeSlots));
        } catch (e) {
            AndroidBridge.showToast("课程已导入，作息时间导入失败：" + e.message);
        }

        // 5. 尝试获取学期开始日期并保存课表配置
        try {
            const startDate = await fetchSemesterStartDate(selectedCode);
            const configData = { semesterTotalWeeks: 22 };
            if (startDate) {
                configData.semesterStartDate = startDate;
            }
            await window.AndroidBridgePromise.saveCourseConfig(JSON.stringify(configData));
        } catch (e) {
            console.warn("保存课表配置失败（不影响导入）:", e);
        }

        // 6. 完成
        AndroidBridge.showToast("成功导入 " + courses.length + " 门课程！（" + selectedLabel + "）");
        AndroidBridge.notifyTaskCompletion();
        console.log("GXMU: 导入流程成功完成 - " + selectedLabel);
    } catch (e) {
        console.error("GXMU import failed:", e);
        await window.AndroidBridgePromise.showAlert(
            "导入失败",
            e.message || String(e),
            "确定"
        );
    }
}

// 启动入口
runImportFlow();
