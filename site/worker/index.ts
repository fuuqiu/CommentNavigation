// Code Comment Navigator site worker.
//
// It does exactly one thing: hand the request to the Static Assets binding. The site
// is purely static — no API, no forms, no KV. Don't add business logic here; a landing
// page's value is that it is fast, crawlable, and cannot fall over.
//
// Redirects, headers and 404 handling are all configured in wrangler.jsonc
// (`html_handling`, `not_found_handling`), so this file should stay this short.

/** The only binding this site needs: the static assets built into ./dist. */
export interface Env {
  ASSETS: { fetch(request: Request): Promise<Response> };
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    return env.ASSETS.fetch(request);
  },
};
