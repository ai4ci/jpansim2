# Scale efficiently on Isambard/Grace Hopper — reduce sequential gates and lock contention

**Status**: Conceptual

## Problem statement

JPanSim2 runs significantly slower per-timestep on Isambard Grace Hopper (72-core) nodes than on a 16-core laptop, despite the Grace node having vastly superior compute and memory bandwidth. ForkJoinPool workers are largely parked, I/O is not the bottleneck (writer threads are mostly waiting). Reducing JVM parallelism to 16 cores did not improve CPU utilisation.

The current implementation has 5 sequential gates per update step at `Updater.java:469-478`:
1. `prepareUpdate()` — sets up ephemeral state builders for outbreak and all people
2. `updateHistory()` — builds contact network + applies policy state history updates
3. `switchHistory()` — commits history lists
4. `updateState()` — applies state machine transitions + person processors
5. `switchState()` — commits new states

These gates stall all parallel work. On a 16-core laptop the gates complete quickly enough that parallelism is productive. On a 72-core Grace node with shared L2 cache between the 72 CPU cores, the gates become severe contention points — threads spend more time stalled than computing.

## Research findings

### Code-level hotspots

1. **Sequential gates with no batching or pipelining** (`Updater.java:469-478`): All 5 phases must complete before the next timestep can begin. Each phase creates a barrier where all parallel threads stop.

2. **Lock contention on state machines** (`StateMachine.java:182-202`): `performStateUpdate()` and `performOutbreakStateUpdate()` are `synchronized` methods. During the `updateState` phase all threads compete for individual person state machine locks — this is fine initially but creates contention as threads finish work at different rates.

3. **`ThreadSafeArray` synchronisation** (`ThreadSafeArray.java:170`): Every access to the people array and social network is synchronised on the element itself. With millions of contacts this creates massive lock churn.

4. **`parallelStream()` with ForkJoinPool** (`contactNetwork()`, `prepareUpdate()`, `switchHistory()`, `updateState()`): Uses the common ForkJoinPool for all parallel work. The pool may create more workers than the Grace node's cache topology can efficiently serve.

5. **`ThreadLocal<Sampler>` expansion** (`Sampler.java:46-47`): Each ForkJoinPool worker thread that is spawned gets its own `Sampler` instance with Mersenne Twister. On a 72-core node the pool may spawn hundreds of workers, each with 600+ bytes of state they almost never use.

6. **`AlwaysPreTouch` GC flag effect** (-XX:+AlwaysPreTouch): Forces all 64GB of heap to be touched at startup, causing massive page fault pressure on ARM's page table walking which is more expensive than on x86.

### Grace Hopper architecture factors

- **Shared L2 cache**: All 72 cores share a large L2 cache (~144MB). Threads spinning on monitors (synchronized blocks) consume L2 cache lines for lock metadata, evicting useful data.
- **Unified memory**: Memory addressable by CPU and GPU has higher latency than traditional NUMA. Thread migration between NUMA domains is expensive.
- **Memory-bound workload**: The simulation allocates objects per-person per-timestep (contact, exposure, history entries). This means throughput is limited by memory allocation rate and cache reuse, not CPU speed.

### What already tried

- Reducing JVM `parallelism` to 16 did not help — suggesting the problem is not just too many threads but architectural contention in the code structure.

## Success criteria

- **Primary**: Time per timestep on 72-core Grace is at least 3x faster than the current 16-core setting (current: ~16 cores performs similarly, suggesting the 72-core is actually slower or comparable in wall-clock time).
- **Secondary**: CPU utilisation in jstat shows 60%+ of available cores are active during the update cycle (currently mostly parked).
- **Tertiary**: No regression on 16-core laptop (or at most 5% slower, which would be acceptable).

## Implementation plan

### Phase 1: Diagnose and instrument (Week 1)

- [ ] **Add timing instrumentation** to all 5 gates in `Updater.update()`. Add per-gate timing to the debug log or an exportable CSV (`debug-update-timing.csv`).
- [ ] **Add lock contention metrics**: Use JVMTI or JFR to record lock wait times during simulation. Specifically track contended monitor enters in `Sampler`, `StateMachine.performStateUpdate`, and `ThreadSafeArray`.
- [ ] **Profile with JFR** on a Grace node using a short run (e.g., 100 steps). Capture: lock contention, thread park/unpark events, GC pauses, CPU utilisation per core.
- [ ] **Measure parallel stream work distribution**: Add per-thread work counts during `contactNetwork()` to see if ForkJoinPool is distributing work evenly or if some threads dominate.
- [ ] **Benchmark**: Run 500-step simulations at different `--cpus-per-task` values (2, 4, 8, 16, 32, 72) to produce a scaling curve. This confirms whether the problem is linear degradation or a threshold effect.

### Phase 2: Remove immediate bottlenecks (Week 2-3)

- [ ] **Remove `AlwaysPreTouch`**: Switch to `-XX:+UseG1GC -XX:HeapDumpOnOutOfMemoryError`. Replace with `-XX:MaxGCPauseMillis=200` and let the JVM manage page faulting. Test impact on startup time and early-step performance.

- [ ] **Lock-free Sampler seeding**: Replace `ThreadLocal.withInitial(Sampler::new)` with a seeded counter approach. Each worker thread gets a deterministic seed derived from `(workerId, globalStep)`. Use `AtomicLong` or `LongAdder` for the global counter instead of a single `ThreadLocal` seed. This eliminates per-thread object allocation during simulation runs.

- [ ] **Reduce `ThreadSafeArray` synchronization**: Replace `synchronized(element)` with `ReentrantLock` using `tryLock(10, MILLISECONDS)` in `contactNetwork()` for non-critical paths. For `updateHistory` and `updateState` the locks are necessary but can be reduced by batching: instead of acquiring the lock for each person, batch the work into chunks.

- [ ] **Replace `parallelStream()` in contact network with a bounded executor**: In `Updater.contactNetwork()` (line 204), replace `network.parallelStream().forEach(...)` with a custom `ExecutorService` using a fixed thread pool (4-8 threads) instead of the common ForkJoinPool. This isolates graph traversal parallelism from the simulation's main parallelism and prevents pool exhaustion.

### Phase 3: Structural changes to reduce gates (Week 4-6)

- [ ] **Merge `prepareUpdate` + `updateHistory`**: The `prepareUpdate` step creates ephemeral builders for all people and then `updateHistory` immediately mutates them. These can be merged into a single parallel loop if the ephemeral builder creation is done inside the update loop instead of as a separate step.

- [ ] **Replace `parallelStream()` with manual partitioning**: Instead of relying on `parallelStream()` (which uses ForkJoinPool.commonPool()), partition the people array into contiguous chunks and process each chunk in a dedicated thread. This gives precise control over parallelism and avoids ForkJoinPool overhead.

- [ ] **Batch history switching**: Instead of synchronising on each person individually in `switchHistory()`, write the next-history entries in parallel (they are already independent), then switch the references atomically. This can use a `volatile` reference on each person's history list rather than synchronized blocks.

### Phase 4: Verify and refine (Week 7)

- [ ] **Run full scaling curve** again and compare against Phase 1 baseline.
- [ ] **Validate simulation outputs** for determinism/reproducibility — the lock contention changes must not alter the stochastic outcomes in ways that invalidate scientific results.
- [ ] **Add a JMH benchmark** for the critical path: `contactNetwork()` + `updateState()` on a 10,000-person outbreak. This gives a repeatable microbenchmark to catch regressions.
- [ ] **Document results** in `design/adr/` (e.g., `006-memory-bound-optimisations.md`) to capture the decisions made and their trade-offs.

## Risks

- **Scientific correctness**: Parallel loop reordering can change the exact order of person updates, which affects the simulation outcome if the model is not designed to be order-independent. The current design uses per-person state isolation, so order should not matter for the *final* state, but `switchHistory()` and state machine locks may introduce order-dependent semantics.
- **Performance regression on x86**: The changes may inadvertently degrade performance on x86 laptops where the ForkJoinPool and synchronized blocks work well. Must validate on a 16-core x86 machine as a regression test.
- **Complexity**: Manual thread management adds code complexity. The solution should be configurable with a system property (e.g., `-Djpansim2.parallel.strategy=forkjoin|manual|hybrid`) so users can choose the best approach for their hardware.

## Testing strategy

- **Unit tests**: No direct unit test for parallelism — the bottleneck is integration-level.
- **Reference scenario**: A 1,000-person test outbreak with known RNG seed, 100 timesteps. The output CSV must match a pre-established reference after any parallelism change.
- **JMH benchmark**: Add `jpansim2-core/src/test/java/io/github/ai4ci/benchmark/UpdaterBenchmark.java` with JMH harness for `@Benchmark` of `contactNetwork()` and `updateState()` on a fixed-size outbreak.
- **Multi-node test**: CI should run this benchmark on at least two architectures (x86 and ARM) if possible.

## Design decisions to document in ADRs

- Choice of parallelism strategy (ForkJoinPool vs manual partitioning vs hybrid)
- Lock-free vs lock-based data structures for person state management
- How to handle JVM flags for ARM-specific tuning (AlwaysPreTouch, G1GC, NUMA, large pages)
