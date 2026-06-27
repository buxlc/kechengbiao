package com.bu.kebiao.ui.courseimport

/**
 * Generic course parser using a row-based strategy similar to the source project adapters.
 * It treats the row header as the base section range, then lets block-level "[xx-yy节]" override it.
 */
object CourseParserJs {
    val SCRIPT: String = """
        (function() {
          function uniqueSorted(numbers) {
            return Array.from(new Set(numbers.filter(function(value) {
              return typeof value === 'number' && !isNaN(value) && value > 0;
            }))).sort(function(a, b) { return a - b; });
          }

          function parseWeeks(weeksText) {
            if (!weeksText) return [];
            var cleaned = String(weeksText)
              .replace(/[（]/g, '(')
              .replace(/[）]/g, ')')
              .replace(/[【]/g, '[')
              .replace(/[】]/g, ']')
              .replace(/<\/?[a-z]+[^>]*>/ig, '')
              .replace(/\(周\)/ig, '')
              .replace(/\[[^\]]*节\]/ig, '')
              .replace(/\([^)]*节\)/ig, '')
              .replace(/周/g, '')
              .replace(/\s+/g, '')
              .trim();

            if (!cleaned) return [];

            var weeks = [];
            cleaned.split(/[,，;；]/).forEach(function(segment) {
              var part = segment.trim();
              if (!part) return;

              var oddOnly = part.indexOf('单') >= 0;
              var evenOnly = part.indexOf('双') >= 0;
              part = part.replace(/单周?|双周?/g, '');

              var rangeRegex = /(\d{1,2})\s*(?:-|~|—|–|至|到)\s*(\d{1,2})/g;
              var rangeMatch = null;
              var hasRange = false;
              while ((rangeMatch = rangeRegex.exec(part)) !== null) {
                hasRange = true;
                var start = parseInt(rangeMatch[1], 10);
                var end = parseInt(rangeMatch[2], 10);
                if (isNaN(start) || isNaN(end)) continue;
                for (var week = start; week <= end; week++) {
                  if (oddOnly && week % 2 === 0) continue;
                  if (evenOnly && week % 2 !== 0) continue;
                  weeks.push(week);
                }
              }

              if (!hasRange) {
                var singleMatches = part.match(/\d{1,2}/g) || [];
                singleMatches.forEach(function(value) {
                  var week = parseInt(value, 10);
                  if (!isNaN(week)) weeks.push(week);
                });
              }
            });

            return uniqueSorted(weeks);
          }

          function parseSections(sectionText) {
            if (!sectionText) return null;
            var normalized = String(sectionText)
              .replace(/[（]/g, '(')
              .replace(/[）]/g, ')')
              .replace(/[【]/g, '[')
              .replace(/[】]/g, ']');

            var match = normalized.match(/\[(\d+(?:-\d+)*)节\]/i);
            if (!match) match = normalized.match(/\((\d+(?:-\d+)*)节\)/i);
            if (!match) match = normalized.match(/第?\s*(\d{1,2})(?:\s*(?:-|~|—|–|至|到)\s*(\d{1,2}))?\s*节/);

            if (!match) return null;

            var numbers = [];
            if (match[1] && match[1].indexOf('-') >= 0) {
              numbers = match[1].split('-').map(function(value) { return parseInt(value, 10); });
            } else {
              numbers = [parseInt(match[1], 10)];
              if (match[2]) numbers.push(parseInt(match[2], 10));
            }

            numbers = numbers.filter(function(value) { return !isNaN(value) && value > 0; });
            if (numbers.length === 0) return null;

            return {
              start: Math.min.apply(null, numbers),
              end: Math.max.apply(null, numbers)
            };
          }

          function inferBaseSections(headerText, rowIndex) {
            var text = String(headerText || '').replace(/\s+/g, ' ').trim();
            var groupMatch = text.match(/\(([\d,\-]+)(?:小节|节)\)/);
            if (groupMatch) {
              var sectionNumbers = (groupMatch[1].match(/\d+/g) || [])
                .map(function(value) { return parseInt(value, 10); })
                .filter(function(value) { return !isNaN(value) && value > 0; });
              if (sectionNumbers.length > 0) {
                return {
                  start: sectionNumbers[0],
                  end: sectionNumbers[sectionNumbers.length - 1]
                };
              }
            }

            var rangeMatch = text.match(/第?\s*(\d{1,2})\s*(?:-|~|—|–|至|到)\s*(\d{1,2})\s*节/);
            if (rangeMatch) {
              return {
                start: parseInt(rangeMatch[1], 10),
                end: parseInt(rangeMatch[2], 10)
              };
            }

            if (rowIndex > 0) {
              return {
                start: rowIndex * 2 - 1,
                end: rowIndex * 2
              };
            }
            return null;
          }

          function splitCourseBlocks(rawHtml) {
            return String(rawHtml || '')
              .split(/-{10,}\s*<br\s*\/?>|---------------------|----------------------|<br>\s*<br>/i)
              .map(function(block) { return block.trim(); })
              .filter(Boolean);
          }

          function getTitleText(container, title) {
            var node = container.querySelector('font[title="' + title + '"]');
            return node ? node.innerText.trim() : '';
          }

          function getCourseName(container) {
            var firstNode = container.childNodes[0];
            if (firstNode && firstNode.nodeType === Node.TEXT_NODE) {
              var direct = firstNode.nodeValue.trim();
              if (direct) return direct;
            }

            var firstFont = container.querySelector('font');
            if (firstFont && firstFont.innerText.trim()) {
              return firstFont.innerText.trim().replace(/[●★○]/g, '');
            }

            return (container.innerText || '')
              .split('\n')
              .map(function(line) { return line.trim(); })
              .filter(Boolean)[0] || '';
          }

          function parseCourseBlock(blockHtml, dayOfWeek, baseSections) {
            var tempDiv = document.createElement('div');
            tempDiv.innerHTML = blockHtml;

            var name = getCourseName(tempDiv);
            if (!name) return null;

            var timeText = getTitleText(tempDiv, '周次(节次)');
            var teacher = getTitleText(tempDiv, '教师');
            var position = getTitleText(tempDiv, '教室');
            var weeks = parseWeeks(timeText || tempDiv.innerText);
            var sections = parseSections(timeText || tempDiv.innerText) || baseSections;

            if (!sections || sections.start <= 0 || sections.end < sections.start || weeks.length === 0) {
              return null;
            }

            return {
              name: name,
              teacher: teacher,
              position: position,
              day: dayOfWeek,
              startSection: sections.start,
              endSection: sections.end,
              weeks: weeks
            };
          }

          function parseTableByRows(table) {
            var courses = [];
            var rows = table.querySelectorAll('tr');
            if (!rows || rows.length === 0) return courses;

            for (var rowIndex = 1; rowIndex < rows.length; rowIndex++) {
              var row = rows[rowIndex];
              var headerCell = row.querySelector('th');
              var baseSections = inferBaseSections(headerCell ? headerCell.innerText : '', rowIndex);
              var cells = row.querySelectorAll('td');

              for (var cellIndex = 0; cellIndex < cells.length; cellIndex++) {
                var cell = cells[cellIndex];
                var dayOfWeek = cellIndex + 1;
                if (dayOfWeek < 1 || dayOfWeek > 7) continue;

                var contentNodes = cell.querySelectorAll('div.kbcontent, div.kbcontent1, div.timetable_con, div[class*="kbcontent"], div[class*="timetable_con"]');
                if (!contentNodes.length && cell.innerHTML.trim()) {
                  contentNodes = [cell];
                }

                Array.from(contentNodes).forEach(function(node) {
                  var rawHtml = node.innerHTML || node.outerHTML || '';
                  if (!rawHtml.trim() || rawHtml === '&nbsp;') return;

                  splitCourseBlocks(rawHtml).forEach(function(blockHtml) {
                    var course = parseCourseBlock(blockHtml, dayOfWeek, baseSections);
                    if (course) courses.push(course);
                  });
                });
              }
            }

            return courses;
          }

          function parseZhengfangList() {
            var table = document.getElementById('kblist_table');
            if (!table) return [];

            var courses = [];
            table.querySelectorAll('tbody').forEach(function(tbody, dayIndex) {
              var day = dayIndex + 1;
              if (day < 1 || day > 7) return;

              tbody.querySelectorAll('tr').forEach(function(tr, rowIndex) {
                if (rowIndex === 0) return;

                var title = tr.querySelector('.title');
                var name = title ? title.textContent.replace(/[●★○]/g, '').trim() : '';
                if (!name) return;

                var fonts = tr.querySelectorAll('p font');
                var weekText = fonts.length > 0 ? fonts[0].textContent.trim() : '';
                var position = fonts.length > 1 ? fonts[1].textContent.replace(/上课地点：/g, '').trim() : '';
                var teacher = fonts.length > 2 ? fonts[2].textContent.replace(/教师\s*：/g, '').trim() : '';
                var sectionText = tr.querySelectorAll('td')[0] ? tr.querySelectorAll('td')[0].textContent : '';

                var weeks = parseWeeks(weekText);
                var sections = parseSections(sectionText) || inferBaseSections(sectionText, rowIndex);
                if (!weeks.length || !sections) return;

                courses.push({
                  name: name,
                  day: day,
                  teacher: teacher,
                  position: position,
                  startSection: sections.start,
                  endSection: sections.end,
                  weeks: weeks
                });
              });
            });

            return courses;
          }

          function tryParse() {
            var table = document.getElementById('timetable')
              || document.getElementById('kbtable')
              || document.getElementById('kbgrid_table_0');

            if (table) {
              var tableCourses = parseTableByRows(table);
              if (tableCourses.length > 0) return tableCourses;
            }

            var listCourses = parseZhengfangList();
            if (listCourses.length > 0) return listCourses;

            return [];
          }

          var result = tryParse();
          if (result.length === 0) {
            AndroidBridge.showToast('未找到课程数据，请确认已登录并打开课表页面');
            return;
          }
          AndroidBridge.showToast('解析到 ' + result.length + ' 门课程');
          AndroidBridge.saveCourseData(JSON.stringify(result));
        })();
    """.trimIndent()
}
