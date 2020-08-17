# Shares service

Shares service utilizes active caching technique for reducing load on storage providers (esp. OwnCloud) and trading up some consistence for performance.

## How it works

When started, downloads all shares into memory and exposes them via REST interface `GET /shares/all`). It keeps syncing shares in the background with the period, which grows according to the exponential backoff algorithm until it reaches `cacher.update.ceilingInterval`; from that point on it remains constant.