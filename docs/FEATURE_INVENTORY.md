# Feature inventory

Status words: Working, Partial, Missing.

| Feature | Status | Evidence |
|---|---|---|
| FindIt Campus branding | Working | Emulator welcome and registration screens |
| Local registration | Working | Emulator created "Ada Lovelace" / North Campus and opened the home board |
| Local login and logout | Working | Emulator logout returned to login; login restored the same home greeting |
| Password rules and hashing | Working | Unit tests for length, mismatch, PBKDF2 verify |
| Campus name chosen by the user | Working | Home showed "North Campus", not a preset city |
| Report without a GPS pin | Working | Validation unit test accepts a place name with no coordinates |
| Optional GPS pin | Partial | Code path exists; not re-run on the emulator in this pass |
| Empty board instead of fake reports | Working | Home screenshot: "No open reports yet" |
| Search and lost/found filters | Partial | Previous device UI test plus current code; not re-exercised after auth |
| On-device match scoring | Working | Unit tests still pass (14 tests) |
| On-device text reading | Partial | Camera preview was verified earlier; OCR was not re-run this pass |
| Private ownership check | Partial | Hash compare is unit-tested; the claim button is not on the detail screen yet |
| Match notifications | Partial | Rows are written when a report is saved; the notification screen is not built |
| Saved items, chat, admin, email reset | Missing | No remote service and no mail transport in this project |
| Forgot password | Partial | Screen explains that email reset is not configured and does not sign anyone in |
