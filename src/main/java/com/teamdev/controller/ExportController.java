package com.teamdev.controller;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.teamdev.entity.Project;
import com.teamdev.entity.ProjectVersion;
import com.teamdev.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 导出控制器
 * 提供Excel导出功能
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private com.teamdev.service.UserService userService;

    /**
     * 导出任务数据为Excel文件（项目→版本→负责人）
     *
     * @return ResponseEntity<byte[]> 包含Excel文件的响应
     */
    @GetMapping("/tasks")
    public ResponseEntity<byte[]> exportTasks() {
        try {
            // 获取所有项目和版本数据
            List<Map<String, Object>> exportData = projectService.getAllProjectsAndVersions();
            
            // 获取当前日期
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1); // 周一
            LocalDate weekEnd = weekStart.plusDays(6); // 周日
            LocalDate monthStart = today.withDayOfMonth(1); // 当月第一天
            LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth()); // 当月最后一天
            LocalDate yearStart = today.withDayOfYear(1); // 当年第一天
            LocalDate yearEnd = today.withDayOfYear(today.lengthOfYear()); // 当年最后一天
            
            // 构建导出数据列表 - 分为今天任务、周内进行中任务、当月任务、本年任务
            List<TaskExportData> todayTasks = new ArrayList<>();
            List<TaskExportData> weekTasks = new ArrayList<>();
            List<TaskExportData> monthCompletedTasks = new ArrayList<>();
            List<TaskExportData> yearCompletedTasks = new ArrayList<>();
            
            // 安全检查
            if (exportData == null) {
                exportData = new ArrayList<>();
            }
            
            for (Map<String, Object> project : exportData) {
                if (project == null) continue;
                
                String projectName = (String) project.get("projectName");
                if (projectName == null || projectName.trim().isEmpty()) {
                    projectName = "未知项目";
                }
                
                List<Map<String, Object>> versions = (List<Map<String, Object>>) project.get("versions");
                if (versions == null || versions.isEmpty()) {
                    continue;
                }
                
                for (Map<String, Object> version : versions) {
                    if (version == null) continue;
                    
                    String versionNumber = (String) version.get("versionNumber");
                    if (versionNumber == null || versionNumber.trim().isEmpty()) {
                        versionNumber = "未知任务";
                    }
                    
                    List<String> userNames = (List<String>) version.get("userNames");
                    String responsibleUsers = userNames != null && !userNames.isEmpty() ? 
                        String.join(", ", userNames) : "未知负责人";
                    
                    // 解析版本时间
                    LocalDateTime startTime = null;
                    Object startTimeObj = version.get("startTime");
                    if (startTimeObj instanceof String) {
                        try {
                            startTime = LocalDateTime.parse((String) startTimeObj);
                        } catch (Exception e) {
                        }
                    } else if (startTimeObj instanceof LocalDateTime) {
                        startTime = (LocalDateTime) startTimeObj;
                    }

                    LocalDateTime endTime = null;
                    Object endTimeObj = version.get("endTime");
                    if (endTimeObj instanceof String) {
                        try {
                            endTime = LocalDateTime.parse((String) endTimeObj);
                        } catch (Exception e) {
                            // 忽略解析错误
                        }
                    } else if (endTimeObj instanceof LocalDateTime) {
                        endTime = (LocalDateTime) endTimeObj;
                    }
                    
                    TaskExportData data = new TaskExportData();
                    data.setProjectName(projectName);
                    data.setTask(versionNumber);
                    data.setResponsibleUsers(responsibleUsers);
                    
                    // 设置时间格式化显示
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                    if (startTime != null) {
                        data.setStartTime(startTime.format(formatter));
                    }
                    if (endTime != null) {
                        data.setEndTime(endTime.format(formatter));
                    }
                    
                    // 1. 今日任务：维持原逻辑，即结束日期为今天的任务
                    if (endTime != null && endTime.toLocalDate().equals(today)) {
                        todayTasks.add(data);
                    }
                    
                    // 2. 周内进行中任务：开始时间或结束时间落在本周内（周一至周日）
                    LocalDate startDay = (startTime != null) ? startTime.toLocalDate() : null;
                    LocalDate endDay = (endTime != null) ? endTime.toLocalDate() : null;
                    
                    boolean inThisWeek = false;
                    // 开始时间在本周内
                    if (startDay != null && !startDay.isBefore(weekStart) && !startDay.isAfter(weekEnd)) {
                        inThisWeek = true;
                    } 
                    // 或者结束时间在本周内
                    else if (endDay != null && !endDay.isBefore(weekStart) && !endDay.isAfter(weekEnd)) {
                        inThisWeek = true;
                    }
                    // 或者任务跨越了整个本周
                    else if (startDay != null && endDay != null && startDay.isBefore(weekStart) && endDay.isAfter(weekEnd)) {
                        inThisWeek = true;
                    }
                    
                    if (inThisWeek) {
                        weekTasks.add(data);
                    }
                    
                    // 3. 当月任务：结束时间在当月的任务
                    if (endDay != null && !endDay.isBefore(monthStart) && !endDay.isAfter(monthEnd)) {
                        monthCompletedTasks.add(data);
                    }
                    
                    // 4. 本年任务：结束时间在本年的任务
                    if (endDay != null && !endDay.isBefore(yearStart) && !endDay.isAfter(yearEnd)) {
                        yearCompletedTasks.add(data);
                    }
                }
            }
            
            // 如果都没有数据，创建测试数据
            if (todayTasks.isEmpty() && weekTasks.isEmpty() && monthCompletedTasks.isEmpty() && yearCompletedTasks.isEmpty()) {
                TaskExportData testData = new TaskExportData();
                testData.setProjectName("测试项目");
                testData.setTask("测试任务");
                testData.setResponsibleUsers("测试用户");
                todayTasks.add(testData);
            }
            
            // 使用ByteArrayOutputStream收集Excel数据
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ExcelWriter excelWriter = null;
            
            try {
                // 创建Excel写入器
                excelWriter = EasyExcel.write(outputStream, TaskExportData.class)
                        .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                        .build();
                
                // 写入第一个sheet：今日任务
                WriteSheet sheet1 = EasyExcel.writerSheet(0, "今日任务").build();
                excelWriter.write(todayTasks, sheet1);
                
                // 写入第二个sheet：周内进行中任务
                if (!weekTasks.isEmpty()) {
                    WriteSheet sheet2 = EasyExcel.writerSheet(1, "周内进行中任务").build();
                    excelWriter.write(weekTasks, sheet2);
                } else {
                    // 如果本周没有任务，也要写入一个空工作表
                    List<TaskExportData> emptyList = new ArrayList<>();
                    WriteSheet sheet2 = EasyExcel.writerSheet(1, "周内进行中任务").build();
                    excelWriter.write(emptyList, sheet2);
                }
                
                // 写入第三个sheet：当月任务
                WriteSheet sheet3 = EasyExcel.writerSheet(2, "当月任务").build();
                excelWriter.write(monthCompletedTasks, sheet3);
                
                // 写入第四个sheet：本年任务
                WriteSheet sheet4 = EasyExcel.writerSheet(3, "本年任务").build();
                excelWriter.write(yearCompletedTasks, sheet4);
            } finally {
                // 确保写入器被正确关闭
                if (excelWriter != null) {
                    excelWriter.finish();
                }
            }
            
            byte[] excelBytes = outputStream.toByteArray();
            
            // 设置响应头 - 修复文件打开问题
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            headers.add("Content-Length", String.valueOf(excelBytes.length));
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
            
            String fileName = URLEncoder.encode("任务安排_" + LocalDate.now(), "UTF-8").replaceAll("\\+", "%20");
            headers.add("Content-Disposition", "attachment; filename=\"" + fileName + ".xlsx\"");
            
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
                
        } catch (Exception e) {
            // 记录详细错误信息
            e.printStackTrace();
            throw new RuntimeException("导出 Excel 文件失败：" + e.getMessage(), e);
        }
    }
        
    /**
     * 导出按人统计的任务数据为 Excel 文件（人→项目→时间）
     * 统计每个人参与了哪些项目，以及项目的开始和结束时间
     *
     * @return ResponseEntity<byte[]> 包含 Excel 文件的响应
     */
    @GetMapping("/tasks-by-user")
    public ResponseEntity<byte[]> exportTasksByUser() {
        try {
            // 获取所有在职用户
            List<com.teamdev.entity.User> users = userService.getActiveUsers();

            // 构建导出数据列表
            List<UserTaskExportData> allUserTasks = new ArrayList<>();

            // 遍历每个用户，获取其参与的项目
            for (com.teamdev.entity.User user : users) {
                // 获取用户参与的所有项目分配
                List<Map<String, Object>> userProjectsAndVersions = projectService.getUserProjectsAndVersions(user.getId());

                if (userProjectsAndVersions.isEmpty()) {
                    // 如果用户没有参与任何项目，添加一条空记录
                    UserTaskExportData data = new UserTaskExportData();
                    data.setUsername(user.getUsername());
                    data.setProjectName("无");
                    data.setTask("无");
                    data.setStartTime("-");
                    data.setEndTime("-");
                    allUserTasks.add(data);
                    continue;
                }

                // 遍历用户参与的每个项目
                for (Map<String, Object> project : userProjectsAndVersions) {
                    String projectName = (String) project.get("projectName");
                    List<Map<String, Object>> versions = (List<Map<String, Object>>) project.get("versions");

                    if (versions == null || versions.isEmpty()) {
                        // 如果项目没有版本，添加项目信息但任务为空
                        UserTaskExportData data = new UserTaskExportData();
                        data.setUsername(user.getUsername());
                        data.setProjectName(projectName != null ? projectName : "未知项目");
                        data.setTask("无具体任务");

                        // 设置项目时间
                        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                        Object projectStartTime = project.get("startTime");
                        Object projectEndTime = project.get("endTime");

                        if (projectStartTime instanceof LocalDateTime) {
                            data.setStartTime(((LocalDateTime) projectStartTime).format(formatter));
                        } else if (projectStartTime instanceof String) {
                            try {
                                data.setStartTime(LocalDateTime.parse((String) projectStartTime).format(formatter));
                            } catch (Exception e) {
                                data.setStartTime("未设置");
                            }
                        } else {
                            data.setStartTime("未设置");
                        }

                        if (projectEndTime instanceof LocalDateTime) {
                            data.setEndTime(((LocalDateTime) projectEndTime).format(formatter));
                        } else if (projectEndTime instanceof String) {
                            try {
                                data.setEndTime(LocalDateTime.parse((String) projectEndTime).format(formatter));
                            } catch (Exception e) {
                                data.setEndTime("未设置");
                            }
                        } else {
                            data.setEndTime("未设置");
                        }

                        allUserTasks.add(data);
                        continue;
                    }

                    // 遍历每个版本
                    for (Map<String, Object> version : versions) {
                        // 解析结束时间对象（保持为 Object类型以便类型检查）
                        Object endTimeObj = version.get("endTime");
                        
                        // 检查结束时间，如果结束时间小于当天，则跳过不导出
                        LocalDateTime versionEndTime = null;
                        if (endTimeObj instanceof LocalDateTime) {
                            versionEndTime = (LocalDateTime) endTimeObj;
                        } else if (endTimeObj instanceof String) {
                            try {
                                versionEndTime = LocalDateTime.parse((String) endTimeObj);
                            } catch (Exception e) {
                                // 忽略解析错误
                            }
                        }
                        
                        // 如果结束时间存在且小于当天，跳过此任务
                        if (versionEndTime != null && versionEndTime.toLocalDate().isBefore(LocalDate.now())) {
                            continue;
                        }
                        
                        UserTaskExportData data = new UserTaskExportData();
                        data.setUsername(user.getUsername());
                        data.setProjectName(projectName != null ? projectName : "未知项目");

                        String versionNumber = (String) version.get("versionNumber");
                        data.setTask(versionNumber != null ? versionNumber : "未知任务");

                        // 设置时间格式化显示
                        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                        Object startTimeObj = version.get("startTime");

                        if (startTimeObj instanceof LocalDateTime) {
                            data.setStartTime(((LocalDateTime) startTimeObj).format(formatter));
                        } else if (startTimeObj instanceof String) {
                            try {
                                data.setStartTime(LocalDateTime.parse((String) startTimeObj).format(formatter));
                            } catch (Exception e) {
                                data.setStartTime("未设置");
                            }
                        } else {
                            data.setStartTime("未设置");
                        }

                        // 设置结束时间
                        if (endTimeObj instanceof LocalDateTime) {
                            data.setEndTime(((LocalDateTime) endTimeObj).format(formatter));
                        } else if (endTimeObj instanceof String) {
                            try {
                                data.setEndTime(LocalDateTime.parse((String) endTimeObj).format(formatter));
                            } catch (Exception e) {
                                data.setEndTime("未设置");
                            }
                        } else {
                            data.setEndTime("未设置");
                        }

                        allUserTasks.add(data);
                    }
                }
            }

            // 如果没有数据，创建测试数据
            if (allUserTasks.isEmpty()) {
                UserTaskExportData testData = new UserTaskExportData();
                testData.setUsername("测试用户");
                testData.setProjectName("测试项目");
                testData.setTask("测试任务");
                testData.setStartTime("2026-03-01 09:00");
                testData.setEndTime("2026-03-10 18:00");
                allUserTasks.add(testData);
            }

            // 使用 ByteArrayOutputStream 收集 Excel 数据
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try {
                // 创建 Excel 写入器
                EasyExcel.write(outputStream, UserTaskExportData.class)
                        .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                        .sheet("人员任务统计")
                        .doWrite(allUserTasks);

                // 现在 outputStream 中已经有 Excel 数据，使用 POI 打开并合并单元格
                ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(inputStream);
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);

                // 合并相同用户名的单元格
                mergeUsernamesInSheet(sheet, allUserTasks);

                // 重新写入 outputStream
                ByteArrayOutputStream finalOutputStream = new ByteArrayOutputStream();
                workbook.write(finalOutputStream);
                workbook.close();
                byte[] excelBytes = finalOutputStream.toByteArray();
                inputStream.close();

                // 设置响应头
                HttpHeaders headers = new HttpHeaders();
                headers.add("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                headers.add("Content-Length", String.valueOf(excelBytes.length));
                headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
                headers.add("Pragma", "no-cache");
                headers.add("Expires", "0");

                String fileName = URLEncoder.encode("人员任务统计_" + LocalDate.now(), "UTF-8").replaceAll("\\+", "%20");
                headers.add("Content-Disposition", "attachment; filename=\"" + fileName + ".xlsx\"");

                return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

            } catch (Exception e) {
                // 记录详细错误信息
                e.printStackTrace();
                throw new RuntimeException("导出 Excel 文件失败：" + e.getMessage(), e);
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 导出数据模型 - 字段名已优化并添加注解
     */
    public static class TaskExportData {
        @ExcelProperty("项目")
        private String projectName;
        
        @ExcelProperty("任务")
        private String task;
        
        @ExcelProperty("负责人")
        private String responsibleUsers;
        
        @ExcelProperty("开始时间")
        private String startTime;
        
        @ExcelProperty("结束时间")
        private String endTime;
        
        // Getters and Setters
        public String getProjectName() {
            return projectName;
        }
        
        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }
        
        public String getTask() {
            return task;
        }
        
        public void setTask(String task) {
            this.task = task;
        }
        
        public String getResponsibleUsers() {
            return responsibleUsers;
        }
        
        public void setResponsibleUsers(String responsibleUsers) {
            this.responsibleUsers = responsibleUsers;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }
    }
    
    /**
     * 按人统计的导出数据模型
     */
    public static class UserTaskExportData {
        @ExcelProperty("用户名")
        private String username;
        
        @ExcelProperty("项目名称")
        private String projectName;
        
        @ExcelProperty("任务")
        private String task;
        
        @ExcelProperty("开始时间")
        private String startTime;
        
        @ExcelProperty("结束时间")
        private String endTime;
        
        // Getters and Setters
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getProjectName() {
            return projectName;
        }
        
        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }
        
        public String getTask() {
            return task;
        }
        
        public void setTask(String task) {
            this.task = task;
        }
        
        public String getStartTime() {
            return startTime;
        }
        
        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }
        
        public String getEndTime() {
            return endTime;
        }
        
        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }
    }
    
    /**
     * 手动合并相同用户名的单元格（使用 POI API）
     */
    private void mergeUsernamesInSheet(org.apache.poi.ss.usermodel.Sheet sheet, List<UserTaskExportData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }
        
        int rowIndex = 1; // 从第 1 行开始（第 0 行是表头）
        String lastUsername = null;
        int startIndex = 0;
        int mergeCount = 0;
        
        for (UserTaskExportData data : dataList) {
            String currentUsername = data.getUsername();
            
            if (lastUsername == null) {
                lastUsername = currentUsername;
                startIndex = rowIndex;
                mergeCount = 1;
            } else if (lastUsername.equals(currentUsername)) {
                mergeCount++;
            } else {
                if (mergeCount > 1) {
                    org.apache.poi.ss.util.CellRangeAddress range = new org.apache.poi.ss.util.CellRangeAddress(
                        startIndex, startIndex + mergeCount - 1, 0, 0);
                    sheet.addMergedRegion(range);
                }
                lastUsername = currentUsername;
                startIndex = rowIndex;
                mergeCount = 1;
            }
            rowIndex++;
        }
        
        // 处理最后一组
        if (mergeCount > 1) {
            org.apache.poi.ss.util.CellRangeAddress range = new org.apache.poi.ss.util.CellRangeAddress(
                startIndex, startIndex + mergeCount - 1, 0, 0);
            sheet.addMergedRegion(range);
        }
    }
}