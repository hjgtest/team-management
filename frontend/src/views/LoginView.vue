<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="login-title">开发组管理系统</h2>
      <p class="login-subtitle">请使用手机号和密码登录</p>

      <el-form ref="loginForm" :model="form" :rules="rules" label-width="0" class="login-form">
        <el-form-item prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="手机号（账号）"
            prefix-icon="el-icon-phone"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            prefix-icon="el-icon-lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>
    </div>
  </div>
</template>

<script>
import { authApi } from '@/api'
import { setToken, setUser } from '@/utils/auth'

export default {
  name: 'LoginView',
  data() {
    return {
      loading: false,
      errorMsg: '',
      form: { phone: '', password: '' },
      rules: {
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate(async (valid) => {
        if (!valid) return
        this.loading = true
        this.errorMsg = ''
        try {
          const res = await authApi.login(this.form.phone, this.form.password)
          if (res.code === 200) {
            setToken(res.data.token)
            setUser(res.data.user)
            this.$message.success('登录成功')
            const redirect = this.$route.query.redirect || '/dashboard'
            this.$router.push(redirect)
          } else {
            this.errorMsg = res.message || '登录失败'
          }
        } catch (err) {
          this.errorMsg = err.response?.data?.message || '登录失败，请检查网络'
        } finally {
          this.loading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
}
.login-card {
  width: 400px;
  padding: 40px 32px;
  background: rgba(30, 41, 59, 0.9);
  border: 1px solid rgba(51, 65, 85, 0.5);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}
.login-title {
  text-align: center;
  color: #e2e8f0;
  font-size: 24px;
  margin-bottom: 8px;
}
.login-subtitle {
  text-align: center;
  color: #94a3b8;
  font-size: 14px;
  margin-bottom: 32px;
}
.login-btn {
  width: 100%;
  margin-top: 8px;
}
.error-msg {
  color: #f87171;
  font-size: 13px;
  text-align: center;
  margin-top: 12px;
}
</style>
