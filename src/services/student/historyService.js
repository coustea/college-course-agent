// History service: store and retrieve recent learning records
// Record shape: { id, type: 'video'|'static'|'document', title, image, duration, watchedAt, progress?, timeSpent? }

const STORAGE_KEY = 'learning_history_v1'

function readStore() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]') } catch { return [] }
}

function writeStore(list) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(list))
try { window.dispatchEvent(new CustomEvent('learning-history-updated')) } catch (e) { console.error(e) }
}

export function getHistory() {
  const list = readStore()
  const uniqueList = []
  const seenKeys = new Set()
  for (const item of list) {
    const key = `${item.title}-${item.type}`
    if (!seenKeys.has(key)) {
      seenKeys.add(key)
      uniqueList.push(item)
    }
  }
  return uniqueList
}

export function clearHistory() {
  writeStore([])
}

export function addOrUpdateRecord(record) {
  const list = readStore()

  // 首先尝试通过id匹配
  let idx = list.findIndex(r => String(r.id) === String(record.id))

  // 如果没找到，尝试通过title匹配（处理id格式不一致的情况）
  if (idx < 0) {
    idx = list.findIndex(r => r.title === record.title && r.type === record.type)
  }

  const existing = idx >= 0 ? list[idx] : {}
  const merged = { ...existing, ...record }
  merged.watchedAt = record.watchedAt || new Date().toISOString()
  if (typeof merged.timeSpent !== 'number') merged.timeSpent = existing.timeSpent || 0

  if (idx >= 0) {
    // 更新现有记录
    list[idx] = merged
  } else {
    // 添加新记录
    list.push(merged)
  }

  // 去重：通过title和type组合去重，确保每个课程只有一条记录
  const uniqueList = []
  const seenKeys = new Set()
  for (const item of list) {
    const key = `${item.title}-${item.type}`
    if (!seenKeys.has(key)) {
      seenKeys.add(key)
      uniqueList.push(item)
    }
  }

  // sort by watchedAt desc
  uniqueList.sort((a, b) => new Date(b.watchedAt) - new Date(a.watchedAt))
  writeStore(uniqueList)
  return merged
}

export function updateProgress(id, progress) {
  const list = readStore()
  const idx = list.findIndex(r => String(r.id) === String(id))
  if (idx >= 0) {
    list[idx].progress = progress
    list[idx].watchedAt = new Date().toISOString()
    writeStore(list)
  }
}

export function addTime(id, seconds) {
  if (!seconds || seconds <= 0) return
  const list = readStore()
  const idx = list.findIndex(r => String(r.id) === String(id))
  if (idx >= 0) {
    list[idx].timeSpent = (list[idx].timeSpent || 0) + seconds
    list[idx].watchedAt = new Date().toISOString()
    writeStore(list)
  }
}

export function onHistoryChanged(handler) {
  window.addEventListener('learning-history-updated', handler)
  return () => window.removeEventListener('learning-history-updated', handler)
}
