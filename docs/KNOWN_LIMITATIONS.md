# Known limitations

- Accounts exist only in this app's database. A second phone cannot see them.
- Forgot password cannot send email. The screen says so and does not pretend otherwise.
- Email addresses are stored and checked for shape. They are not verified with a mailbox.
- Match notifications are saved in Room and are not shown in an inbox yet.
- The claim comparison works in code and unit tests. The detail screen does not yet collect the private answer.
- Google Maps is used only when `MAPS_API_KEY` is set in `local.properties`. The default map does not invent a campus.
- Destructive migration from database version 2 drops the old sample board. That board was fictional.
