import assert from 'node:assert/strict'
import test from 'node:test'
import { renderSafeMarkdown } from '../src/utils/markdown.js'

test('escapes raw HTML from transcription or LLM output', () => {
  const result = renderSafeMarkdown('<img src=x onerror="alert(1)">')
  assert.equal(result.includes('<img'), false)
  assert.equal(result.includes('&lt;img'), true)
})

test('preserves supported Markdown formatting', () => {
  const result = renderSafeMarkdown('# 标题\n\n**重点**')
  assert.match(result, /<h1>标题<\/h1>/)
  assert.match(result, /<strong>重点<\/strong>/)
})
