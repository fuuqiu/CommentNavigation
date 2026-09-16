// Rasterises the two PNG assets the site needs from SVG composed here:
//   public/og.png            1200×630  social card
//   public/apple-touch-icon.png 180×180 iOS home-screen icon
//
// Run after changing the icon or the tagline:  npm run og
// Both files are committed, so a normal build never needs sharp.
//
// macOS fallback if sharp ever refuses to install:
//   qlmanage -t -s 1200 -o <dir> <file>.svg
// then crop/resize the result — check the dimensions with
//   sips -g pixelWidth -g pixelHeight public/og.png

import { mkdir, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import sharp from 'sharp';

const here = dirname(fileURLToPath(import.meta.url));
const publicDir = resolve(here, '..', 'public');

const ACCENT = '#3592C4';
const BG = '#0e1116';
const INK = '#e6edf3';
const MUTED = '#9aa7b4';
const FONT = "'Helvetica Neue', Helvetica, Arial, 'Liberation Sans', sans-serif";

/** The plugin icon (META-INF/pluginIcon.svg), as a group placed at x/y and scaled. */
function icon(x, y, size, circle = ACCENT, stroke = '#ffffff') {
  const s = size / 40;
  return `<g transform="translate(${x} ${y}) scale(${s})">
    <circle cx="20" cy="20" r="18" fill="${circle}"/>
    <path d="M12 15 L28 15" stroke="${stroke}" stroke-width="2.5" stroke-linecap="round"/>
    <path d="M12 20 L28 20" stroke="${stroke}" stroke-width="2.5" stroke-linecap="round"/>
    <path d="M12 25 L28 25" stroke="${stroke}" stroke-width="2.5" stroke-linecap="round"/>
    <path d="M8 15 L10 15" stroke="${stroke}" stroke-width="2" stroke-linecap="round"/>
    <path d="M8 20 L10 20" stroke="${stroke}" stroke-width="2" stroke-linecap="round"/>
    <path d="M8 25 L10 25" stroke="${stroke}" stroke-width="2" stroke-linecap="round"/>
  </g>`;
}

/** Geometric echo of the outline tree: indented bars, deeper levels dimmer. */
function outlineBars(x, y) {
  const rows = [
    [0, 210],
    [26, 170],
    [52, 140],
    [52, 156],
    [26, 182],
    [52, 132],
    [78, 110],
    [0, 190],
  ];
  return rows
    .map(([indent, width], i) => {
      const opacity = i === 3 ? 1 : 0.78 - indent / 380;
      const fill = i === 3 ? ACCENT : '#7f8c99';
      return `<rect x="${x + indent}" y="${y + i * 38}" width="${width}" height="12" rx="6" fill="${fill}" opacity="${opacity.toFixed(2)}"/>`;
    })
    .join('\n    ');
}

const og = `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="630" viewBox="0 0 1200 630">
  <rect width="1200" height="630" fill="${BG}"/>
  <rect width="1200" height="8" fill="${ACCENT}"/>
  <rect x="700" y="96" width="420" height="440" rx="18" fill="#161b22" stroke="#232a33" stroke-width="2"/>
  ${outlineBars(744, 150)}
  ${icon(80, 92, 96)}
  <text x="80" y="288" font-family="${FONT}" font-size="66" font-weight="700" fill="${INK}">Code Comment</text>
  <text x="80" y="364" font-family="${FONT}" font-size="66" font-weight="700" fill="${INK}">Navigator</text>
  <text x="80" y="428" font-family="${FONT}" font-size="30" fill="${MUTED}">A table of contents for your SQL</text>
  <text x="80" y="470" font-family="${FONT}" font-size="30" fill="${MUTED}">and HTTP files.</text>
  <rect x="80" y="516" width="64" height="4" rx="2" fill="${ACCENT}"/>
  <text x="80" y="566" font-family="${FONT}" font-size="27" font-weight="600" fill="${ACCENT}">IntelliJ IDEA · MIT · free</text>
</svg>`;

// iOS masks the corners itself, so the artwork is a full-bleed tile, not a circle.
const touch = `<svg xmlns="http://www.w3.org/2000/svg" width="180" height="180" viewBox="0 0 180 180">
  <rect width="180" height="180" fill="${ACCENT}"/>
  <path d="M54 66 L126 66" stroke="#ffffff" stroke-width="11" stroke-linecap="round"/>
  <path d="M54 90 L126 90" stroke="#ffffff" stroke-width="11" stroke-linecap="round"/>
  <path d="M54 114 L126 114" stroke="#ffffff" stroke-width="11" stroke-linecap="round"/>
  <path d="M36 66 L42 66" stroke="#ffffff" stroke-width="9" stroke-linecap="round"/>
  <path d="M36 90 L42 90" stroke="#ffffff" stroke-width="9" stroke-linecap="round"/>
  <path d="M36 114 L42 114" stroke="#ffffff" stroke-width="9" stroke-linecap="round"/>
</svg>`;

await mkdir(publicDir, { recursive: true });
await writeFile(resolve(publicDir, 'og.svg.tmp'), og);

// density 72 keeps 1 SVG user unit = 1 px; the explicit resize is the belt to that
// braces, because og:image dimensions are declared in the <head> and must match.
await sharp(Buffer.from(og), { density: 72 })
  .resize(1200, 630, { fit: 'fill' })
  .png({ compressionLevel: 9 })
  .toFile(resolve(publicDir, 'og.png'));
await sharp(Buffer.from(touch), { density: 72 })
  .resize(180, 180, { fit: 'fill' })
  .png({ compressionLevel: 9 })
  .toFile(resolve(publicDir, 'apple-touch-icon.png'));

const { rm } = await import('node:fs/promises');
await rm(resolve(publicDir, 'og.svg.tmp'), { force: true });

for (const file of ['og.png', 'apple-touch-icon.png']) {
  const meta = await sharp(resolve(publicDir, file)).metadata();
  console.log(`${file}: ${meta.width}×${meta.height}`);
}
