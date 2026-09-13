# Frontend restoration validation — 2026-09-13

Branch: `local/frontend-restoration`, based on `828b782c4e347df94b460edb1f1c4f14427dfd60`. Changes remain local and uncommitted. Backend branch and PR #1 were not changed or published to.

## Automated checks

| Check | Actual result |
|---|---|
| Local Node/npm | 10.24.1 / 6.14.12 |
| Clean npm ci | Pass, 1,184 packages |
| Angular AOT build | Pass; outdated Browserslist warning remains |
| Full frontend suite | 22 passing tests in HeadlessChrome 152, including 8 new focused tests |
| TSLint | Fails: 381 reported style violations across the source tree; rules preserved |
| Backend clean verify | 7 tests, 0 failures/errors/skips |
| Persistent demo restart | 3 products before and after; browser account and order retained |
| Runtime | Loopback 4200/8080; /api development proxy works |
| Review hygiene | git diff --check passes; index remains empty; common credential-pattern scan found 0 matches in added patch lines |

Backend tests comprise five existing restoration tests plus two new demo/shopping tests. Three pre-existing deleted legacy test files remain deleted. Tests use isolated in-memory H2 databases. New coverage includes repeatable seeding without overwriting an edited product, customer-only registration, quantity validation, buying the last stock, checkout totals, empty-cart rejection, and order ownership. Frontend tests cover JWT scoping/multipart headers, safe return URLs, quantity responses, pending saves, and checkout success/error navigation.

## Actual browser results

All actions used `http://127.0.0.1:4200` and synthetic local data.

1. Catalog displayed three products with local artwork, descriptions, stock 20, and prices $24/$29/$18. Lamp details showed $29, stock 20, quantity 1, subtotal $29.
2. Guest addition succeeded. Cart contained one lamp; checkout redirected to `/login?returnUrl=%2Fcart`.
3. Registered `browser-flow@example.invalid`, name `Browser Customer`, phone `0000000000`, address `Local demo address`, password `BrowserOnly123!`. Registration returned to login.
4. Logged in with Remember me unchecked. Default navigation reached the catalog. Opening the cart merged the guest lamp.
5. Increased quantity 1 to 2; total became $58. Full refresh retained authentication, quantity 2, and $58. Customer name now remains visible after refresh.
6. Checkout created order **#38**, status New, total **$58**. Details showed unit price $29, quantity 2, subtotal $58. Cart became empty; stock fell to 18. No payment was requested or taken.
7. Authenticated addition of a vase succeeded; cart showed one vase at $18. Removing it produced the empty-cart state.
8. Canceled order #38; status changed to Canceled, stock returned to 20, and the order record remained.
9. Restarted backend with `-Demo`: still three products; browser account and canceled order remained accessible.
10. Prepared a new review cart with two lamps ($58), without checking it out again. It remains under the browser test account.

Screenshots in the completion outputs: `catalog.png`, `cart.png`, `orders.png` (original New order), `order-detail.png`, `order-canceled.png`. All except `orders.png` reflect the final local banner/navigation fixes.

The browser log retained earlier incremental-compiler errors and a restart disconnect. No new errors were observed during the successful shopping flow after the clean restart. A clean AOT build passed.

## Limitations and preserved work

No payment provider, shipping/fulfillment integration, coupon calculation, or mail delivery is supported. Wishlist, manager fulfillment/administration, and CSV/Excel features are not comprehensively browser-validated. H2 does not prove MySQL or GCP compatibility. Production security, concurrent checkout behavior, mobile polish, lint cleanup, and upgrading obsolete Angular/Node dependencies remain separate work.

Preserved: the extra `spring-core` dependency in `backend/pom.xml`, three deleted historical tests, the prior environment-file comment, and untracked `github link for project.txt`. No staging, commit, push, or PR action was performed. The completion patch is relative to the starting working files and excludes unrelated pre-existing edits. See [LOCAL-FRONTEND.md](LOCAL-FRONTEND.md) for exact commands and interview explanations.

The patch contains 57 files, including new files. Most of its size is removal of npm v2 lockfile metadata when normalizing to npm 6's v1 format. Public demo/test passwords are intentional; no live credentials, downloaded tools, database files, or logs are included. The patch is for review against the starting working tree, not for applying again to this already-modified checkout.
