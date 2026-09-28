---
name: implement-feature
description: Implement a requested feature in the real-time fraud detection project using the existing architecture, with minimal file changes, tests, and build verification. Use when implementing or modifying project functionality.
---

# Implement Feature

## Goal

Implement exactly the requested feature in the existing project without expanding the scope unnecessarily.

## Procedure

### Step 1 - Inspect

Before changing anything:

- inspect the repository structure
- inspect the relevant module
- inspect its pom.xml
- inspect existing source code
- inspect current git status if relevant

Do not assume files or architecture that have not been inspected.

### Step 2 - Plan

Create a short implementation plan containing:

- files to modify
- files to create
- required dependencies
- how the feature will work
- how it will be tested

Keep the plan minimal.

### Step 3 - Implement

Implement the feature.

Rules:

- reuse existing classes where appropriate
- do not duplicate existing logic
- do not introduce unnecessary abstractions
- do not add unrelated refactoring
- keep responsibilities aligned with the architecture
- keep business logic out of common unless genuinely shared

### Step 4 - Verify

Run the smallest relevant verification first.

Examples:

- Maven compile
- Maven test
- focused unit test
- application startup
- REST API test

Then run the broader build if appropriate.

### Step 5 - Fix

If compilation or tests fail:

1. inspect the actual error
2. fix the root cause
3. rerun the relevant verification

Do not hide or ignore build failures.

### Step 6 - Report

At the end report:

- files created
- files modified
- dependencies added
- behavior implemented
- commands/tests run
- verification result
- known limitations

Do not claim something was tested if it was not actually tested.

## Scope Control

Never proactively implement future milestones.

For example:

If asked to implement the transaction API, do NOT also implement:

- Kafka
- Redis
- ML
- dashboard
- Docker
- fraud rules

unless explicitly requested.

## Project-Specific Rules

Follow the workspace architecture rules.

For this project:

- tx-simulator creates transactions
- Kafka carries transaction events
- feature-service calculates features
- Redis stores current feature state
- decision-service makes the final decision
- dashboard observes the system