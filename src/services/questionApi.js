// 简易问题接口与本地缓存
import axios from 'axios'

//防止出现重复题目
const LS_KEY = 'course_questions_state_v1'
function readLS() {
    try { return JSON.parse(localStorage.getItem(LS_KEY) || '{}') } catch { return {} }
}

function writeLS(state) {
    localStorage.setItem(LS_KEY, JSON.stringify(state))
}

function setExamId(courseId, nodeKey, examId) {
    if (!courseId || !nodeKey || !examId) return
    const s = readLS()
    if (!s[courseId]) s[courseId] = {}
    const prev = s[courseId][nodeKey] || {}
    s[courseId][nodeKey] = { ...prev, examId }
    writeLS(s)
}

function getExamId(courseId, nodeKey) {
    const s = readLS()
    return s?.[courseId]?.[nodeKey]?.examId
}

// 避免重复
export function markQuestionShown(courseId, nodeKey) {
    const s = readLS()
    if (!s[courseId]) s[courseId] = {}
    s[courseId][nodeKey] = { shown: true, ts: Date.now() }
    writeLS(s)
}

export function hasQuestionShown(courseId, nodeKey) {
    const s = readLS()
    return !!s?.[courseId]?.[nodeKey]?.shown
}

// ===== 扩展：按题目ID跟踪“已展示过”的题，确保最多两次且不重复 =====
function getShownIds(courseId, nodeKey) {
    const s = readLS()
    const rec = s?.[courseId]?.[nodeKey]
    const arr = Array.isArray(rec?.shownIds) ? rec.shownIds : []
    return arr
}

function addShownId(courseId, nodeKey, qid) {
    if (!courseId || !nodeKey || qid == null) return
    const s = readLS()
    if (!s[courseId]) s[courseId] = {}
    const prev = s[courseId][nodeKey] || {}
    const arr = Array.isArray(prev.shownIds) ? prev.shownIds.slice() : []
    if (!arr.includes(qid)) arr.push(qid)
    s[courseId][nodeKey] = { ...prev, shownIds: arr }
    writeLS(s)
}

// ==== 新增：调用后端 AI 生成试题 ====
/**
 * 调用后端生成题目
 * @param {object} payload { courseId:number, studentId:number, choiceCount:number, judgeCount:number }
 * @returns {Promise<Array>} 标准化题目数组
 */
export async function generateQuestions(payload) {
    const url = `http://192.168.52.75:9999/api/aiexam/generate`
    const body = {
        courseId: payload?.courseId,
        studentId: payload?.studentId,
        choiceCount: Math.max(0, Number(payload?.choiceCount || 0)),
        judgeCount: Math.max(0, Number(payload?.judgeCount || 0))
    }
    try {
        const res = await axios.post(url, body)
        const raw = res?.data
        // 兼容多种后端返回：
        // 1) 直接数组
        // 2) { data: [...] }
        // 3) { code, message, data: { exam: {...}, questions: [...] } }
        const list = Array.isArray(raw)
            ? raw
            : (Array.isArray(raw?.data?.questions)
                ? raw.data.questions
                : (Array.isArray(raw?.data)
                    ? raw.data
                    : []))
        return normalizeBackendQuestions(list)
    } catch (e) {
        console.warn('生成题目失败，使用本地占位', e)
        return []
    }
}

// 返回 exam 与标准化题目
export async function generateExamAndQuestions(payload) {
    const url = `http://192.168.52.75:9999/api/aiexam/generate`
    const body = {
        courseId: payload?.courseId,
        studentId: payload?.studentId,
        choiceCount: Math.max(0, Number(payload?.choiceCount || 0)),
        judgeCount: Math.max(0, Number(payload?.judgeCount || 0))
    }
    try {
        const res = await axios.post(url, body)
        const raw = res?.data
        const exam = raw?.data?.exam || null
        const questionsRaw = Array.isArray(raw)
            ? raw
            : (Array.isArray(raw?.data?.questions)
                ? raw.data.questions
                : (Array.isArray(raw?.data)
                    ? raw.data
                    : []))
        const questions = normalizeBackendQuestions(questionsRaw)
        return { exam, questions }
    } catch (e) {
        console.warn('生成试卷失败', e)
        return { exam: null, questions: [] }
    }
}

/**
 * 将后端题目结构映射为 Question 组件可用结构
 * 支持两类：选择题(choice/单选/多选)、判断题(judge/true_false)
 */
function normalizeBackendQuestions(arr) {
    if (!Array.isArray(arr)) return []
    let idx = 0
    return arr.map((q) => {
        idx += 1
        const typeRaw = (q.type || q.kind || '').toLowerCase()
        const isJudge = typeRaw.includes('judge') || typeRaw.includes('tf') || typeRaw.includes('true') || typeRaw.includes('判断')
        const isMultiple = typeRaw.includes('multiple') || typeRaw.includes('multi') || typeRaw.includes('多选')
        const isSingle = typeRaw.includes('single') || typeRaw.includes('单选') || (!isJudge && !isMultiple)

        // 题干（兼容后端 content 字段）
        const stem = q.stem || q.question || q.content || q.title || '请回答以下问题'

        // 选项（判断题没有选项则自动生成“正确/错误”）
        let options = Array.isArray(q.options) ? q.options : (Array.isArray(q.choices) ? q.choices : null)
        if (isJudge) {
            options = [
                { value: true, label: '正确' },
                { value: false, label: '错误' }
            ]
        } else if (Array.isArray(options)) {
            options = options.map((opt, i) => {
                const label = opt?.label ?? opt?.text ?? opt?.content ?? String(opt)
                const value = opt?.value ?? opt?.key ?? String(i)
                return { value, label }
            })
        } else {
            options = []
        }

        // 答案
        let answer = q.answer
        if (answer == null && q.correct != null) answer = q.correct
        if (isJudge) {
            // 后端可能返回 '正确'/'错误' 或 true/false
            if (typeof answer === 'string') {
                const s = answer.trim()
                answer = (s === '正确' || s.toLowerCase() === 'true' || s === '1')
            } else {
                answer = !!answer
            }
        }

        return {
            id: q.id ?? q.questionId ?? `q-${Date.now()}-${idx}`,
            type: isMultiple ? 'multiple' : (isSingle ? 'single' : (isJudge ? 'single' : 'single')),
            stem,
            options,
            answer
        }
    })
}

// 获取问题（若后端无，则返回占位问题）
export async function fetchQuestions(courseId, nodeKey) {
    try {
        // 临时: 学生ID来源。若本地存有 studentId 则使用；否则回退 18。
        let studentId = 18
        try {
            const saved = localStorage.getItem('currentStudentId')
            if (saved != null) {
                const n = Number(saved)
                if (Number.isFinite(n) && n > 0) studentId = n
            }
        } catch {}

        // 基于节点类型设置默认题量：视频/文档半程触发给 2 道判断题
        const isDocEnd = nodeKey === 'document-end'
        const choiceCount = 0
        const judgeCount = isDocEnd ? 2 : 2

        const { exam, questions } = await generateExamAndQuestions({ courseId, studentId, choiceCount, judgeCount })
        if (exam?.id) setExamId(courseId, nodeKey, exam.id)
        if (Array.isArray(questions) && questions.length) {
            // 仅返回“下一道未展示过的题”，最多两次
            const shown = getShownIds(courseId, nodeKey)
            if (shown.length >= 2) return []
            const next = questions.find(q => !shown.includes(q.id)) || null
            if (next) {
                addShownId(courseId, nodeKey, next.id)
                return [next]
            }
            return []
        }
    } catch {}
    // 本地占位
    {
        const local = [
            {
                id: `${courseId}-${nodeKey}-q1`,
                type: 'single',
                stem: '中国特色社会主义进入新时代始于哪一年？',
                options: [
                    { value: '2012', label: '2012年' },
                    { value: '2017', label: '2017年' },
                    { value: '2021', label: '2021年' }
                ],
                answer: '2012'
            },
            {
                id: `${courseId}-${nodeKey}-q2`,
                type: 'multiple',
                stem: '以下哪些属于社会主义核心价值观的国家层面价值目标？（多选）',
                options: [
                    { value: 'fq', label: '富强' },
                    { value: 'mz', label: '民主' },
                    { value: 'zy', label: '自由' },
                    { value: 'aq', label: '安全' }
                ],
                answer: ['fq', 'mz']
            }
        ]
        const shown = getShownIds(courseId, nodeKey)
        if (shown.length >= 2) return []
        const next = local.find(q => !shown.includes(q.id)) || null
        if (next) {
            addShownId(courseId, nodeKey, next.id)
            return [next]
        }
        return []
    }
}

// 批量提交答案，符合后端所需数据结构
export async function submitExamAnswers(courseId, nodeKey, questions, answersMap) {
    // 学生ID
    let studentId = 18
    try {
        const saved = localStorage.getItem('currentStudentId')
        if (saved != null) {
            const n = Number(saved)
            if (Number.isFinite(n) && n > 0) studentId = n
        }
    } catch {}

    const examId = getExamId(courseId, nodeKey)

    // 构造 answers 数组
    const ids = Object.keys(answersMap || {})
    const answers = ids.map((qid) => {
        let v = answersMap[qid]
        if (Array.isArray(v)) v = v.join(',')
        if (typeof v === 'boolean') v = v ? 'true' : 'false'
        return { questionId: isNaN(qid) ? qid : Number(qid), answer: String(v) }
    })

    // 若缺少 examId，则尽力回退为逐题上报（兼容旧逻辑）
    if (!examId) {
        try {
            for (const a of answers) {
                await axios.post(`${API}/api/aiexam/submit`, {
                    node: nodeKey,
                    questionId: a.questionId,
                    answer: a.answer
                })
            }
            return { answers, attempt: null }
        } catch (e) {
            console.warn('逐题回退上报失败', e)
            return { answers: [], attempt: null }
        }
    }

    // 按后端新格式批量提交
    try {
        const res = await axios.post(`${API}/api/aiexam/submit`, {
            examId,
            studentId,
            answers
        })
        const raw = res?.data
        const data = raw?.data || {}
        return {
            answers: Array.isArray(data.answers) ? data.answers : answers,
            attempt: data.attempt || null
        }
    } catch (e) {
        console.warn('批量提交失败', e)
        return { answers: [], attempt: null }
    }
}
