package com.teamdev.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件解析控制器
 * 提供文件上传和解析统计功能
 */
@RestController
@RequestMapping("/api/file")
public class FileParseController {

    /**
     * 解析上传的文件，统计第二列去重数量和第三列总合计数
     * 数据格式：第一列 | 第二列 | 第三列|...
     *
     * @param file 上传的文件
     * @return 统计结果
     */
    @PostMapping("/parse")
    public ResponseEntity<FileParseResult> parseFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new FileParseResult(0, 0, "文件为空"));
            }

            Set<String> secondColumnSet = new HashSet<>();
            int thirdColumnSum = 0;
            int lineCount = 0;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }

                    String[] columns = line.split("\\|");
                    if (columns.length >= 3) {
                        // 第二列去重统计
                        String secondColumn = columns[1].trim();
                        if (!secondColumn.isEmpty()) {
                            secondColumnSet.add(secondColumn);
                        }

                        // 第三列求和
                        String thirdColumn = columns[2].trim();
                        if (!thirdColumn.isEmpty()) {
                            try {
                                thirdColumnSum += Integer.parseInt(thirdColumn);
                            } catch (NumberFormatException e) {
                                // 忽略无法解析为数字的第三列数据
                            }
                        }
                    }
                    lineCount++;
                }
            }

            FileParseResult result = new FileParseResult(
                secondColumnSet.size(),
                thirdColumnSum,
                "解析成功，共读取 " + lineCount + " 行数据"
            );
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new FileParseResult(0, 0, "文件读取失败：" + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new FileParseResult(0, 0, "解析失败：" + e.getMessage()));
        }
    }

    /**
     * 文件解析结果封装类
     */
    public static class FileParseResult {
        private int secondColumnDistinctCount;
        private int thirdColumnSum;
        private String message;

        public FileParseResult() {
        }

        public FileParseResult(int secondColumnDistinctCount, int thirdColumnSum, String message) {
            this.secondColumnDistinctCount = secondColumnDistinctCount;
            this.thirdColumnSum = thirdColumnSum;
            this.message = message;
        }

        public int getSecondColumnDistinctCount() {
            return secondColumnDistinctCount;
        }

        public void setSecondColumnDistinctCount(int secondColumnDistinctCount) {
            this.secondColumnDistinctCount = secondColumnDistinctCount;
        }

        public int getThirdColumnSum() {
            return thirdColumnSum;
        }

        public void setThirdColumnSum(int thirdColumnSum) {
            this.thirdColumnSum = thirdColumnSum;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
