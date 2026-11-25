import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('kb_token') || '',
    username: localStorage.getItem('kb_username') || '',
    displayName: localStorage.getItem('kb_display') || '',
    role: localStorage.getItem('kb_role') || '',
    personalSpaceId: Number(localStorage.getItem('kb_personal_space') || 0),
    spaces: []
  }),
  getters: {
    logged: s => !!s.token,
    isAdmin: s => s.role === 'ADMIN'
  },
  actions: {
    setLogin(d) {
      this.token = d.token
      this.username = d.username
      this.displayName = d.displayName
      this.role = d.role
      localStorage.setItem('kb_token', d.token)
      localStorage.setItem('kb_username', d.username)
      localStorage.setItem('kb_display', d.displayName || d.username)
      localStorage.setItem('kb_role', d.role)
    },
    async fetchMe(api) {
      const res = await api.get('/users/me')
      const d = res.data
      this.personalSpaceId = d.personalSpace?.id || 0
      this.spaces = d.spaces || []
      localStorage.setItem('kb_personal_space', String(this.personalSpaceId))
      return d
    },
    logout() {
      this.token = ''
      this.username = ''
      this.role = ''
      this.personalSpaceId = 0
      ;['kb_token', 'kb_username', 'kb_display', 'kb_role', 'kb_personal_space'].forEach(k => localStorage.removeItem(k))
    }
  }
})
