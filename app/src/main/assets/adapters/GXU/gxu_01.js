// 广西大学(gxu.edu.cn) 拾光课程表适配脚本
// 基于正方教务系统接口适配
// API: POST /jwglxt/kbcx/xskbcx_cxXsKb.html

// ---- 预设作息时间（广西大学标准） ----
var GxuTimeSlots = [
    { number: 1, startTime: "08:00", endTime: "08:45" },
    { number: 2, startTime: "08:55", endTime: "09:40" },
    { number: 3, startTime: "10:00", endTime: "10:45" },
    { number: 4, startTime: "10:55", endTime: "11:40" },
    { number: 5, startTime: "14:30", endTime: "15:15" },
    { number: 6, startTime: "15:25", endTime: "16:10" },
    { number: 7, startTime: "16:30", endTime: "17:15" },
    { number: 8, startTime: "17:25", endTime: "18:10" },
    { number: 9, startTime: "19:30", endTime: "20:15" },
    { number: 10, startTime: "20:25", endTime: "21:10" },
    { number: 11, startTime: "21:20", endTime: "22:05" }
];

// ---- 自动检测学年学期 ----
function detectSemester() {
    var now = new Date();
    var year = now.getFullYear();
    var month = now.getMonth() + 1;
    var xnm, xqm, label;

    if (month >= 2 && month <= 8) {
        // 春季/夏季 → 第二学期
        xnm = (year - 1).toString();
        xqm = "12";
        label = (year - 1) + "-" + year + " 第二学期";
    } else {
        // 秋季/冬季 → 第一学期
        xnm = year.toString();
        xqm = "3";
        label = year + "-" + (year + 1) + " 第一学期";
    }

    return { xnm: xnm, xqm: xqm, label: label };
}

// ---- 解析周次 ----
function parseWeeks(weekStr) {
    if (!weekStr) return [];
    var weekSets = weekStr.split(',');
    var weeks = [];

    for (var i = 0; i < weekSets.length; i++) {
        var seg = weekSets[i].trim();
        var rangeMatch = seg.match(/(\d+)-(\d+)周/);
        var singleMatch = seg.match(/^(\d+)周/);
        var start = 0, end = 0, processed = false;

        if (rangeMatch) {
            start = Number(rangeMatch[1]);
            end = Number(rangeMatch[2]);
            processed = true;
        } else if (singleMatch) {
            start = end = Number(singleMatch[1]);
            processed = true;
        }

        if (processed) {
            var isSingle = seg.indexOf('(单)') >= 0;
            var isDouble = seg.indexOf('(双)') >= 0;
            for (var w = start; w <= end; w++) {
                if (isSingle && w % 2 === 0) continue;
                if (isDouble && w % 2 !== 0) continue;
                weeks.push(w);
            }
        }
    }

    // 去重排序
    var unique = {};
    var result = [];
    for (var j = 0; j < weeks.length; j++) {
        if (!unique[weeks[j]]) {
            unique[weeks[j]] = true;
            result.push(weeks[j]);
        }
    }
    result.sort(function(a, b) { return a - b; });
    return result;
}

// ---- 解析课表 JSON ----
function parseCourseData(jsonData) {
    if (!jsonData || !Array.isArray(jsonData.kbList)) {
        return [];
    }

    var rawList = jsonData.kbList;
    var courses = [];

    for (var i = 0; i < rawList.length; i++) {
        var raw = rawList[i];
        if (!raw.kcmc || !raw.xm || !raw.xqj || !raw.jcs || !raw.zcd) {
            continue;
        }

        var weeks = parseWeeks(raw.zcd);
        if (weeks.length === 0) continue;

        var sectionParts = raw.jcs.split('-');
        var startSection = Number(sectionParts[0]);
        var endSection = Number(sectionParts[sectionParts.length - 1]);
        var day = Number(raw.xqj);

        if (isNaN(day) || isNaN(startSection) || isNaN(endSection) ||
            day < 1 || day > 7 || startSection > endSection) {
            continue;
        }

        courses.push({
            name: raw.kcmc.trim(),
            teacher: raw.xm.trim(),
            position: (raw.cdmc || "").trim(),
            day: day,
            startSection: startSection,
            endSection: endSection,
            weeks: weeks
        });
    }

    courses.sort(function(a, b) {
        return a.day - b.day || a.startSection - b.startSection || a.name.localeCompare(b.name);
    });

    return courses;
}

// ---- 请求课表数据 ----
async function fetchCourses(xnm, xqm) {
    var baseUrl = window.location.origin;
    var apiUrl = baseUrl + "/jwglxt/kbcx/xskbcx_cxXsKb.html";
    var body = "xnm=" + xnm + "&xqm=" + xqm;

    var resp = await fetch(apiUrl, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
        body: body,
        credentials: "include"
    });

    if (!resp.ok) {
        throw new Error("网络请求失败，状态码: " + resp.status);
    }

    var text = await resp.text();
    var json;
    try {
        json = JSON.parse(text);
    } catch (e) {
        throw new Error("数据解析失败，可能未登录或会话过期");
    }

    return json;
}

// ---- 保存课程 ----
async function saveCourses(courses) {
    await window.AndroidBridgePromise.saveImportedCourses(JSON.stringify(courses, null, 2));
}

// ---- 保存作息时间 ----
async function saveTimeSlots(timeSlots) {
    await window.AndroidBridgePromise.savePresetTimeSlots(JSON.stringify(timeSlots));
}

// ---- 保存课表配置 ----
async function saveConfig(config) {
    await window.AndroidBridgePromise.saveCourseConfig(JSON.stringify(config));
}

// ---- 主流程 ----
async function runImportFlow() {
    // 1. 提示用户
    var confirmed = await window.AndroidBridgePromise.showAlert(
        "广西大学课表导入",
        "请确保已登录教务系统并处于课表相关页面。\n\n系统将自动检测当前学年学期并获取课表数据。",
        "好的，开始导入"
    );
    if (!confirmed) {
        AndroidBridge.showToast("用户取消了导入。");
        return;
    }

    // 2. 检测学期
    var semester = detectSemester();
    AndroidBridge.showToast("检测到学期: " + semester.label);

    // 3. 请求数据
    AndroidBridge.showToast("正在获取课表数据...");
    var jsonData;
    try {
        jsonData = await fetchCourses(semester.xnm, semester.xqm);
    } catch (error) {
        await window.AndroidBridgePromise.showAlert(
            "导入失败",
            "获取课表数据失败: " + error.message + "\n\n请确认已登录教务系统。",
            "确定"
        );
        return;
    }

    // 4. 检查学期信息
    if (jsonData.xsxx) {
        var actualLabel = jsonData.xsxx.XNMC + " 第" + jsonData.xsxx.XQMMC + "学期";
        AndroidBridge.showToast("当前学期: " + actualLabel + "，共" + jsonData.xsxx.KCMS + "门课");
    }

    // 5. 解析课程
    var courses = parseCourseData(jsonData);
    if (courses.length === 0) {
        await window.AndroidBridgePromise.showAlert(
            "导入失败",
            "未找到任何课程数据。请检查:\n1. 是否已登录教务系统\n2. 当前学年学期是否正确\n3. 本学期是否有课",
            "确定"
        );
        return;
    }

    // 6. 保存课程
    AndroidBridge.showToast("正在保存 " + courses.length + " 门课程...");
    try {
        await saveCourses(courses);
    } catch (error) {
        await window.AndroidBridgePromise.showAlert("导入失败", "课程保存失败: " + error.message, "确定");
        return;
    }

    // 7. 保存配置
    try {
        await saveConfig({ semesterTotalWeeks: 20, firstDayOfWeek: 1 });
    } catch (error) {
        console.warn("课表配置保存失败:", error);
    }

    // 8. 保存作息时间
    try {
        await saveTimeSlots(GxuTimeSlots);
    } catch (error) {
        console.warn("作息时间保存失败:", error);
    }

    // 9. 完成
    AndroidBridge.showToast("课程导入成功！共导入 " + courses.length + " 门课程");
    AndroidBridge.notifyTaskCompletion();
}

runImportFlow();
