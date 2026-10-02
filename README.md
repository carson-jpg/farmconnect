# FarmConnect Backend (Spring Boot 3, Java 17)

## Run
1. Install JDK 17+ and Maven (or open the folder in IntelliJ).
2. `cd farmconnect-backend`
3. `mvn spring-boot:run`
4. API: http://localhost:8080  (Android emulator: http://10.0.2.2:8080)
5. H2 console: http://localhost:8080/h2-console  (JDBC URL `jdbc:h2:file:./data/farmconnect`, user `sa`, empty password)

## Endpoints (send `Authorization: Bearer <token>` except public ones)
| Method | Path | Who |
|---|---|---|
| POST | /api/auth/register | public (role: FARMER or BUYER) |
| POST | /api/auth/login | public |
| GET | /api/auth/me | any user |
| GET | /api/farms, /api/farms/{id} | public |
| GET | /api/farms/mine | FARMER |
| POST, PUT, DELETE | /api/farms, /api/farms/{id} | FARMER (owner) |
| GET | /api/products?q=&category=&farmId= , /api/products/{id} | public |
| POST, PUT, DELETE | /api/products, /api/products/{id} | FARMER (owner) |
| GET, POST, DELETE | /api/cart | BUYER |
| PUT, DELETE | /api/cart/{itemId} | BUYER |
| GET | /api/wishlist | BUYER |
| POST, DELETE | /api/wishlist/{productId} | BUYER |
| POST | /api/orders  `{"deliveryAddress":"..."}` | BUYER (from cart) |
| GET | /api/orders | BUYER |
| PATCH | /api/orders/{id}/cancel | BUYER |
| GET | /api/orders/farmer | FARMER |
| PATCH | /api/orders/{id}/status `{"status":"SHIPPED"}` | FARMER |

## Quick test
```
curl -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" \
 -d '{"name":"Wanjiku","email":"farmer@test.com","password":"secret1","role":"FARMER"}'
```
Copy the `token`, then:
```
curl -X POST localhost:8080/api/farms -H "Content-Type: application/json" -H "Authorization: Bearer TOKEN" \
 -d '{"name":"Green Acres","location":"Nakuru","description":"Veggies"}'
```

Before deploying: change `app.jwt.secret` in application.properties.
