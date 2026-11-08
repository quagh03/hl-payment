# US-XXX: <Story title>

| | |
|---|---|
| Feature | [FT-XXX](../features/FT-XXX-slug.md) |
| Status | Backlog / Ready / In progress / Done |
| Phase | 0–4 |
| Estimate | S / M / L |
| Last updated | YYYY-MM-DD |

## Story

As a **<persona>**, I want **<capability>** so that **<outcome>**.

## Acceptance criteria

```gherkin
Scenario: <happy path>
  Given ...
  When ...
  Then ...

Scenario: <failure or edge case>
  Given ...
  When ...
  Then ...
```

## Notes

- API: `POST /v1/...`
- Events: `...`
- Error codes: `...`

## Definition of done

- [ ] Acceptance scenarios automated
- [ ] OpenAPI / event schema updated in `contracts/`
- [ ] Observability: metrics/logs/traces for the new path
- [ ] Docs updated (feature, glossary if new terms)
