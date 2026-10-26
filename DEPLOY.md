# Running FarmConnect on a real server

1. **Server size.** At least 2 CPU cores and 2 GB RAM (4 cores is better). Logins are CPU heavy because passwords are hashed on purpose.
2. **Database.** Create a PostgreSQL database. Do not use the H2 file database in production.
3. **Environment variables** (set these on the host, never commit them):
   `SPRING_PROFILES_ACTIVE=prod`, `DATABASE_URL=jdbc:postgresql://HOST:5432/farmconnect`, `DATABASE_USER`, `DATABASE_PASSWORD`,
   `JWT_SECRET` (random, 48+ characters), `CRYPTO_KEY` (random, keep it forever), `ADMIN_EMAIL`, `ADMIN_PASSWORD` (a strong one),
   plus the M-Pesa and Firebase values you already use (`MPESA_CONSUMER_KEY`, `MPESA_CONSUMER_SECRET`, `MPESA_PASSKEY`, ...).
4. **HTTPS.** Put the server behind HTTPS (the host or a proxy such as Caddy/nginx does this). The app expects it.
5. **Java options** (example for 2 GB): `-Xms768m -Xmx1280m`.
6. **Backups (built in).** See "Backups, alerts and monitoring" below.
7. **Load test before launch:** see `loadtest/login-2000.js`.

## Backups, alerts and monitoring

**What runs by itself.** Every night at 02:30 the server saves (1) the whole database and (2) a zip of all uploaded photos and documents
into `BACKUP_DIR` (default `/var/backups/farmconnect`). The newest 14 copies of each are kept (`BACKUP_KEEP`). After a PostgreSQL dump the
server runs `pg_restore --list` on it, which proves the file is a readable backup. The super admin console has a **System health** page
with the database state, free disk, memory, the last good backup and a **Back up now** button.

**What you must install.** The server needs the PostgreSQL client tools, in the same major version as your database:
`sudo apt install postgresql-client` (gives `pg_dump` and `pg_restore`). Make sure `BACKUP_DIR` exists and the app's user can write to it.

**Keep a second copy somewhere else.** A backup on the same disk does not help if the disk or server is lost. Either set
`BACKUP_COPY_TO` to a different mounted disk, or copy the folder off the server every day (for example `rclone` to Google Drive or S3).
Once a quarter, restore a backup on a spare machine to prove it works.

**Restore (PostgreSQL).**
```
createdb farmconnect_restored
pg_restore -h HOST -U USER -d farmconnect_restored --no-owner farmconnect-db-YYYYMMDD-HHMM.dump
unzip farmconnect-uploads-YYYYMMDD-HHMM.zip -d /path/to/uploads
```
Start the server pointing `DATABASE_URL` at the restored database and `APP_UPLOAD_DIR` at the restored uploads folder.
Keep the same `CRYPTO_KEY` as before: ID numbers and KRA PINs are encrypted with it and cannot be read without it.

**Alerts.** The super admin is emailed (the server needs working mail settings) when a backup fails, no good backup exists for 36 hours,
the disk is nearly full (under 1 GB or 10% free), the database stops answering, or 200 or more logins fail within 10 minutes
(`ALERT_FAILED_LOGINS`). The same alert is not repeated within 6 hours.

**Uptime monitoring.** `GET https://YOUR-SERVER/api/health` returns 200 with `{"status":"UP"}`, or 503 when the database is down.
Add it to a free checker such as UptimeRobot or Better Stack (check every 5 minutes, alert by email or SMS to you).
For crash and error logs, keep the application log (`journalctl -u farmconnect` or your host's log page) and look at it after any alert.
