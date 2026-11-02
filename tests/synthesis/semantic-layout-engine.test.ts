import { describe, it, expect } from "vitest";
import { SemanticLayoutSynthesizer } from "../../src/synthesis/semantic-layout-engine";
import { OBSIDIAN_TOKENS } from "../../src/synthesis/tokens";

describe("SemanticLayoutSynthesizer", () => {
  const engine = new SemanticLayoutSynthesizer();

  it("produces concrete DOM tree structures without stub or mock components for product view", () => {
    const result = engine.synthesize({
      viewType: "product",
      components: ["header", "product-details", "action-bar"]
    });

    expect(result).toBeDefined();
    expect(result.root.tag).toBe("main");
    expect(result.root.children.length).toBe(3);

    const tags = result.root.children.map((c) => c.tag);
    expect(tags).toEqual(["header", "section", "div"]);

    // Verify absence of mock stubs
    const serialized = JSON.stringify(result.root);
    expect(serialized).not.toContain("mock");
    expect(serialized).not.toContain("stub");
  });

  it("binds #08090C to root background, #11141C to card/surface components, and emerald-500 (#10B981) to primary accents", () => {
    const result = engine.synthesize({
      viewType: "product",
      components: ["product-details", "action-bar"]
    });

    // Root background
    expect(result.root.style["background-color"]).toBe("#08090C");

    // Card / surface component
    const cardNode = result.root.children.find((c) => c.id.startsWith("product-card"));
    expect(cardNode).toBeDefined();
    expect(cardNode?.style["background-color"]).toBe("#11141C");

    // Action bar primary accent button
    const actionBar = result.root.children.find((c) => c.id.startsWith("action-bar"));
    expect(actionBar).toBeDefined();
    const primaryBtn = actionBar?.children.find((c) => c.tag === "button");
    expect(primaryBtn?.style["background-color"]).toBe("#10B981");
  });

  it("recovers gracefully with fallback structural rules for malformed component requests without crashing", () => {
    const result = engine.synthesize({
      viewType: "product",
      components: ["unsupported-widget", "", null as unknown as string]
    });

    expect(result).toBeDefined();
    expect(result.root.children.length).toBe(3);

    for (const child of result.root.children) {
      expect(child.attributes["data-fallback"]).toBe("true");
      expect(child.style["background-color"]).toBe(OBSIDIAN_TOKENS.background.surface);
    }
  });

  it("handles null or missing synthesis request payloads smoothly", () => {
    const result = engine.synthesize(null);
    expect(result).toBeDefined();
    expect(result.root.tag).toBe("main");
    expect(result.root.children.length).toBeGreaterThan(0);
  });

  it("produces deterministic DOM node IDs and structures when seed is provided", () => {
    const req = { viewType: "product", seed: 12345 };
    const res1 = engine.synthesize(req);
    const res2 = engine.synthesize(req);

    expect(res1.root.id).toBe(res2.root.id);
    expect(JSON.stringify(res1.root)).toBe(JSON.stringify(res2.root));
  });

  it("provides correct metadata with token bindings", () => {
    const result = engine.synthesize({ viewType: "product", seed: 999 });
    expect(result.metadata.tokenBindings.rootBackground).toBe("#08090C");
    expect(result.metadata.tokenBindings.cardSurface).toBe("#11141C");
    expect(result.metadata.tokenBindings.primaryAccent).toBe("#10B981");
  });
});
