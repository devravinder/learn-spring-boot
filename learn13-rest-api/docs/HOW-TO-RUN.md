# How to run learn13-rest-api

No containers are needed.

## rest1-validation-problem-details

```bash
./gradlew :learn13-rest-api:rest1-validation-problem-details:bootRun
```

| Request                                                    | Expected                                     |
| ---------------------------------------------------------- | -------------------------------------------- |
| valid `POST /users`                                        | 201 with `Location: /users/1`                |
| `POST /users` with bad fields                              | 400 `application/problem+json`, `errors[]` per field |
| the same with `Accept-Language: fr`                        | French messages                              |
| the same email twice                                       | 409 `duplicate-email` problem                |
| `GET /users/99`                                            | 404 with `userId: 99`                        |
| `GET /users/0`, `GET /users?limit=500`                     | 400 method validation                        |
| `DELETE /users/1`                                          | 405 problem                                  |
| `POST /users` with `text/plain`                            | 415 problem                                  |
| `GET /users/boom`                                          | 500 with an `errorId`; the cause appears only in the log |

All requests: [../http/rest1-problems.http](../http/rest1-problems.http).

## rest2-api-versioning

```bash
./gradlew :learn13-rest-api:rest2-api-versioning:bootRun
curl -si localhost:8142/products/1                         # v1 + Deprecation/Sunset/Link
curl -s  localhost:8142/products/1 -H 'API-Version: 2.0'   # v2 shape
curl -s  'localhost:8142/products/1?version=2.0'           # same, as a query parameter
curl -si localhost:8142/products -H 'API-Version: 1.0'     # 404: added in 1.1
curl -s  localhost:8142/products/1 -H 'API-Version: 3.0'   # 400 problem
```

## rest3-http-interface-clients

```bash
./gradlew :learn13-rest-api:rest3-http-interface-clients:bootRun
curl -s localhost:8143/shop/products/1
curl -s 'localhost:8143/shop/products?q=book&limit=1'
curl -s -X POST localhost:8143/shop/products -H 'Content-Type: application/json' -d '{"name":"Pen","price":20}'
curl -s localhost:8143/shop/echo              # headers the client sent: API-Version 2.0, X-Client rest3
curl -s localhost:8143/shop/products/99       # upstream 404 -> our 404 problem
curl -s 'localhost:8143/shop/slow?ms=2000'    # 504 after the 1 s read timeout
```

## rest4-openapi

```bash
./gradlew :learn13-rest-api:rest4-openapi:bootRun
```

| Open                                         | What                                  |
| -------------------------------------------- | ------------------------------------- |
| <http://localhost:8144/docs>                 | Swagger UI; pick the group top right  |
| <http://localhost:8144/v3/api-docs>          | the full OpenAPI 3 JSON               |
| <http://localhost:8144/v3/api-docs.yaml>     | the same as YAML                      |
| <http://localhost:8144/v3/api-docs/public>   | books only                            |
| <http://localhost:8144/v3/api-docs/admin>    | admin only                            |

In Swagger UI, "Authorize" accepts any bearer token. The scheme is documented
but not enforced in this module.
