---
name: code-review
description: Use when user asks to perform code reviews
---

**Target to code review:** $ARGUMENTS

## Review Checklist
Focus your critical analysis on:
1. Logical mistakes that could cause errors or unexpected behavior.
2. Unaccounted-for edge cases and missing error handling.
3. Performance bottlenecks or scalability issues.
4. Security vulnerabilities (e.g., injection risks, unvalidated inputs).
5. Hard-to-understand code that needs better documentation.
6. Verify test coverage of the actual requirements and edge cases. 

## Feedback Guidelines
Format your response as follows:
- [CRITICALITY: High/Medium/Low]
- File & Line Number
- Issue: What is the problem?
- Solution: How would you fix it? (Include refactored code if applicable)
