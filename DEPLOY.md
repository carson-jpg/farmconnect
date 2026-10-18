# Running FarmConnect on a real server

1. **Server size.** At least 2 CPU cores and 2 GB RAM (4 cores is better). Logins are CPU heavy because passwords are hashed on purpose.
2. **Database.** Create a PostgreSQL database. Do not use the H2 file database in production.
3. **Environment variables** (set these on the host, never commit them):
   `SPRING_PROFILES_ACTIVE=prod`, `DATABASE_URL=jdbc:postgresql://HOST:5432/farmconnect`, `DATABASE_USER`, `DATABASE_PASSWORD`,
   `JWT_SECRET` (random, 48+ characters), `CRYPTO_KEY` (random, keep it forever), `ADMIN_EMAIL`, `ADMIN_PASSWORD` (a strong one),
   plus the M-Pesa and Firebase values you already use (`MPESA_CONSUMER_KEY`, `MPESA_CONSUMER_SECRET`, `MPESA_PASSKEY`, ...).
4. **HTTPS.** Put the server behind HTTPS (the host or a proxy such as Caddy/nginx does this). The app expects it.
5. **Java options** (example for 2 GB): `-Xms768m -Xmx1280m`.
6. **Back up the database** daily.
7. **Load test before launch:** see `loadtest/login-2000.js`.
