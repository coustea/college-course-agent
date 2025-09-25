const STORAGE_KEY = 'learning_progress_v1'

function readStore() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
  } catch {
    return {}
  }
}

function writeStore(store) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(store))
}

export function getVideoProgress(courseId, chapterIndex) {
  const store = readStore()
  return store?.[courseId]?.videos?.[chapterIndex] ?? 0
}

export function setVideoProgress(courseId, chapterIndex, progress) {
  const store = readStore()
  if (!store[courseId]) store[courseId] = { videos: {}, overall: 0, lastUpdated: 0 }
  store[courseId].videos[chapterIndex] = Math.max(0, Math.min(1, progress))
  const vals = Object.values(store[courseId].videos)
  const overall = vals.length ? vals.reduce((a, b) => a + b, 0) / vals.length : 0
  store[courseId].overall = overall
  store[courseId].lastUpdated = Date.now()
  writeStore(store)
  return overall
}

export function getOverallProgress(courseId) {
  const store = readStore()
  return store?.[courseId]?.overall ?? 0
}

export function getAllCoursesSummary() {
  const store = readStore()
  return Object.entries(store).map(([courseId, v]) => ({
    courseId: isNaN(Number(courseId)) ? courseId : Number(courseId),
    overall: v?.overall ?? 0,
    lastUpdated: v?.lastUpdated ?? 0
  }))
}

export function resetCourseProgress(courseId) {
  const store = readStore()
  delete store[courseId]
  writeStore(store)
}


