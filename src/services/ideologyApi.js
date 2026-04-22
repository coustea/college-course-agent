import request from '@/utils/request'

export const searchIdeologyResources = (params = {}) =>
  request.get('/api/ideology/resources', { params })

export const createIdeologyResource = (payload) =>
  request.post('/api/ideology/resources', payload)

export const updateIdeologyResource = (resourceId, payload) =>
  request.put(`/api/ideology/resources/${resourceId}`, payload)

export const deleteIdeologyResource = (resourceId) =>
  request.delete(`/api/ideology/resources/${resourceId}`)

export const analyzeIdeologyResource = (payload) =>
  request.post('/api/ideology/resources/analyze', payload)

export const listIdeologyResourceTags = (resourceId) =>
  request.get(`/api/ideology/resources/${resourceId}/tags`)

export const getIdeologyRecommendations = (params = {}) =>
  request.get('/api/ideology/resources/recommendations', { params })

export const refreshIdeologyRecommendations = (params = {}) =>
  request.post('/api/ideology/resources/recommendations/refresh', null, { params })

export const markIdeologyRecommendationClicked = (id) =>
  request.post(`/api/ideology/resources/recommendations/${id}/click`)
