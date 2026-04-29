Frontend GH Pages Deploy — log and actions

Summary
- Branch merged: `chore/frontend-gh-pages` → `master` (PR #1).
- Workflow: `.github/workflows/deploy-frontend.yml` builds `firmador-frontend` and deploys to `gh-pages` using `peaceiris/actions-gh-pages@v3`.

Steps performed
1. Merged PR #1 into `master` to add frontend scaffold and workflows.
2. CI run failed initially because `actions/upload-artifact@v3` (used indirectly) is deprecated.
   - Fix: Replaced artifact/deploy flow with `peaceiris/actions-gh-pages@v3`.
3. Second run failed because the workflow used `npm ci` but `package-lock.json` was missing.
   - Fix: Replaced `npm ci` with `npm install`.
4. Third run failed to push because `permissions.contents` was `read`.
   - Fix: Changed workflow permissions to give `contents: write` so `GITHUB_TOKEN` can push `gh-pages`.
5. Final run succeeded: the action built the site and pushed the `gh-pages` branch.

Important workflow edits
- `.github/workflows/deploy-frontend.yml`:
  - Use `npm install --no-audit --no-fund` in `firmador-frontend` (no lockfile present).
  - Deploy with `peaceiris/actions-gh-pages@v3` (publish_dir: `./firmador-frontend/dist`, publish_branch: `gh-pages`).
  - Set `permissions.contents: write` and `permissions.pages: write`.

Action run details
- Failed run (deprecated artifact): Actions run id database 25139139223 (created 2026-04-29T23:27:08Z).
- Fixed run (npm ci -> install) database 25139375157 (failed pushing due to permissions).
- Successful run database 25139407698 (created 2026-04-29T23:36:25Z) — build completed and `gh-pages` branch created.

Where to check now
- Actions run logs: https://github.com/DreyOWO/FirmadorDigital/actions
- `gh-pages` branch: https://github.com/DreyOWO/FirmadorDigital/tree/gh-pages
- Expected Pages URL: https://DreyOWO.github.io/FirmadorDigital/
  (If it doesn't show immediately, allow a few minutes for Pages provisioning; check repository Pages settings.)

Next steps / Recommendations
- Add `package-lock.json` to `firmador-frontend` (commit lockfile) to allow faster reproducible builds with `npm ci`.
- Replace `peaceiris` with `actions/deploy-pages` + `actions/upload-pages-artifact` after GitHub restores artifact compatibility, if desired.
- Consider Vercel for production hosting (better preview handling and environment variables).
- Implement incremental deployment checks (validate built site contents before pushing).

If you want, I can:
- Add `package-lock.json` by running `npm ci` locally and committing the lockfile (you'll need Node locally), or
- Switch the workflow to use `actions/deploy-pages` again once artifact versions are stable.

Logs and CI run IDs were recorded above for traceability.
