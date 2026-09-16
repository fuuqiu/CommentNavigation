---
name: comment-navigator
description: Generate or organize SQL and JetBrains HTTP Client .http/.rest files with Markdown headings inside comments for Code Comment Navigator. Use when the user requests an outline-ready SQL script, API request file, or Scratch file. This skill does not execute SQL or send requests.
---

# Code Comment Navigator file conventions

Preserve the requested SQL dialect, API contract, variables, business logic, and content language. When reorganizing a file, preserve query semantics, request order, and pre/post-request scripts. Verify missing schema or API details; use explicit placeholders in examples and never invent real credentials.

## Outline structure

- Use 1–6 hashes for heading levels, followed by a space and a non-empty title.
- Use business domain, operation/query, and scenario groups only as needed. Prefer consecutive levels and short, distinguishable titles. Siblings keep file order.
- Add headings to meaningful sections, not every explanatory line. Use ordinary comments for ordinary notes.
- With explicit headings, ordinary comments stay out of the outline. Without headings, the plugin falls back to a flat comment list. Skipped levels attach to the preceding shallower heading.

## Keep files concise

- Replace decoration with headings. Do not generate `====`, `----`, asterisk borders, ASCII banners, or a hand-written table of contents. Preserve HTTP `###` request separators.
- Give each independently useful query or request scenario one short heading. Add shared groups only where useful; 1–3 levels usually suffice. Do not stack synonymous domain/operation/scenario headings or annotate every statement, field, or step.
- Titles name the query, operation, or scenario. Keep lengthy explanations, line numbers, and section numbering out of headings. At the top, include only necessary purpose and prerequisites, not the full requirements, API documentation, change history, or business specification.
- Ordinary comments explain what code cannot: business definitions, units, time zones, dependencies, significant side effects, or expected results. Prefer 1–2 nearby lines. Do not narrate obvious code; use clear parameter names and column aliases.
- Put detailed procedures, formula derivations, and verification reports in the conversation or existing user documentation. Create companion documentation only when needed by the user. Preserve constraints, transaction/request dependencies, and warnings needed for correct use.
- When organizing old files, replace numbered chapter banners with explicit headings and remove repetitive prose. Retain at least one meaningful explicit heading so ordinary comments do not all become fallback outline entries. Preserve execution order, parameters, and semantics.

## SQL

Prefer standalone `-- # Heading`, `-- ## Heading`, and `-- ### Heading` lines. Headings inside `/* ... */` comments also work, including lines prefixed with `*`. Do not put headings after a SQL statement on the same line. Hashes inside strings, bare Markdown headings, and MySQL `#` line comments are not substitutes for this format.

```sql
-- # Order analytics
-- ## Daily paid orders
SELECT CAST(paid_at AS DATE) AS paid_date,
       COUNT(*) AS order_count,
       SUM(total_amount) AS paid_amount
FROM orders
WHERE status = 'PAID'
GROUP BY CAST(paid_at AS DATE)
ORDER BY paid_date;

-- ## Missing payment timestamps
SELECT id FROM orders WHERE status = 'PAID' AND paid_at IS NULL;
```

For long scripts, use relevant sections such as schema, indexes, seed data, migrations, and analytical queries. Do not add unrelated SQL to inflate length. Preserve transaction boundaries and dependency order.

## JetBrains HTTP Client (.http / .rest)

Prefer an empty `###` request separator, followed by `// # Heading` through `// ###### Heading`, then the request line. The `# # Heading` form is also supported.

**After a JSON or other request body, place the next section's headings AFTER the next `###` separator.** Never place the next request's comments between the previous body and its following separator: the IDE can interpret them as part of that body. Separate headers from the body with a blank line. JSON must contain no comments or trailing commas.

```http
@host = https://example.invalid

###
// # Orders API
// ## Create order
POST {{host}}/orders
Content-Type: application/json

{
  "skuId": 1001,
  "quantity": 2
}

###
// ## Find order by ID
GET {{host}}/orders/example-order-id
Accept: application/json
```

This example demonstrates formatting; the host and resource ID are placeholders. Use existing project environment variables, authentication, and API contracts for actual output.

- `### Request name` is a native request separator and always produces a level-2 outline node, not a level-3 Markdown heading. For precise nesting, use an empty `###` plus `// ### Heading`; mixing the forms can change parent relationships.
- An empty `###` does not create an outline node. Do not change request boundaries just to create outline entries.
- Comments inside `< {% ... %}` pre-request and `> {% ... %}` response-handler scripts are excluded from the outline. Keep scripts attached to their requests and put the next section's headings after the next separator.
- Use existing variables or placeholders for authentication; never embed tokens or passwords in examples.

## Before delivery

1. Check that SQL headings occupy standalone comment lines and HTTP headings follow the corresponding separator. Each heading needs a space and a title.
2. Check every HTTP request boundary, especially after bodies. Validate JSON and preserve script-to-request associations.
3. Remove decorative banners, duplicate tables of contents, obvious narration, and synonymous heading stacks. Preserve necessary business definitions and warnings.
4. Check the intended business hierarchy, SQL dialect, statement semantics, transactions, and API contracts.
5. Return the complete requested files or requested edits. Do not claim SQL or requests were executed unless they were; this skill itself does not authorize execution.

Open the files with the plugin, inspect the right-hand **Comment Outline**, and click headings to verify navigation. If the environment cannot operate the IDE, say this check remains unperformed.

References: [Plugin source and examples](https://github.com/fuuqiu/CommentNavigation) · [JetBrains HTTP syntax](https://www.jetbrains.com/help/idea/exploring-http-syntax.html)
