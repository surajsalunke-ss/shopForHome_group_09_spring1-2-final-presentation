# Local frontend and shopping demo

This milestone restores the Angular 7 storefront against the local Spring backend. It stays on Angular 7; framework modernization is separate work. Use only synthetic data and keep both servers on loopback.

## Windows prerequisites

- Windows x64, PowerShell 5.1 or later, Git, `curl.exe`, and a writable checkout.
- Internet access for the first tool/dependency download (Node.js, GitHub, Maven Central, npm, and the legacy node-sass binary).
- Available ports 4200 and 8080. Chrome or Edge is required for the headless frontend tests.
- No global Java, Maven, Node, or npm installation is required.

The setup pins Node **10.24.1 / npm 6.14.12**, compatible with Angular **7.1.x**, CLI **7.3.x**, TypeScript **3.1.6**, and node-sass **4.10**. These are obsolete versions for local restoration, not a production recommendation. The Node archive is downloaded from nodejs.org and SHA-256 checked. npm uses the checked-in v1 lockfile and project-local cache. Backend setup uses Java 11 and Maven 3.9.16; see [LOCAL-BACKEND.md](LOCAL-BACKEND.md).

## Exact setup and launch commands

Run from the repository root. Replace only the first path for another checkout; scripts resolve tools and state relative to their own location.

```powershell
Set-Location -LiteralPath 'D:\projects\chatgpt\ShopForHome'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\setup-local.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\frontend\setup-local.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\frontend\local.ps1 Install
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\local.ps1 Build
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\frontend\local.ps1 Build
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\local.ps1 Start -Demo
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\frontend\local.ps1 Start
Start-Process 'http://127.0.0.1:4200/product'
```

Use `127.0.0.1` consistently; `localhost` is a different browser origin for cookies and storage. The frontend serves on `127.0.0.1:4200`. Its development proxy sends `/api/*` to `127.0.0.1:8080/*`. For example `/api/product` becomes `/product`. JWT authorization is preserved, with no broad CORS rule or authentication bypass. The interceptor attaches the bearer token only to this API prefix and lets the browser set multipart boundaries.

## Explicit, repeatable demo data

`Start -Demo` enables the fixture runner. Plain `Start` explicitly disables it. On a **new database**, plain `Start` still produces an empty catalog and no seeded accounts. Once populated, the database persists; starting without `-Demo` does not remove existing data.

The runner requires all three: Spring profile `local`, `shop.demo.enabled=true`, and a verified H2 connection with `server.address=127.0.0.1`. It inserts only missing stable product IDs, category type 0, and missing emails. It never deletes records, resets stock, changes an existing account's password/role, or overwrites edited products. If those IDs/emails already exist, their existing values win. It runs transactionally, and repeated startup is tested.

| Demo item | Price | Initial stock |
|---|---:|---:|
| Amber Table Lamp (`demo-lamp`) | $29 | 20 |
| Sage Ceramic Vase (`demo-vase`) | $18 | 20 |
| Terracotta Cushion (`demo-cushion`) | $24 | 20 |

All three belong to Shop Living Room and use checked-in SVG artwork. Other categories can be empty.

| Synthetic account | Role | Demo password |
|---|---|---|
| `customer@example.invalid` | Customer | `DemoOnly123!` |
| `manager@example.invalid` | Manager | `DemoOnly123!` |

These passwords are deliberately public local fixtures. Do not reuse them elsewhere. Registration can create another synthetic customer, for example `your-test@example.invalid`, name `Test Customer`, phone `0000000000`, address `Local demo address`. Registration always creates a customer regardless of the role supplied by the browser. Mail delivery remains disabled.

To enable demo data on a backend already running without it:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\local.ps1 Stop
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\local.ps1 Start -Demo
```

## Checks, status, stop, and restart

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\frontend\local.ps1 Test
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\frontend\local.ps1 Lint
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\frontend\local.ps1 Status
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\local.ps1 Status
Invoke-RestMethod 'http://127.0.0.1:4200/api/product'

powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\frontend\local.ps1 Stop
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\local.ps1 Stop
```

`Build` runs Angular AOT compilation; `Test` runs the full Karma suite in headless Chrome (Edge can supply the Chromium executable). If auto-discovery fails, set `$env:CHROME_BIN` to your installed browser executable for that shell. The scripts restore inherited environment variables afterward. `Lint` preserves the existing TSLint rules and currently fails on legacy style violations; it is not silently bypassed.

Stop the frontend before reinstalling dependencies. Stop the backend before rebuilding its JAR. After a compiler/import change, stop and restart the frontend if legacy incremental compilation reports `externalModuleIndicator`; a clean compilation succeeds. Background processes survive terminal closure. Stop commands validate the recorded executable and process start time before terminating the PID.

Runtime files live under `.local`: `frontend.stdout.log`, `frontend.stderr.log`, `frontend-process.json`, the corresponding backend files, and `database/shopforhome.mv.db`. Downloads/caches are under `.tools`; build and test outputs are under `frontend/dist` and `backend/target`. These are ignored, along with credentials and machine state. Do not delete the persistent database to repeat demo setup.

## Important fixes and interview talking points

1. **Compatibility is a matrix.** Pin a Node/npm pair accepted by Angular 7 and its native Sass dependency. Use `npm ci` and a compatible lockfile format. Remove the unused Angular-10-only uploader module; the application already uses a native CSV file input. Avoid treating a major framework upgrade as an installation fix.
2. **Compilation and runtime are different checks.** The original JIT build passed, but AOT exposed undeclared search fields and stylesheet links embedded in component templates. Styles now load from the document head. SheetJS's newer `.mjs` entry also caused a browser `require` error with this build stack; selecting its packaged browser bundle fixes loading without upgrading Angular.
3. **Same-origin API routing simplifies local authentication.** `/api` is proxied server-side to Spring. Scope JWT headers to that prefix, avoid sending tokens to unrelated assets, and avoid forcing JSON headers on file uploads. Session storage keeps a non-remembered login through page refresh; logout clears both storage modes.
4. **Wait for mutations before navigation.** Cart controls and checkout are gated while updates are pending. Quantity responses update the displayed item; checkout navigates to orders only after success. Errors remain visible. The old UI's invented coupon/shipping totals were removed because the backend did not charge them.
5. **The server owns business rules.** Guest-cart merge reloads product data instead of trusting browser prices. Quantity edits validate stock; buying exactly the remaining stock is permitted; empty checkout is rejected. Public registration cannot select a privileged role. Tests also verify order ownership.
6. **Fixtures are not migrations.** The opt-in local/H2 guard and insert-if-missing identities make demo setup repeatable without destroying existing work. Regression tests use isolated in-memory databases, never the persistent browser demo database.

## Validation and limitations

See [LOCAL-FRONTEND-VALIDATION.md](LOCAL-FRONTEND-VALIDATION.md) for actual results and the manual browser flow.

- H2 supports local development only. This does not validate MySQL behavior/schema migration, GCP/App Engine deployment, or production operation.
- Checkout creates a database order and updates stock. There is no payment collection, shipping integration, coupon calculation, or real fulfillment. Mail intentionally cannot be sent.
- Customer listing/details, registration/login, guest merge, cart quantity/removal, checkout, order details, and cancellation were exercised. Manager administration, CSV import/export, wishlist, and fulfillment are not comprehensively validated by this milestone.
- Angular 7, Node 10, old native dependencies, and the legacy authorization architecture need separate modernization/security work. Concurrent checkout/stock contention and price changes after an item enters a cart are not proven safe by these single-user tests.
- Existing styling and mobile behavior are retained rather than redesigned. Production build configuration and remote deployment URLs have not been validated.
