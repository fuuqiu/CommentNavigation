export type Lang = 'en' | 'zh';

export const LANGS: Lang[] = ['en', 'zh'];

/** BCP-47 tag used for <html lang>, hreflang and the sitemap's i18n pairing. */
export const HTML_LANG: Record<Lang, string> = {
  en: 'en',
  zh: 'zh-Hans',
};

/** Facebook-style locale, only used for og:locale. */
export const OG_LOCALE: Record<Lang, string> = {
  en: 'en_US',
  zh: 'zh_CN',
};

export type RouteKey = 'home';

/**
 * Canonical path for every page in every language — the one place a URL is written.
 * Every path ends in a slash to match astro.config.mjs `trailingSlash: 'always'`
 * and the Worker's `html_handling: "force-trailing-slash"`.
 *
 * This product is a single landing page per language; the sections below the hero are
 * in-page anchors (see SECTIONS), not routes, so they never need hreflang pairs.
 */
export const ROUTES: Record<RouteKey, Record<Lang, string>> = {
  home: { en: '/', zh: '/zh/' },
};

/** In-page anchors. Ids are language-independent so both pages share one set. */
export const SECTIONS = ['features', 'syntax', 'install', 'faq'] as const;
export type SectionKey = (typeof SECTIONS)[number];

/* ── Product facts (single source of truth for every page) ──────────────────────
   Traceable to https://github.com/fuuqiu/CommentNavigation @ main:
   README.md, src/main/resources/META-INF/plugin.xml, LICENSE.txt.            */

export const APP = 'Code Comment Navigator';
export const PLUGIN_ID = 'cn.tinyue.commentnavigation';
export const VERSION = '1.1.0';
export const AUTHOR = 'Fuuqiu';
/** LICENSE.txt: "Copyright (c) 2024–2026 Fuuqiu (Tinyue)". */
export const COPYRIGHT_HOLDER = 'Fuuqiu (Tinyue)';
export const COPYRIGHT_YEARS = '2024–2026';
export const EMAIL = 'fuuqiu@gmail.com';
export const LICENSE = 'MIT';

/** Compatibility range from plugin.xml: since-build 262, until-build 262.*. */
export const IDE = 'IntelliJ IDEA 2026.2';
export const IDE_BUILD = '262.*';
export const JDK = 'JDK 25';
export const BUILD_COMMAND = './gradlew test buildPlugin';
export const DIST_ZIP = `build/distributions/comment-navigation-${VERSION}.zip`;
/** plugin.xml keyboard-shortcut, $default keymap: "shift meta F12". */
export const SHORTCUT = 'Shift+Cmd+F12';
export const TOOL_WINDOW = 'Comment Outline';
export const ACTION = 'Comment Structure';

export const GITHUB = 'https://github.com/fuuqiu/CommentNavigation';
export const ISSUES = 'https://github.com/fuuqiu/CommentNavigation/issues';
export const LICENSE_URL = 'https://github.com/fuuqiu/CommentNavigation/blob/main/LICENSE.txt';
export const README_URL = 'https://github.com/fuuqiu/CommentNavigation/blob/main/README.md';
export const EXAMPLE_SQL_URL =
  'https://github.com/fuuqiu/CommentNavigation/blob/main/examples/comment-outline.sql';
export const EXAMPLE_HTTP_URL =
  'https://github.com/fuuqiu/CommentNavigation/blob/main/examples/comment-outline.http';

/**
 * JetBrains Marketplace listing for THIS plugin id (cn.tinyue.commentnavigation).
 *
 * Empty on purpose: the listing is not live yet, and there are no GitHub Releases.
 * Never point this at plugin id 27390 / cn.tinyue.console — that is a different,
 * retired listing.
 *
 * Flip the switch by pasting the real URL here and rebuilding:
 *   ''            → hero CTA is "Get it on GitHub"; the install section explains that
 *                   the listing is being set up, plus build-from-source + Install
 *                   Plugin from Disk.
 *   'https://…'   → hero CTA becomes "Install from JetBrains Marketplace" and links
 *                   here; the install section leads with the Marketplace step.
 */
export const MARKETPLACE_URL = '';

/** Language switcher target for the page you are currently on. */
export function otherLang(lang: Lang): Lang {
  return lang === 'en' ? 'zh' : 'en';
}

/** Absolute URL for a path, against the origin injected via SITE_URL. */
export function absolute(path: string, site: URL): string {
  return new URL(path, site).href;
}

/** Chrome shared by every page. */
export const ui = {
  en: {
    langName: 'English',
    langShort: 'EN',
    switchLabel: 'Language',
    skip: 'Skip to content',
    navLabel: 'Sections',
    nav: {
      features: 'Features',
      syntax: 'Syntax',
      install: 'Install',
      faq: 'FAQ',
    },
    github: 'GitHub',
    footerRights: `© ${COPYRIGHT_YEARS} ${COPYRIGHT_HOLDER}`,
    footerLicense: 'MIT License',
    footerIssues: 'Issues',
    footerEmail: 'Email',
    footerNavLabel: 'Links',
    ogImageAlt: `${APP} — a comment outline for SQL and HTTP files in IntelliJ IDEA`,
    iconAlt: `${APP} plugin icon`,
  },
  zh: {
    langName: '简体中文',
    langShort: '中文',
    switchLabel: '语言',
    skip: '跳到正文',
    navLabel: '页面小节',
    nav: {
      features: '功能',
      syntax: '标题写法',
      install: '安装',
      faq: '常见问题',
    },
    github: 'GitHub',
    footerRights: `© ${COPYRIGHT_YEARS} ${COPYRIGHT_HOLDER}`,
    footerLicense: 'MIT 许可证',
    footerIssues: '问题反馈',
    footerEmail: '邮箱',
    footerNavLabel: '相关链接',
    ogImageAlt: `${APP} —— IntelliJ IDEA 里的 SQL / HTTP 注释大纲`,
    iconAlt: `${APP} 插件图标`,
  },
} as const;
