---
description: Start a new feature
agent: build
---

### /new-feature
Use at the start of any new feature.
Run with the @architect agent first, then hand the output to the @developer agent.

**I need to implement:** $ARGUMENTS

Please produce:
1. The API contract (endpoint, request DTO, response DTO, status codes)
2. The service interface (method signatures only)
3. Any new entities or entity changes needed
4. Any cross-cutting concerns to consider (auth, validation, async, etc.)
