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
