/**
 * Copy strings may contain Markdown-style `inline code`. It is expanded here, at build
 * time, into <code> elements — the alternative would be a client-side Markdown
 * renderer for six backticks per page, which this site does not ship.
 */

const ESCAPES: Record<string, string> = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;',
};

export function escapeHtml(value: string): string {
  return value.replace(/[&<>"']/g, (ch) => ESCAPES[ch]);
}

/** Escape, then turn `x` into <code>x</code>. Safe to pass to set:html. */
export function inlineCode(value: string): string {
  return escapeHtml(value).replace(/`([^`]+)`/g, '<code>$1</code>');
}

/** Same string with the backticks removed — for JSON-LD, meta tags and alt text. */
export function plainText(value: string): string {
  return value.replace(/`([^`]+)`/g, '$1');
}
