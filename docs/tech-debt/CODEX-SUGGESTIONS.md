# CODEX Suggestions

## 1. Security issue

### Findings
- **High**: The server accepts unauthenticated plaintext connections and executes all commands without auth checks.
  - Evidence: `src/main/java/br/net/reichel/redis/Main.java:26`, `src/main/java/br/net/reichel/redis/server/RedisServer.java:37`, `src/main/java/br/net/reichel/redis/server/ClientHandler.java:48`
  - Impact: Any reachable client can read/write data and run transactions.
- **Medium**: RESP parsing has no hard limits for element counts/frame sizes.
  - Evidence: `src/main/java/br/net/reichel/redis/protocol/RespParser.java:30-35`
  - Impact: Large crafted frames can trigger memory pressure/OOM (DoS).

### Suggested actions
- Add `AUTH`/authorization checks before command dispatch.
- Add TLS or restrict bind interface/network exposure.
- Enforce strict parser limits (max array len, bulk len, frame size) and drop abusive clients.

## 2. Code quality

### Findings
- **High**: Many command handlers index arguments directly (`args[1]`, `args[2]`) without validating arity first.
  - Evidence: `src/main/java/br/net/reichel/redis/command/impl/SetCommandHandler.java:29-42`, `GetCommandHandler.java:29-33`, `LrangeCommandHandler.java:31-37`
  - Impact: malformed input leads to exceptions instead of stable RESP errors.
- **Medium**: `ClientHandler` mixes socket I/O, transaction state management, command dispatch, and response assembly.
  - Evidence: `src/main/java/br/net/reichel/redis/server/ClientHandler.java:40-97`
  - Impact: high coupling and harder reasoning/testing.
- **Low**: RESP response assembly is duplicated via manual `StringBuilder` logic in blocking handlers.
  - Evidence: `src/main/java/br/net/reichel/redis/command/impl/BlpopCommandHandler.java:66+`, `XreadCommandHandler.java`
  - Impact: repeated formatting code and inconsistency risk.

### Suggested actions
- Add shared argument validation and standard `ERR wrong number of arguments` responses.
- Extract transaction/command execution from `ClientHandler` into a dedicated executor.
- Reuse/expand `RespEncoder` helpers for array/bulk response construction.

## 3. Bugs

### Findings
- **High**: List operations are not consistently synchronized; pops/reads race with pushes on `ArrayList`.
  - Evidence: `src/main/java/br/net/reichel/redis/store/impl/InMemoryDataStore.java:61-120`
  - Impact: inconsistent list state, runtime exceptions, potential element loss.
- **Medium**: `XREAD` option parsing is incomplete (`COUNT`/`BLOCK` ordering and usage).
  - Evidence: `src/main/java/br/net/reichel/redis/command/impl/XreadCommandHandler.java:38-118`
  - Impact: valid argument combinations behave incorrectly; `COUNT` not enforced.
- **Medium**: `TYPE` ignores expiration checks and can report stale type info.
  - Evidence: `src/main/java/br/net/reichel/redis/store/impl/InMemoryDataStore.java:53-59`
  - Impact: `GET` can return nil while `TYPE` returns `string` for the same expired key.
- **Medium**: `EXEC` builds response through `byte[] -> String -> byte[]` conversion.
  - Evidence: `src/main/java/br/net/reichel/redis/server/ClientHandler.java:85-97`
  - Impact: binary response framing/data corruption risk.

### Suggested actions
- Unify synchronization strategy for all list read/write paths.
- Rewrite `XREAD` argument parser as a proper option loop and enforce `COUNT`.
- Make `type()` consult expiration logic before returning type.
- Assemble `EXEC` responses at byte level (no intermediate String decoding).

## 4. Race

### Findings
- **High**: `lpop`/`lpopN` mutate per-key lists without the same lock used by `lpush`/`rpush`.
  - Evidence: `src/main/java/br/net/reichel/redis/store/impl/InMemoryDataStore.java:105-118` vs `:61-85`
  - Impact: data races and non-thread-safe `ArrayList` mutation under concurrent clients.
- **Medium**: Stream list access (`appendStreamEntry`, `xrange`, `xreadAfter`, `lastStreamEntry`) is unsynchronized.
  - Evidence: `src/main/java/br/net/reichel/redis/store/impl/InMemoryDataStore.java:141-185`
  - Impact: concurrent append/read can cause inconsistent reads or fail-fast iteration exceptions.

### Suggested actions
- Lock all list operations (reads and writes) on the same per-key lock or use thread-safe deques.
- Add synchronization strategy for stream operations (per-stream lock or concurrent collection + snapshot strategy).

## 5. Test flakiness

### Findings
- **High**: Integration tests depend on fixed sleeps (`Thread.sleep(200)`) to establish blocking state.
  - Evidence: `src/test/java/br/net/reichel/redis/integration/BlpopIntegrationTest.java:48`, `XreadIntegrationTest.java:59`
  - Impact: scheduler/CI timing variance causes intermittent failures.
- **Moderate**: Expiry tests rely on short TTL + short sleeps (`50-100ms` + `100-150ms`).
  - Evidence: `src/test/java/br/net/reichel/redis/store/impl/InMemoryDataStoreTest.java:51`, `.../GetCommandHandlerTest.java:54`, `.../TypeCommandHandlerTest.java:78`, `.../SetCommandHandlerTest.java:78`
  - Impact: clock/scheduler jitter causes nondeterministic pass/fail.
- **Low**: Cleanup waits use fixed sleep after socket close.
  - Evidence: `src/test/java/br/net/reichel/redis/integration/ClientHandlerIntegrationTest.java:111`
  - Impact: cleanup completion is assumed, not observed.

### Suggested actions
- Replace sleeps with latches/handshakes that signal actual blocking state.
- Inject controllable clock or poll with timeout instead of fixed TTL waits.
- Join/wait on explicit server shutdown signal instead of sleep-based cleanup.

## 6. Maintainability of the code

### Findings
- **High**: Locking policy is inconsistent and implicit across list/stream APIs in datastore.
  - Evidence: `src/main/java/br/net/reichel/redis/store/impl/InMemoryDataStore.java`
  - Impact: fragile behavior and hard-to-extend concurrency model.
- **Medium**: `ClientHandler` has too many responsibilities and is difficult to evolve safely.
  - Evidence: `src/main/java/br/net/reichel/redis/server/ClientHandler.java`
  - Impact: changes to protocol/transactions risk regressions in socket handling.
- **Low**: Response encoding logic is duplicated across handlers.
  - Evidence: `src/main/java/br/net/reichel/redis/command/impl/BlpopCommandHandler.java`, `XreadCommandHandler.java`
  - Impact: format drift and extra maintenance overhead.

### Suggested actions
- Encapsulate concurrency policy inside datastore primitives (single lock discipline per key/type).
- Split I/O from command execution and transaction orchestration.
- Centralize RESP formatting helpers and reuse them across handlers.

## Consolidated priority order
1. Fix datastore concurrency races (list + stream) and add multithreaded tests.
2. Add command arity validation and return RESP errors for malformed requests.
3. Harden server security boundary (auth + parser limits + transport/binding restrictions).
4. Correct `XREAD` option semantics and `TYPE` expiration consistency.
5. Remove timing-based sleeps from tests in favor of deterministic synchronization.
