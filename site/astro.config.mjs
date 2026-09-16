import { defineConfig } from 'astro/config';
import sitemap from '@astrojs/sitemap';

// The public origin is injected at build time:
//   SITE_URL=https://commentnavigation.app.topxup.com npm run build
// `site` is what Astro uses for canonical URLs, hreflang alternates, the sitemap and
// the Sitemap: line in robots.txt — a wrong value here silently poisons every SEO
// signal on the site. The fallback is the house domain convention
// (${app}.app.topxup.com), which is also the domain wired into wrangler.jsonc.
const SITE_URL = process.env.SITE_URL || 'https://commentnavigation.app.topxup.com';

export default defineConfig({
  site: SITE_URL,
  // Every internal link in this project is written with a trailing slash, and the
  // Worker forces the slash (assets.html_handling), so only one form ever serves a
  // 200 — no /zh vs /zh/ duplicate-content pair for Google to pick between.
  trailingSlash: 'always',
  build: { format: 'directory' },
  compressHTML: true,
  integrations: [
    sitemap({
      // Pairs / with /zh/ as <xhtml:link rel="alternate"> inside the sitemap,
      // matching the hreflang tags emitted by layouts/Base.astro.
      i18n: {
        defaultLocale: 'en',
        locales: { en: 'en', zh: 'zh-Hans' },
      },
    }),
  ],
});
