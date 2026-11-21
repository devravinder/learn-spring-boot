# How to run learn14-data-jpa

No containers are needed; H2 runs in memory and is seeded at startup.

```bash
./gradlew :learn14-data-jpa:data1-jpa-essentials:bootRun
```

| Try                                                 | Look at                                      |
| --------------------------------------------------- | -------------------------------------------- |
| `curl -s localhost:8151/queries/n-plus-one`         | `sqlCount: 21`                               |
| `curl -s localhost:8151/queries/fetch-join`         | `sqlCount: 1` and the `join` in `sql`        |
| `curl -s 'localhost:8151/queries/projection?maxPrice=200'` | `sql` selects only title and price    |
| `curl -s 'localhost:8151/books/offset?page=2&size=10'` | two statements, one is `count(...)`       |
| `curl -s 'localhost:8151/books/slice?page=2&size=10'` | one statement, `hasNext`                   |
| `curl -s 'localhost:8151/books/cursor?size=10'`     | `nextCursor`; pass it back as `after=`       |
| `curl -s -X POST 'localhost:8151/books?title=Cheap&price=1'`, then page again | offset pages shift, cursor pages don't |
| `curl -s -X PUT 'localhost:8151/books/5/price?price=999'` | `version` +1 and `updatedAt` changes  |
| `curl -s -X POST 'localhost:8151/orders?bookId=20&fail=true'` | stock unchanged, `/audit` has a row  |
| `curl -s -X POST 'localhost:8151/orders?bookId=20&mode=checked-default'` | stock changed although the call failed |

Race 20 requests (bash):

```bash
curl -s -X POST 'localhost:8151/stock/10/reset?to=20'
seq 20 | xargs -P 20 -I{} curl -s -o /dev/null -w '%{http_code}\n' -X POST localhost:8151/stock/10/optimistic | sort | uniq -c
curl -s localhost:8151/books/10    # stock = 20 - number of 200s
```

Replace `optimistic` with `pessimistic` or `atomic` to see all 20 succeed.
