# Detail layout snapshot — 2026-10-21

This folder preserves the eight original source files before the RTL reference-layout experiment.
To revert this experiment, copy its `app/` tree over the project's `app/` tree, preserving relative paths.
Restore all eight files together: layouts, backdrop view, fragments, TMDB service, repository and view model.
The extra `ic_detail_add.xml` and `ic_detail_added.xml` drawables can remain unused after rollback.

The experiment changes TV layouts only. Mobile bookmark icons and layouts remain unchanged.
TV series receive a cast shelf below their existing season/episode controls.
No APK or live-device visual verification is included in this snapshot.
