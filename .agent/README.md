# Noveliq Agent Docs

This directory is the shared engineering context for the Noveliq Android app.

Current implementation snapshot:

- Foundation hardening is largely complete.
- Catalog expansion is in progress.
- The app now supports audiobook detail navigation and chapter loading from the server.
- Playback and downloads are still not implemented.

Files in this directory:

- `about_project.md`: Technical and product-level introduction to the project.
- `state.md`: Current implementation status and architectural assessment.
- `road_map.md`: Recommended development roadmap from current state to a production-ready app.
- `architecture_target.md`: Target architecture and modularization direction for phone, Android Auto, Wear OS, and offline support.
- `best_practices.md`: Engineering practices and architectural guardrails for future work.
- `milestones/`: Major roadmap milestones, each with implementation notes and exit criteria.

How to use this folder:

- Read `about_project.md` first if you are new to the project.
- Read `state.md` before making major architectural changes.
- Use `road_map.md` and the `milestones/` files to plan upcoming work.
- Treat `best_practices.md` as the default standard unless there is an explicit product or technical reason to do otherwise.
