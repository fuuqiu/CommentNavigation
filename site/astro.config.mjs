import { defineConfig } from 'astro/config';
import sitemap from '@astrojs/sitemap';

// 构建时统一 canonical、hreflang、sitemap 与 robots.txt 的公开域名。
// 默认值与线上落地页和 Wrangler 路由保持一致。
const SITE_URL = process.env.SITE_URL || 'https://commentnavigation.plugins.topxup.com';

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
