import {
  APP,
  AUTHOR,
  GITHUB,
  HTML_LANG,
  IDE,
  LICENSE_URL,
  ROUTES,
  VERSION,
  absolute,
  type Lang,
} from './index';
import { HOME } from './home';
import { FAQ, plainAnswer } from './faq';

/**
 * SoftwareApplication for the plugin itself. Free software, so the offer is a real
 * 0 USD offer rather than an omitted price — Google treats a missing price as unknown.
 */
export function softwareApplicationLd(lang: Lang, site: URL): Record<string, unknown> {
  return {
    '@context': 'https://schema.org',
    '@type': 'SoftwareApplication',
    name: APP,
    url: absolute(ROUTES.home[lang], site),
    image: absolute('/og.png', site),
    description: HOME[lang].meta.description,
    inLanguage: HTML_LANG[lang],
    applicationCategory: 'DeveloperApplication',
    operatingSystem: 'Windows, macOS, Linux',
    softwareVersion: VERSION,
    softwareRequirements: IDE,
    license: LICENSE_URL,
    downloadUrl: GITHUB,
    isAccessibleForFree: true,
    offers: {
      '@type': 'Offer',
      price: '0',
      priceCurrency: 'USD',
      availability: 'https://schema.org/InStock',
    },
    author: { '@type': 'Person', name: AUTHOR, url: GITHUB },
    publisher: { '@type': 'Person', name: AUTHOR, url: GITHUB },
  };
}

export function faqPageLd(lang: Lang): Record<string, unknown> {
  return {
    '@context': 'https://schema.org',
    '@type': 'FAQPage',
    inLanguage: HTML_LANG[lang],
    mainEntity: FAQ[lang].map((entry) => ({
      '@type': 'Question',
      name: plainAnswer(entry.q),
      acceptedAnswer: { '@type': 'Answer', text: plainAnswer(entry.a) },
    })),
  };
}
