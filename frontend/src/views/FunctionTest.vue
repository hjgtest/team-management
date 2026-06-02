<template>
  <div class="test-container">
    <h2>功能测试页面</h2>
    <el-button @click="testExport">测试导出功能</el-button>
    <el-button @click="testDialog">测试弹窗功能</el-button>
    
    <!-- 测试弹窗 -->
    <el-dialog v-model="dialogVisible" title="测试弹窗" width="60%">
      <div>这是一个测试弹窗</div>
    </el-dialog>
  </div>
</template>

<script>
import * as XLSX from 'xlsx'

export default {
  name: 'FunctionTest',
  data() {
    return {
      dialogVisible: false
    }
  },
  methods: {
    testExport() {
      console.log('测试导出功能')
      this.$message.success('导出功能测试')
      
      // 简单的Excel生成测试
      const wb = XLSX.utils.book_new()
      const ws = XLSX.utils.aoa_to_sheet([['测试数据']])
      XLSX.utils.book_append_sheet(wb, ws, '测试表')
      
      const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
      console.log('Excel生成成功，大小:', wbout.length)
    },
    
    testDialog() {
      console.log('测试弹窗功能')
      this.dialogVisible = true
      this.$message.success('弹窗功能测试')
    }
  }
}
</script>

<style scoped>
.test-container {
  padding: 20px;
}
</style>