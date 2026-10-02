## Goal
Ship the **offline** sync for _boards_.

### Tasks
- [x] Design the queue (see [spec](https://example.org/spec))
- [ ] Write `ConflictResolver`
- [ ] Review with @ana

| Case | Rule |
|------|------|
| title | ask |
| due | last write wins |

```json
{"etag": "abc"}
```

> Note: keep the server as source of truth.

---
Last edited by Luis