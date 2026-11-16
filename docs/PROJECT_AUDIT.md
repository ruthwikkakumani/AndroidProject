# Project audit

Audit date: 17 November 2026. Repository: `Androidd Projectt`. This was not a Git repository when the audit started.

## What is actually in the tree

The project is a single-module Kotlin app, application id `com.findit.campus`, branded FindIt Campus. There is no file named `ApiClientTest.kt`, no `LostFound` module, and no client for `http://10.0.2.2:8787`. Those were not present to preserve.

There is no Firebase configuration, no remote API client, and no mail sender.

## What was working before this pass

- Jetpack Compose UI for a local lost-and-found board.
- Room database for categories and items, with a 1-to-2 migration that adds `dominantColor`.
- On-device match scoring (category, words, place name, distance, color, recency).
- On-device Latin text recognition from a camera photo (ML Kit).
- Camera capture and an optional gallery pick.
- Optional GPS pin and reverse geocoding when the user asks for it.
- Search, lost/found filters, item detail, reunite, and delete.
- 12 unit tests and 3 device UI tests had been executed successfully on that local board.

## Defects found

| ID | Severity | Issue |
|---|---|---|
| BUG-001 | P1 | Campus geometry was hardcoded to 37.422094, -122.083922 (Mountain View / Googleplex) and the home subtitle was hardcoded as Riverside Campus. A device at that point, or a geocoder, can show an address such as 1600 Mountain View as if it were the student's campus. |
| BUG-002 | P1 | Launch inserted fictional lost-and-found reports at those coordinates and presented them as the live board. |
| BUG-003 | P1 | Opening a report requested location automatically and snapped the place name to the nearest hardcoded zone. |
| BUG-004 | P1 | No accounts. Anyone opening the app shared one unsigned board. |
| BUG-005 | P2 | Found-item private details, claims, notifications, and saved items were not implemented. |
| BUG-006 | P2 | There is no remote backend, so accounts, messages, and notifications cannot leave this device. |

## Not found

Email verification, password-reset mail, multi-device chat, admin roles, Firebase, and a network API. They cannot be marked complete.
