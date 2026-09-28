# Resource Booking System

REST API for booking rooms, vehicles, and equipment. Users can view available resources and manage their own reservations. Administrators can manage every resource and reservation.

## Requirements

- Java 21
- PostgreSQL
- Maven wrapper included (`mvnw`)

## Configuration

Settings live in a `.env` file in this folder. That file is listed in `.gitignore`, so it stays on your machine.

```
DB_URL=jdbc:postgresql://localhost:5432/Backend_db
DB_USERNAME=postgres
DB_PASSWORD=Kaveri@2005

JWT_SECRET=your_super_secret_key_change_me_32
JWT_EXPIRATION_MS=3600000
```

| Variable | Value in `.env` | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/Backend_db` | JDBC URL |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | set in `.env` | Database password |
| `JWT_SECRET` | set in `.env` | HMAC signing key, at least 32 characters |
| `JWT_EXPIRATION_MS` | `3600000` | Token lifetime in milliseconds (1 hour) |

The app loads `.env` from the `Backend` folder when it starts, including when the run directory is the parent folder. `JWT_SECRET` must be at least 32 characters.

Create the database before the first start:

```sql
CREATE DATABASE "Backend_db";
```

## Run

From the `Backend` folder:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

API base URL: `http://localhost:8080`

Swagger UI: `http://localhost:8080/api-docs`

## Seed accounts

Created on startup when the username is missing:

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@123` | ADMIN |
| `user` | `User@123` | USER |

Public registration always creates a USER. Send `username` and `password`. The response contains `id` and `username`.

Resources are created by an admin through `POST /resources`.

## Authentication

`POST /auth/login`

```json
{"username":"user","password":"User@123"}
```

`UserName` and `Password` are also accepted. The response contains the JWT and the username:

```json
{"token":"<jwt>","username":"user"}
```

Send the token on later requests:

```
Authorization: Bearer <token>
```

Passwords are stored with BCrypt. The API does not use server sessions.

| Situation | Status | Message |
|---|---|---|
| No token | 401 | Authentication required |
| Expired token | 401 | Token expired |
| Invalid token | 401 | Invalid token |
| Logged in, but not allowed | 403 | You do not have permission to perform this action |

`POST /auth/register` returns 201. The username is trimmed before the duplicate check.

## Authorization

| Action | USER | ADMIN |
|---|---|---|
| List and get available resources | yes | yes, including unavailable |
| Create, update, delete resources | no | yes |
| Create a reservation | yes | yes |
| List and get reservations | own only | all |
| Update and delete reservations | no | yes |

The reservation owner is the user in the JWT. A user id in the request body is ignored.

## Reservations

Status values: `PENDING`, `CONFIRMED`, `CANCELLED`. New reservations start as `PENDING`.

Price is a decimal with up to two fraction digits.

`GET /resources` and `GET /reservations` take these query parameters. In Swagger they are dropdowns or number fields, not part of the response body.

| Parameter | Meaning | Resources | Reservations |
|---|---|---|---|
| `page` | Page number, starting at 1 | yes | yes |
| `size` | Rows per page, default 10, maximum 100 | yes | yes |
| `sortBy` | Column to sort by | `id`, `name`, `type`, `price`, `available` | `id`, `price`, `status`, `startTime`, `endTime` |
| `direction` | `asc` or `desc` | yes | yes |
| `status` | Reservation status | no | optional |
| `minPrice` | Lowest price | no | optional |
| `maxPrice` | Highest price | no | optional |

Example:

```
GET /reservations?status=PENDING&minPrice=10&maxPrice=200&page=1&size=10&sortBy=price&direction=asc
```

Create:

```json
{
  "resourceId": 1,
  "startTime": "2026-11-01T09:00:00",
  "endTime": "2026-11-01T10:00:00",
  "price": 100.00
}
```

Start time must not be in the past, and end time must be after start time. The reservation price must match the resource price. A mismatch returns 400, for example `Price must match the resource price of 100.00`. Overlapping active reservations for the same resource are rejected. Validation errors return 400 with a `message` that says what to correct.

Successful responses use this shape:

```json
{"message":"Resource created successfully","data":{}}
```

`GET /reservations/my` returns only the signed-in user's reservations.

Update sends only the fields you want to change. A price-only update does not need name or type.

`createdAt` and `updatedAt` are stored on users, resources, and reservations. They are not included in API responses.

## Tests

```bash
./mvnw test
```
