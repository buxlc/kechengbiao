(function () {
  // ---------- 预设作息时间（12 节课） ----------
  var LntuTimeSlots = [
    { number: 1,  startTime: "08:00", endTime: "08:45" },
    { number: 2,  startTime: "08:55", endTime: "09:40" },
    { number: 3,  startTime: "10:00", endTime: "10:45" },
    { number: 4,  startTime: "10:55", endTime: "11:40" },
    { number: 5,  startTime: "14:00", endTime: "14:45" },
    { number: 6,  startTime: "14:55", endTime: "15:40" },
    { number: 7,  startTime: "16:00", endTime: "16:45" },
    { number: 8,  startTime: "16:55", endTime: "17:40" },
    { number: 9,  startTime: "19:00", endTime: "19:45" },
    { number: 10, startTime: "19:55", endTime: "20:40" },
    { number: 11, startTime: "20:50", endTime: "21:35" },
    { number: 12, startTime: "21:45", endTime: "22:30" }
  ];

  function getBaseUrl() {
    const fullUrl = String(window.location.href || "").split("#")[0];
    const match = fullUrl.match(/^(https?:\/\/.+?)\/eams\//i);
    if (match && match[1]) return match[1];
    throw new Error("LNTU EAMS base url not found. Open the final schedule page first.");
  }

  function cleanArg(value) {
    const text = String(value || "").trim();
    if (text === "null") return null;
    return text.replace(/^["']|["']$/g, "");
  }

  function splitArgs(paramsRaw) {
    const parts = [];
    let current = "";
    let depth = 0;
    let quote = "";
    let inQuote = false;

    for (let index = 0; index < paramsRaw.length; index++) {
      const char = paramsRaw[index];
      if ((char === '"' || char === "'") && paramsRaw[index - 1] !== "\\") {
        if (!inQuote) {
          inQuote = true;
          quote = char;
        } else if (quote === char) {
          inQuote = false;
          quote = "";
        }
      }

      if (!inQuote) {
        if (char === "(" || char === "[" || char === "{") depth++;
        if (char === ")" || char === "]" || char === "}") depth--;
      }

      if (char === "," && depth === 0 && !inQuote) {
        parts.push(cleanArg(current));
        current = "";
      } else {
        current += char;
      }
    }

    parts.push(cleanArg(current));
    return parts;
  }

  function uniqueSorted(numbers) {
    return Array.from(new Set(numbers.filter((item) => Number.isInteger(item) && item > 0))).sort((a, b) => a - b);
  }

  function mergeContinuousLessons(lessons) {
    if (!Array.isArray(lessons) || lessons.length === 0) return [];

    const grouped = {};
    lessons.forEach((lesson) => {
      const key = [lesson.name, lesson.teacher, lesson.position, lesson.day].join("|");
      if (!grouped[key]) {
        grouped[key] = {
          name: lesson.name,
          teacher: lesson.teacher,
          position: lesson.position,
          day: lesson.day,
          weeksMatrix: Array.from({ length: 35 }, function () { return new Set(); })
        };
      }

      (lesson.weeks || []).forEach((week) => {
        if (week < 1 || week >= grouped[key].weeksMatrix.length) return;
        for (let section = lesson.startSection; section <= lesson.endSection; section++) {
          grouped[key].weeksMatrix[week].add(section);
        }
      });
    });

    const merged = [];
    Object.keys(grouped).forEach((key) => {
      const group = grouped[key];
      const rangeMap = {};

      for (let week = 1; week < group.weeksMatrix.length; week++) {
        const sections = Array.from(group.weeksMatrix[week]).sort((a, b) => a - b);
        if (sections.length === 0) continue;

        let start = sections[0];
        let end = sections[0];
        for (let index = 1; index < sections.length; index++) {
          const current = sections[index];
          if (current === end + 1) {
            end = current;
          } else {
            const rangeKey = start + "-" + end;
            if (!rangeMap[rangeKey]) rangeMap[rangeKey] = [];
            rangeMap[rangeKey].push(week);
            start = current;
            end = current;
          }
        }

        const finalRangeKey = start + "-" + end;
        if (!rangeMap[finalRangeKey]) rangeMap[finalRangeKey] = [];
        rangeMap[finalRangeKey].push(week);
      }

      Object.keys(rangeMap).forEach((rangeKey) => {
        const range = rangeKey.split("-").map((item) => parseInt(item, 10));
        merged.push({
          name: group.name,
          teacher: group.teacher,
          position: group.position,
          day: group.day,
          startSection: range[0],
          endSection: range[1],
          weeks: rangeMap[rangeKey]
        });
      });
    });

    return merged.sort((a, b) => {
      if (a.day !== b.day) return a.day - b.day;
      if (a.startSection !== b.startSection) return a.startSection - b.startSection;
      return a.name.localeCompare(b.name);
    });
  }

  function parseTaskActivities(html) {
    const rawLessons = [];
    const blocks = String(html || "").split(/var\s+teachers\s*=/);

    for (let index = 1; index < blocks.length; index++) {
      const block = blocks[index];
      const activityMatch = block.match(/new\s+TaskActivity\(([\s\S]*?)\);/);
      if (!activityMatch) continue;

      const teacherMatch = block.match(/actTeachers\s*=\s*\[\s*\{[\s\S]*?name:\s*"(.*?)"/);
      const teacher = teacherMatch ? teacherMatch[1].trim() : "";
      const args = splitArgs(activityMatch[1]);
      const courseName = String(args[3] || "").split("(")[0].trim();
      const position = String(args[5] || "").replace(/\(.*?\)/g, "").trim();
      const weeksBitmap = String(args[6] || "");
      const unitCountMatch = block.match(/unitCount\s*=\s*(\d+)/);
      const unitCount = unitCountMatch ? parseInt(unitCountMatch[1], 10) : 14;

      if (!courseName) continue;

      const weeks = [];
      for (let weekIndex = 0; weekIndex < weeksBitmap.length; weekIndex++) {
        if (weeksBitmap[weekIndex] === "1") weeks.push(weekIndex);
      }

      const indexRegex = /index\s*=\s*(\d+)\s*\*\s*unitCount\s*\+\s*(\d+);/g;
      let match = null;
      let matchedIndexedBlock = false;
      while ((match = indexRegex.exec(block)) !== null) {
        matchedIndexedBlock = true;
        const day = parseInt(match[1], 10) + 1;
        const section = parseInt(match[2], 10) + 1;
        if (day < 1 || day > 7) continue;

        rawLessons.push({
          name: courseName,
          teacher: teacher,
          position: position,
          day: day,
          startSection: section,
          endSection: section,
          weeks: weeks.length > 0 ? weeks : [1]
        });
      }

      if (!matchedIndexedBlock && unitCount > 0) {
        const fallbackIndexRegex = /index\s*=\s*(\d+);/g;
        while ((match = fallbackIndexRegex.exec(block)) !== null) {
          const absoluteIndex = parseInt(match[1], 10);
          const day = Math.floor(absoluteIndex / unitCount) + 1;
          const section = (absoluteIndex % unitCount) + 1;
          if (day < 1 || day > 7) continue;
          rawLessons.push({
            name: courseName,
            teacher: teacher,
            position: position,
            day: day,
            startSection: section,
            endSection: section,
            weeks: weeks.length > 0 ? weeks : [1]
          });
        }
      }
    }

    return mergeContinuousLessons(rawLessons).map(function (lesson) {
      return {
        name: lesson.name,
        teacher: lesson.teacher,
        position: lesson.position,
        day: lesson.day,
        startSection: lesson.startSection,
        endSection: lesson.endSection,
        weeks: uniqueSorted(lesson.weeks)
      };
    });
  }

  function parseEntryParams(entryHtml) {
    const idsMatch = String(entryHtml || "").match(/bg\.form\.addInput\(form,"ids","(\d+)"\)/);
    const tagIdMatch = String(entryHtml || "").match(/id="(semesterBar\d+Semester)"/);
    return {
      studentId: idsMatch ? idsMatch[1] : "",
      tagId: tagIdMatch ? tagIdMatch[1] : ""
    };
  }

  function parseSemesterResponse(rawText) {
    let data = null;
    try {
      data = Function("return (" + String(rawText || "").trim() + ");")();
    } catch (error) {
      throw new Error("Failed to parse semester payload");
    }

    const semesters = [];
    if (!data || !data.semesters || typeof data.semesters !== "object") return semesters;

    Object.keys(data.semesters).forEach(function (key) {
      const list = data.semesters[key];
      if (!Array.isArray(list)) return;
      list.forEach(function (item) {
        if (!item || !item.id) return;
        semesters.push({
          id: String(item.id),
          schoolYear: String(item.schoolYear || ""),
          name: String(item.name || "")
        });
      });
    });

    return semesters;
  }

  function pickSemester(semesters) {
    if (!Array.isArray(semesters) || semesters.length === 0) return null;

    const now = new Date();
    const month = now.getMonth() + 1; // 1-12
    const fullYear = now.getFullYear();

    // 根据当前月份推断当前学年起始年
    // 2~8月: 当前学年起始年 = 去年（处于上一学年开始的新学年的第二学期）
    // 9~1月: 当前学年起始年 = 当年（当前学年的第一学期）
    const academicStartYear = (month >= 2 && month <= 8) ? fullYear - 1 : fullYear;
    const targetSchoolYear = academicStartYear + "-" + (academicStartYear + 1);

    // 优先精确匹配当前学年
    const exactMatches = semesters.filter(function (s) {
      return s.schoolYear === targetSchoolYear;
    });

    if (exactMatches.length > 0) {
      if (month >= 2 && month <= 8) {
        // 春季学期（第二学期）
        return exactMatches.find(function (s) { return s.name === "2"; }) || exactMatches[0];
      } else {
        // 秋季学期（第一学期）
        return exactMatches.find(function (s) { return s.name === "1"; }) || exactMatches[0];
      }
    }

    // 兆底：从最新学期中根据月份偏好选择
    const sorted = semesters.slice().sort(function (a, b) {
      if (a.schoolYear !== b.schoolYear) return b.schoolYear.localeCompare(a.schoolYear);
      return b.name.localeCompare(a.name);
    });

    if (month >= 2 && month <= 8) {
      return sorted.find(function (s) { return s.name === "2"; }) || sorted[0];
    }
    return sorted.find(function (s) { return s.name === "1"; }) || sorted[0];
  }

  async function requestText(url, options) {
    const response = await fetch(url, Object.assign({
      credentials: "include"
    }, options || {}));
    if (!response.ok) {
      throw new Error("Request failed: " + response.status);
    }
    return await response.text();
  }

  async function fetchCourses() {
    const base = getBaseUrl();
    const entryHtml = await requestText(base + "/eams/courseTableForStd.action?sf_request_type=ajax");
    const params = parseEntryParams(entryHtml);
    if (!params.studentId || !params.tagId) {
      throw new Error("Student context not found. Please confirm you are logged in.");
    }

    const semesterRaw = await requestText(base + "/eams/dataQuery.action?sf_request_type=ajax", {
      method: "POST",
      headers: {
        "content-type": "application/x-www-form-urlencoded; charset=UTF-8"
      },
      body: "tagId=" + encodeURIComponent(params.tagId) + "&dataType=semesterCalendar"
    });
    const selectedSemester = pickSemester(parseSemesterResponse(semesterRaw));
    if (!selectedSemester) {
      throw new Error("No semester available for current account.");
    }

    const courseHtml = await requestText(base + "/eams/courseTableForStd!courseTable.action?sf_request_type=ajax", {
      method: "POST",
      headers: {
        "content-type": "application/x-www-form-urlencoded; charset=UTF-8"
      },
      body: [
        "ignoreHead=1",
        "setting.kind=std",
        "startWeek=",
        "project.id=1",
        "semester.id=" + encodeURIComponent(selectedSemester.id),
        "ids=" + encodeURIComponent(params.studentId)
      ].join("&")
    });

    return parseTaskActivities(courseHtml);
  }

  (async function bootstrap() {
    try {
      // 1. 显示引导公告
      await window.AndroidBridgePromise.showAlert(
        "辽工大教务导入",
        "本脚本适配辽宁工程技术大学 WebVPN + EAMS 教务系统。\n" +
        "请确保已登录教务系统并进入课表页面。\n" +
        "学年学期将自动检测。",
        "好的，开始导入"
      );

      // 2. 获取课程数据
      AndroidBridge.showToast("正在获取课表数据...");
      const courses = await fetchCourses();
      if (!courses || courses.length === 0) {
        throw new Error("未解析到课程数据。请确认已进入课表页面且登录状态正常。");
      }

      // 3. 保存课程
      await window.AndroidBridgePromise.saveImportedCourses(JSON.stringify(courses));

      // 4. 保存预设作息时间
      try {
        await window.AndroidBridgePromise.savePresetTimeSlots(JSON.stringify(LntuTimeSlots));
      } catch (e) {
        AndroidBridge.showToast("课程已导入，作息时间导入失败：" + e.message);
      }

      // 5. 保存课表配置（26 周）
      try {
        await window.AndroidBridgePromise.saveCourseConfig(JSON.stringify({ semesterTotalWeeks: 26 }));
      } catch (e) {
        console.warn("保存课表配置失败（不影响导入）:", e);
      }

      // 6. 完成
      AndroidBridge.showToast("成功导入 " + courses.length + " 门课程！");
      AndroidBridge.notifyTaskCompletion();
    } catch (error) {
      console.error("[LNTU_IMPORT]", error);
      await window.AndroidBridgePromise.showAlert(
        "导入失败",
        error && error.message ? error.message : String(error),
        "确定"
      );
    }
  })();
})();
