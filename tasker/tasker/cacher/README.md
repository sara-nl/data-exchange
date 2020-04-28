# Cacher

Cacher utilizes active read-through technique with async adaptive update interval.

## How it works

When started, Cacher downloads all Research Drive shares into memory and exposes them via REST interface `GET /shares/all`). It keeps updating the shares in the background with the timeout, which grows according to the exponential backoff algorithm.

When a new request hits the service, the update interval gets reset to its minimal value (configurable via `cacher.initialUpdateInterval`).  At the same time the update attempts are guaranteed to happen at least as often as configured in `cacher.update.ceilingInterval`.

  