# Coffio Server (Optional)

This folder is optional.
Use it only if you want LAN sync between multiple devices.

## Start

```bash
docker compose up -d --build
```

Services:

- PostgreSQL: localhost:5432
- Sync API: localhost:8000
- Health check: GET /api/v1/health

## Android App Setup

1. Open Settings in the app.
2. In Sync, set your server IP/host (example: 192.168.1.10:8000).
3. Enable Sync.
4. Brews sync automatically after save.
5. You can still trigger manual sync with Sync Now.

## Notes

- All devices must be in the same local network.
- Allow ports 8000 and 5432 in firewall when needed.
- API merges coffee/sieve/drink by name.
- API deduplicates brews by content signature.
