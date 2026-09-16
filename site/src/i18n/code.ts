/**
 * Build-time syntax colouring.
 *
 * Every code sample on this site is tokenised here, during `astro build`, and rendered
 * as plain <span> elements. There is no Prism/Shiki runtime, no client JavaScript and
 * no highlighter dependency — the pages ship the spans already coloured.
 *
 * Token classes (styled in styles/global.css):
 *   c  comment        hh heading hashes   ht heading text
 *   k  keyword        s  string           u  url / value
 *   v  http variable  sp ### separator    p  plain text
 */

export type Tok = { c: string; t: string };
export type Line = { toks: Tok[]; active?: boolean };

/** `-- ### Heading` → comment marker, hashes, heading text. */
function headingToks(prefix: string, rest: string): Tok[] {
  const m = /^(\s*)(#{1,6})(\s+)(\S.*)$/.exec(rest);
  if (!m) return rest ? [{ c: 'c', t: prefix + rest }] : [{ c: 'c', t: prefix }];
  return [
    { c: 'c', t: prefix + m[1] },
    { c: 'hh', t: m[2] + m[3] },
    { c: 'ht', t: m[4] },
  ];
}

const SQL_KEYWORD =
  /\b(SELECT|FROM|WHERE|AS|INSERT|INTO|VALUES|UPDATE|SET|DELETE|JOIN|LEFT|INNER|ON|ORDER|GROUP|BY|LIMIT|AND|OR|NOT|NULL|CREATE|TABLE|ALTER|DROP)\b/;

/** SQL statement text: single-quoted strings and a small keyword set. */
function sqlCodeToks(text: string): Tok[] {
  const toks: Tok[] = [];
  let rest = text;
  while (rest.length > 0) {
    const str = /^'[^']*'/.exec(rest);
    if (str) {
      toks.push({ c: 's', t: str[0] });
      rest = rest.slice(str[0].length);
      continue;
    }
    const word = /^[A-Za-z_][A-Za-z0-9_]*/.exec(rest);
    if (word) {
      toks.push({ c: SQL_KEYWORD.test(word[0].toUpperCase()) ? 'k' : 'p', t: word[0] });
      rest = rest.slice(word[0].length);
      continue;
    }
    const other = /^[^'A-Za-z_]+/.exec(rest);
    toks.push({ c: 'p', t: other ? other[0] : rest[0] });
    rest = rest.slice(other ? other[0].length : 1);
  }
  return toks;
}

function sqlToks(line: string): Tok[] {
  if (line.trim() === '') return [];
  const lineComment = /^(\s*--)(.*)$/.exec(line);
  if (lineComment) return headingToks(lineComment[1], lineComment[2]);
  // Block comment: /* … */ and its continuation lines, which may carry headings.
  const blockStar = /^(\s*\*\/?)(.*)$/.exec(line);
  if (blockStar) return headingToks(blockStar[1], blockStar[2]);
  if (/^\s*\/\*/.test(line)) return [{ c: 'c', t: line }];
  return sqlCodeToks(line);
}

function httpToks(line: string): Tok[] {
  if (line.trim() === '') return [];
  // The HTTP Client's own request separator, e.g. `### Replenishment detail`.
  const sep = /^(###)(\s*)(.*)$/.exec(line);
  if (sep) {
    const toks: Tok[] = [{ c: 'sp', t: sep[1] }];
    if (sep[3]) toks.push({ c: 'ht', t: sep[2] + sep[3] });
    return toks;
  }
  const slash = /^(\s*\/\/)(.*)$/.exec(line);
  if (slash) return headingToks(slash[1], slash[2]);
  const hash = /^(\s*#)(.*)$/.exec(line);
  if (hash) return headingToks(hash[1], hash[2]);
  const variable = /^(@[A-Za-z0-9_-]+)(\s*=\s*)(.*)$/.exec(line);
  if (variable) {
    return [
      { c: 'v', t: variable[1] },
      { c: 'p', t: variable[2] },
      { c: 'u', t: variable[3] },
    ];
  }
  const request = /^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)(\s+)(.*)$/.exec(line);
  if (request) {
    return [
      { c: 'k', t: request[1] },
      { c: 'p', t: request[2] },
      { c: 'u', t: request[3] },
    ];
  }
  return [{ c: 'p', t: line }];
}

export type CodeLang = 'sql' | 'http';

/** Tokenise one line. */
export function tokenize(lang: CodeLang, line: string): Tok[] {
  return lang === 'sql' ? sqlToks(line) : httpToks(line);
}

/** Tokenise a whole sample; `activeLine` is 1-based and only used by the IDE mock. */
export function highlight(lang: CodeLang, code: string, activeLine?: number): Line[] {
  return code.split('\n').map((line, i) => ({
    toks: tokenize(lang, line),
    active: activeLine !== undefined && i + 1 === activeLine,
  }));
}

/** Escape + wrap tokens in spans at build time, so pages ship coloured markup. */
function esc(value: string): string {
  return value.replace(/[&<>]/g, (ch) => (ch === '&' ? '&amp;' : ch === '<' ? '&lt;' : '&gt;'));
}

/**
 * One <span class="il"> per source line, newline inside the span so <pre> keeps the
 * layout. Line numbers come from a CSS counter, not from the markup.
 */
export function renderLines(lines: Line[]): string {
  return lines
    .map((line) => {
      const inner = line.toks.map((tok) => `<span class="t-${tok.c}">${esc(tok.t)}</span>`).join('');
      return `<span class="il${line.active ? ' is-active' : ''}">${inner}\n</span>`;
    })
    .join('');
}

/** Convenience: tokenise and render in one call. */
export function renderCode(lang: CodeLang, code: string, activeLine?: number): string {
  return renderLines(highlight(lang, code, activeLine));
}
