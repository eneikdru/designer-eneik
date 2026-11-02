import { OBSIDIAN_TOKENS, DesignTokens } from "./tokens";

export interface DOMNode {
  tag: string;
  id: string;
  classes: string[];
  attributes: Record<string, string>;
  style: Record<string, string>;
  children: DOMNode[];
  text?: string;
}

export interface SynthesisRequest {
  viewType: "product" | "dashboard" | "general" | string;
  components?: string[];
  seed?: number;
  customTokens?: Partial<DesignTokens>;
}

export interface SynthesisResult {
  root: DOMNode;
  metadata: {
    viewType: string;
    seed: number;
    componentCount: number;
    tokenBindings: {
      rootBackground: string;
      cardSurface: string;
      primaryAccent: string;
    };
  };
}

class SeededRandom {
  private state: number;

  constructor(seed: number = 42) {
    this.state = seed % 2147483647;
    if (this.state <= 0) {
      this.state += 2147483646;
    }
  }

  public next(): number {
    this.state = (this.state * 16807) % 2147483647;
    return (this.state - 1) / 2147483646;
  }

  public nextId(prefix: string): string {
    const val = Math.floor(this.next() * 100000);
    return `${prefix}-${val}`;
  }
}

export class SemanticLayoutSynthesizer {
  private tokens: DesignTokens;

  constructor(tokens: DesignTokens = OBSIDIAN_TOKENS) {
    this.tokens = tokens;
  }

  public synthesize(request?: SynthesisRequest | null): SynthesisResult {
    const safeRequest: SynthesisRequest = request && typeof request === "object" ? request : { viewType: "product" };
    const seed = typeof safeRequest.seed === "number" ? safeRequest.seed : 42;
    const rng = new SeededRandom(seed);

    const rootId = rng.nextId("view-root");
    const rootNode: DOMNode = {
      tag: "main",
      id: rootId,
      classes: ["obsidian-root-container", "min-h-screen", "w-full"],
      attributes: {
        "role": "main",
        "data-theme": "obsidian"
      },
      style: {
        "background-color": this.tokens.background.root,
        "color": this.tokens.text.primary,
        "display": "flex",
        "flex-direction": "column",
        "min-height": "100vh"
      },
      children: []
    };

    const rawComponents = Array.isArray(safeRequest.components) && safeRequest.components.length > 0
      ? safeRequest.components
      : ["header", "product-details", "action-bar"];

    let componentCount = 0;

    for (const compName of rawComponents) {
      const synthesized = this.synthesizeComponent(compName, rng);
      rootNode.children.push(synthesized);
      componentCount++;
    }

    return {
      root: rootNode,
      metadata: {
        viewType: safeRequest.viewType || "product",
        seed,
        componentCount,
        tokenBindings: {
          rootBackground: this.tokens.background.root,
          cardSurface: this.tokens.background.surface,
          primaryAccent: this.tokens.accent.primary
        }
      }
    };
  }

  private synthesizeComponent(name: unknown, rng: SeededRandom): DOMNode {
    if (typeof name !== "string" || !name.trim()) {
      return this.synthesizeFallback(rng, "Empty or non-string component identifier");
    }

    const normalized = name.trim().toLowerCase();

    switch (normalized) {
      case "header":
        return {
          tag: "header",
          id: rng.nextId("header"),
          classes: ["obsidian-header", "w-full", "px-6", "py-4"],
          attributes: { "role": "banner" },
          style: {
            "background-color": this.tokens.background.surface,
            "border-bottom": `1px solid ${this.tokens.border.subtle}`,
            "color": this.tokens.text.primary
          },
          children: [
            {
              tag: "nav",
              id: rng.nextId("nav"),
              classes: ["flex", "items-center", "justify-between"],
              attributes: { "aria-label": "Main Navigation" },
              style: {
                "display": "flex",
                "justify-content": "space-between",
                "align-items": "center"
              },
              children: [
                {
                  tag: "span",
                  id: rng.nextId("brand-title"),
                  classes: ["font-bold", "text-lg"],
                  attributes: {},
                  style: { "color": this.tokens.accent.primary },
                  children: [],
                  text: "Obsidian Studio"
                }
              ]
            }
          ]
        };

      case "product-details":
      case "product-card":
        return {
          tag: "section",
          id: rng.nextId("product-card"),
          classes: ["obsidian-card", "rounded-lg", "p-6", "m-4"],
          attributes: {
            "aria-labelledby": "product-title",
            "data-component": "product-details"
          },
          style: {
            "background-color": this.tokens.background.surface,
            "border": `1px solid ${this.tokens.border.subtle}`,
            "border-radius": "8px",
            "padding": "24px"
          },
          children: [
            {
              tag: "h1",
              id: rng.nextId("product-title"),
              classes: ["text-2xl", "font-semibold"],
              attributes: {},
              style: { "color": this.tokens.text.primary },
              children: [],
              text: "Semantic Layout Specification"
            },
            {
              tag: "p",
              id: rng.nextId("product-desc"),
              classes: ["text-sm", "mt-2"],
              attributes: {},
              style: { "color": this.tokens.text.muted },
              children: [],
              text: "Autonomously generated DOM layout tree bound to Obsidian system design tokens."
            }
          ]
        };

      case "action-bar":
        return {
          tag: "div",
          id: rng.nextId("action-bar"),
          classes: ["obsidian-actions", "flex", "gap-4", "p-4"],
          attributes: { "role": "toolbar" },
          style: {
            "background-color": this.tokens.background.surface,
            "display": "flex",
            "gap": "12px",
            "padding": "16px"
          },
          children: [
            {
              tag: "button",
              id: rng.nextId("btn-primary"),
              classes: ["obsidian-btn-primary", "font-medium", "px-4", "py-2", "rounded"],
              attributes: {
                "type": "button",
                "data-variant": "primary"
              },
              style: {
                "background-color": this.tokens.accent.primary,
                "color": "#08090C",
                "border": "none",
                "border-radius": "4px",
                "cursor": "pointer",
                "font-weight": "600"
              },
              children: [],
              text: "Deploy Layout"
            }
          ]
        };

      default:
        return this.synthesizeFallback(rng, `Unsupported component: ${String(name)}`);
    }
  }

  private synthesizeFallback(rng: SeededRandom, reason: string): DOMNode {
    return {
      tag: "div",
      id: rng.nextId("fallback-container"),
      classes: ["obsidian-fallback-card", "p-4", "rounded"],
      attributes: {
        "data-fallback": "true",
        "data-reason": reason
      },
      style: {
        "background-color": this.tokens.background.surface,
        "border": `1px dashed ${this.tokens.border.default}`,
        "color": this.tokens.text.muted,
        "padding": "16px",
        "margin": "8px"
      },
      children: [
        {
          tag: "span",
          id: rng.nextId("fallback-message"),
          classes: ["text-xs"],
          attributes: {},
          style: { "color": this.tokens.text.muted },
          children: [],
          text: `Structural Recovery: ${reason}`
        }
      ]
    };
  }
}
