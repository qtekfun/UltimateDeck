## Goal
Ship the **offline** sync for _boards_, with ~~no~~ zero data loss.

### Tasks
- [x] Design the operation queue (see [spec](https://example.org/spec))
- [ ] Write `ConflictResolver`
- [ ] Review with Ana
  - [ ] Nested follow-up

| Case  | Rule            |
|-------|-----------------|
| title | ask the user    |
| due   | last write wins |

```json
{"etag": "abc123"}
```

> Keep the server as the source of truth.

Plain paragraph with __underscore strong__ and *star emphasis*.
