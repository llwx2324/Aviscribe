import MarkdownIt from 'markdown-it'

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true
})

export function renderSafeMarkdown(source) {
  return markdown.render(source || '')
}
