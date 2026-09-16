import type { APIRoute } from 'astro';

// Generated rather than static, so the Sitemap: line always carries the origin that was
// injected via SITE_URL at build time. A hardcoded robots.txt is how a staging origin
// ends up in production's sitemap reference.
//
// Everything is allowed, AI crawlers included: this is public documentation for a free,
// open-source plugin, and being quotable is the whole point.
export const GET: APIRoute = ({ site }) => {
  const origin = site ? site.href.replace(/\/$/, '') : '';
  const body = [
    'User-agent: *',
    'Allow: /',
    '',
    // @astrojs/sitemap emits sitemap-index.xml plus the sitemap-N.xml files it points at.
    `Sitemap: ${origin}/sitemap-index.xml`,
    '',
  ].join('\n');

  return new Response(body, {
    headers: { 'Content-Type': 'text/plain; charset=utf-8' },
  });
};
