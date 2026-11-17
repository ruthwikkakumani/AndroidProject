# Fixes and remaining checks

Fixed startup authentication appearing before a saved session resolved; report routing that mixed lost/found actions; inbox navigation to the wrong recipient; direct reunion without both parties; missing private evidence flow; stale map marker coordinates; camera lifecycle/permission continuation; large image decoding; and inconsistent screen spacing/styles.

Fixed a live-map cold-start crash by waiting for native map readiness before creating camera updates. The regression check loaded real Google tiles with the configured key.

Test runs now retain the installed app. Uninstalling/clearing data still removes local accounts by Android design.

Remaining external/device checks are listed in [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md). No claim is made that arbitrary bugs, untested devices or live cloud behavior are fully validated.
