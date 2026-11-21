# learn3-events

Spring application events: decouple "something happened" from "who reacts".

| Item  | Value                                |
| ----- | ------------------------------------ |
| Port  | 8080                                 |
| Needs | nothing                              |
| Run   | `./gradlew :learn3-events:bootRun`, then `curl localhost:8080/api` |

```mermaid
sequenceDiagram
  participant C as Client
  participant A as AppController
  participant P as ApplicationEventPublisher
  participant L as AppEventListener
  C->>A: GET /api
  A->>P: publishEvent(new AppEvent(...))
  P->>L: @EventListener handleAppEvent(event)
  Note over P,L: same thread, before publishEvent returns
  A-->>C: response
```

| Piece              | Class                                 |
| ------------------ | ------------------------------------- |
| Event              | `AppEvent extends ApplicationEvent`   |
| Publisher          | `ApplicationEventPublisher` injected in `AppController` |
| Listener           | `AppEventListener` with `@EventListener` |

What to try next:

| Variation                               | Effect                                                    |
| --------------------------------------- | --------------------------------------------------------- |
| `@Async` on the listener + `@EnableAsync` | the publisher returns right away; the listener runs on another thread |
| `@TransactionalEventListener(phase = AFTER_COMMIT)` | runs only if the surrounding transaction commits (send emails after saving) |
| a plain record as the event (no `ApplicationEvent`) | works the same since Spring 4.2                |
