import { describe, expect, it } from 'vitest';
import { OBSIDIAN_TOKENS, SemanticLayoutSynthesizer } from '../src/layout-synthesis';

describe('BARCAN-TAG-02 Semantic Layout Synthesis Engine', () => {
  it('synthesizes concrete DOM tree structures without stub or mock components', () => {
    const synthesizer = new SemanticLayoutSynthesizer(100);
    const result = synthesizer.synthesize({
      viewId: 'product-overview',
      title: 'Product Catalog',
      seed: 100,
      components: [
        {
          type: 'header',
          title: 'Catalog Header',
          children: [
            {
              type: 'navigation',
              children: [
                { type: 'button', content: 'Explore', variant: 'primary' },
                { type: 'button', content: 'Filter', variant: 'secondary' },
              ],
            },
          ],
        },
        {
          type: 'hero',
          title: 'Autonomous System Architecture',
          content: 'Engineered layout generation without template mock stubs.',
        },
        {
          type: 'grid',
          children: [
            { type: 'card', title: 'Service Alpha', content: 'Telemetry pipeline' },
            { type: 'card', title: 'Service Beta', content: 'Decoupled agent bus' },
          ],
        },
      ],
    });

    expect(result.viewId).toBe('product-overview');
    expect(result.rootNode.tag).toBe('main');
    expect(result.rootNode.className).toBe('eneik-layout-root');
    expect(result.nodeCount).toBeGreaterThan(5);

    // Verify all child elements are real semantic tags, no mocks
    const serialized = JSON.stringify(result.rootNode);
    expect(serialized).not.toContain('mock');
    expect(serialized).not.toContain('stub');
    expect(serialized).not.toContain('placeholder');
  });

  it('strictly binds Obsidian tokens: #08090C to root, #11141C to card/surface, and emerald-500 to accents', () => {
    const synthesizer = new SemanticLayoutSynthesizer(200);
    const result = synthesizer.synthesize({
      viewId: 'theme-binding-view',
      title: 'Token Binding View',
      seed: 200,
      components: [
        {
          type: 'card',
          title: 'Surface Card',
          content: 'Ensuring obsidian surface colors',
          children: [{ type: 'button', content: 'Primary Action', variant: 'primary' }],
        },
      ],
    });

    // Root background
    expect(result.rootNode.styles.backgroundColor).toBe('#08090C');
    expect(result.rootNode.styles.backgroundColor).toBe(OBSIDIAN_TOKENS.colors.rootBackground);

    // Card / Surface component
    const cardNode = result.rootNode.children[0];
    expect(cardNode.tag).toBe('article');
    expect(cardNode.styles.backgroundColor).toBe('#11141C');
    expect(cardNode.styles.backgroundColor).toBe(OBSIDIAN_TOKENS.colors.cardSurface);

    // Primary accent button (emerald-500 = #10B981)
    const buttonNode = cardNode.children.find((c) => c.tag === 'button');
    expect(buttonNode).toBeDefined();
    expect(buttonNode?.styles.backgroundColor).toBe('#10B981');
    expect(buttonNode?.styles.backgroundColor).toBe(OBSIDIAN_TOKENS.colors.primaryAccent);
  });

  it('recovers with fallback structural rules upon malformed or unsupported component requests', () => {
    const synthesizer = new SemanticLayoutSynthesizer(300);
    const result = synthesizer.synthesize({
      viewId: 'recovery-view',
      title: 'Resilience Test View',
      seed: 300,
      components: [
        // @ts-expect-error intentional malformed input for resilience verification
        null,
        // Unsupported custom/legacy component type
        {
          type: 'experimental-unsupported-widget-x',
          title: 'Legacy Widget',
          content: 'Payload preserved',
        },
        { type: 'card', title: 'Standard Node' },
      ],
    });

    expect(result.unsupportedRecoveries).toBe(2);
    expect(result.rootNode.children).toHaveLength(3);

    const recoveredMalformed = result.rootNode.children[0];
    expect(recoveredMalformed.isFallback).toBe(true);
    expect(recoveredMalformed.attributes['data-fallback']).toBe('true');

    const recoveredUnsupported = result.rootNode.children[1];
    expect(recoveredUnsupported.isFallback).toBe(true);
    expect(recoveredUnsupported.styles.backgroundColor).toBe('#11141C');
    expect(recoveredUnsupported.textContent).toContain('Payload preserved');

    const standardNode = result.rootNode.children[2];
    expect(standardNode.isFallback).toBeUndefined();
    expect(standardNode.tag).toBe('article');
  });

  it('produces deterministic, seed-stable output without random divergence', () => {
    const req = {
      viewId: 'deterministic-view',
      title: 'Determinism Test',
      seed: 555,
      components: [
        { type: 'header', title: 'Deterministic Header' },
        { type: 'card', title: 'Deterministic Card' },
      ],
    };

    const runA = new SemanticLayoutSynthesizer().synthesize(req);
    const runB = new SemanticLayoutSynthesizer().synthesize(req);

    expect(runA).toEqual(runB);
  });
});
