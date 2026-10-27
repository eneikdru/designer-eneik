# ADR-003 - Autonomous Sovereign Designer Engine Architecture and Interaction Boundaries

## Status
ACCEPTED

## Context and Problem Statement
The Autonomous Sovereign Designer Engine generates production-grade UI/UX mockups (`mockup.html`) adhering to million-dollar design standards (Stripe, Linear, Apple, Vercel, Mercury) without external paid dependencies.

The engine requires coordination between 6 core functional components:
1. `DesignTokenCompiler` - Product archetype classification, adaptive typography (`clamp(...)`), APCA/WCAG AAA contrast calculations (>= 7:1 ratio).
2. `LayoutPatternRegistry` - Compositional grids (Swiss editorial asymmetry 7/5 & 8/4, Bento Grid 2.0, multi-layered obsidian palette `#08090C` -> `#11141C` with `border-white/[0.07]`).
3. `SemanticLayoutSynthesizer` - Semantic HTML5/Tailwind DOM generator supporting project media assets and keyboard accessibility (`focus-visible`).
4. `PlaywrightVisionHarness` - Hardware screenshot rendering harness capturing dual viewports: 1440px desktop and 375px mobile.
5. `VisionAuditLoop` - Visual quality gate evaluation (zero Leica/GPS noise, zero 375px horizontal scroll) with iterative correction feedback.
6. `SovereignDesignSynthesisService` - Orchestration pipeline integrating synthesis and connecting to `DesignAssetService`.

Without explicit interaction boundaries and data contracts, the feedback loop between visual rendering (`PlaywrightVisionHarness`), visual audit (`VisionAuditLoop`), and DOM regeneration (`SemanticLayoutSynthesizer`) risks circular dependencies and infinite feedback loops.

## Decision Drivers
- **Acyclic Dependency Architecture**: Strict directed acyclic data flow across compilation, synthesis, rendering, and auditing phases.
- **Deterministic Loop Control**: Strict upper bound (`MAX_AUDIT_ROUNDS = 3`) and monotonic score thresholds to guarantee termination without infinite correction loops.
- **Explicit Data Contracts**: Typed, immutable value objects for phase transitions (`DesignTokenSet`, `LayoutGridPattern`, `SynthesizedDomDocument`, `ViewportCapture`, `VisionAuditReport`, `CorrectionDirective`).
- **Deontic Quality Gates**: Hard rejection if contrast < 7:1, horizontal scroll exists at 375px mobile viewport, or keyboard `focus-visible` accessibility is missing.

## Architecture & Bounded Context Map

```mermaid
graph TD
    Client[Client Application / DesignAssetService] -->|Request Synthesis| Service[SovereignDesignSynthesisService]

    subgraph Token & Layout Context
        Compiler[DesignTokenCompiler]
        Registry[LayoutPatternRegistry]
    end

    subgraph DOM Synthesis Context
        Synthesizer[SemanticLayoutSynthesizer]
    end

    subgraph Vision & Audit Context
        Harness[PlaywrightVisionHarness]
        Audit[VisionAuditLoop]
    end

    Service -->|1. Compile Tokens| Compiler
    Compiler -->|DesignTokenSet| Service

    Service -->|2. Select Grid| Registry
    Registry -->|LayoutGridPattern| Service

    Service -->|3. Synthesize DOM| Synthesizer
    Synthesizer -->|SynthesizedDomDocument| Service

    Service -->|4. Render Captures| Harness
    Harness -->|ViewportCaptures| Service

    Service -->|5. Evaluate Quality| Audit
    Audit -->|VisionAuditReport| Service

    Service -->|6. Refine DOM (if r < 3 & score < 9.2)| Synthesizer
```

## Service Interactions and Data Contracts

### 1. Design Token Compilation Phase
- **Input**: `ProductArchetype` (DEV_TOOLS, FIN_TECH, EDITORIAL, CONSUMER), primary color preferences.
- **Output**: `DesignTokenSet`
  - `typographyClamps`: Map of adaptive typography rules calculated via `clamp(...)`.
  - `colorPalette`: Multi-layered obsidian scale (`#08090C`, `#11141C`) with 1px border tokens (`border-white/[0.07]`).
  - `contrastRatios`: APCA / WCAG AAA contrast guarantees (minimum 7:1 contrast against dark background).

### 2. Layout Pattern Selection Phase
- **Input**: `ProductArchetype`, content density spec.
- **Output**: `LayoutGridPattern`
  - `patternType`: Swiss Editorial (7/5, 8/4 asymmetric) or Bento Grid 2.0.
  - `gridTemplateCss`: Clean Tailwind layout grid classes.

### 3. Semantic Layout Synthesis Phase
- **Input**: `DesignTokenSet`, `LayoutGridPattern`, asset references from `DesignAssetService`, optional `CorrectionDirective` (when in refinement round `r > 0`).
- **Output**: `SynthesizedDomDocument`
  - `htmlContent`: Valid semantic HTML5 string with Tailwind CSS utility classes and `focus-visible` accessibility styles.
  - `roundIndex`: Iteration round (0 to 3).
  - `domFingerprint`: Deterministic hash of the generated HTML.

### 4. Hardware Vision Rendering Phase
- **Input**: `SynthesizedDomDocument`
- **Output**: `List<ViewportCapture>`
  - `viewport`: Desktop (1440px x 900px) or Mobile (375px x 812px).
  - `screenshotImage`: Captured image payload / hash.
  - `scrollWidthPx`: Measured document scroll width (must equal viewport width to pass 375px mobile gate).

### 5. Vision Audit Evaluation Phase
- **Input**: `SynthesizedDomDocument`, `List<ViewportCapture>`
- **Output**: `VisionAuditReport`
  - `overallScore`: double precision score (0.0 to 10.0, target >= 9.2).
  - `deonticGatePassed`: boolean indicating zero Leica noise, zero 375px horizontal scroll, and WCAG AAA compliance.
  - `correctionDirectives`: List of targeted CSS selector and token adjustments for the next iteration.

## Deterministic Loop Termination Mechanism

To prevent infinite loops during iterative visual correction:

1. **Upper Bound Limit**:
   - `MAX_AUDIT_ROUNDS` is fixed at `3`.
   - Iteration index `round` starts at `0` and increments monotonically: `round = 0, 1, 2, 3`.

2. **Terminal State Transitions**:
   - **APPROVED**: If `visionAuditReport.isApproved()` (`score >= 9.2` AND `deonticGatePassed == true`), session transitions immediately to `COMPLETED_APPROVED`.
   - **FALLBACK_ACCEPTANCE**: If `round == MAX_AUDIT_ROUNDS` and `score < 9.2`, session transitions to `COMPLETED_FALLBACK` with recorded diagnostic logs. Further refinement loops are strictly prohibited.
   - **FAILURE**: If any step encounters an unrecoverable structural exception, session transitions to `FAILED`.

3. **Atomic State Guard (Compare-And-Swap)**:
   All state transitions in `SynthesisSession` are guarded by Compare-And-Swap check-and-set logic to prevent concurrent or invalid state transitions.

```
       [INITIALIZED]
             │
             ▼
      [SYNTHESIZING] ───► [AUDITING]
             ▲                  │
             │ (round < 3       │ (score >= 9.2 & gates pass)
             │  & refine)       ├─────────────────────────────► [COMPLETED_APPROVED]
             │                  │
      [REFINING] ───────────────┤ (round == 3 & score < 9.2)
                                └─────────────────────────────► [COMPLETED_FALLBACK]
```

## Implementation Owners & Next Deliverables

| Component / Boundary | Role Responsible | Deliverable Description |
|----------------------|------------------|-------------------------|
| Architecture & Boundaries | `BARCAN-TAG-01` | Service interaction contracts, domain entities, `SynthesisSession` state machine, ADR-003. |
| REST API Contracts | `BARCAN-TAG-02` | OpenAPI specification and HTTP controller endpoints for `SovereignDesignSynthesisService`. |
| Semantic DOM Synthesizer | `BARCAN-TAG-09` | HTML5/Tailwind DOM generator, Swiss 7/5 & 8/4 grid templates, Bento Grid 2.0. |
| Vision Audit & Rendering | `BARCAN-TAG-11` | Playwright screenshot rendering (1440px / 375px) and Leica/scroll deontic quality gate checks. |

## Consequences
- No circular dependencies between synthesis, rendering, and auditing.
- Guarantees termination in at most 3 audit refinement rounds.
- Clean separation of concerns allowing parallel development by implementation roles.
