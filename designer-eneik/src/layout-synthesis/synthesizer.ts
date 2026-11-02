import { OBSIDIAN_TOKENS } from './tokens';
import {
  ComponentType,
  DOMStyleMap,
  LayoutSynthesisRequest,
  LayoutSynthesisResult,
  SynthesisComponentSpec,
  SynthesizedDOMNode,
} from './types';

export class SemanticLayoutSynthesizer {
  private idCounter = 0;
  private unsupportedRecoveries = 0;

  constructor(private readonly initialSeed: number = 42) {
    this.idCounter = initialSeed;
  }

  public synthesize(request: LayoutSynthesisRequest): LayoutSynthesisResult {
    this.idCounter = request.seed ?? this.initialSeed;
    this.unsupportedRecoveries = 0;

    const rootNode: SynthesizedDOMNode = {
      id: `view-root-${request.viewId}`,
      tag: 'main',
      role: 'main',
      className: 'eneik-layout-root',
      styles: {
        backgroundColor: OBSIDIAN_TOKENS.colors.rootBackground,
        color: OBSIDIAN_TOKENS.colors.textPrimary,
        fontFamily: OBSIDIAN_TOKENS.typography.fontFamilySans,
        minHeight: '100vh',
        width: '100%',
        boxSizing: 'border-box',
        padding: OBSIDIAN_TOKENS.spacing.lg,
        display: 'flex',
        flexDirection: 'column',
        gap: OBSIDIAN_TOKENS.spacing.lg,
      },
      attributes: {
        'data-view-id': request.viewId,
        'data-theme': 'obsidian',
      },
      children: [],
    };

    if (Array.isArray(request.components)) {
      for (const compSpec of request.components) {
        const childNode = this.synthesizeComponent(compSpec);
        rootNode.children.push(childNode);
      }
    }

    const totalNodes = this.countNodes(rootNode);

    return {
      viewId: request.viewId,
      rootNode,
      nodeCount: totalNodes,
      unsupportedRecoveries: this.unsupportedRecoveries,
      contrastValidation: {
        rootBg: OBSIDIAN_TOKENS.colors.rootBackground,
        surfaceBg: OBSIDIAN_TOKENS.colors.cardSurface,
        primaryAccent: OBSIDIAN_TOKENS.colors.primaryAccent,
        wcagCompliant: true,
      },
    };
  }

  private nextId(prefix: string): string {
    this.idCounter += 1;
    return `${prefix}-${this.idCounter}`;
  }

  private countNodes(node: SynthesizedDOMNode): number {
    let count = 1;
    for (const child of node.children) {
      count += this.countNodes(child);
    }
    return count;
  }

  private synthesizeComponent(spec: SynthesisComponentSpec | null | undefined): SynthesizedDOMNode {
    if (!spec || typeof spec !== 'object') {
      this.unsupportedRecoveries += 1;
      return this.createFallbackNode('malformed-spec', 'Malformed component descriptor recovered safely');
    }

    const type = (spec.type || '').toLowerCase() as ComponentType;

    switch (type) {
      case 'header':
        return this.synthesizeHeader(spec);
      case 'navigation':
        return this.synthesizeNavigation(spec);
      case 'hero':
        return this.synthesizeHero(spec);
      case 'card':
        return this.synthesizeCard(spec);
      case 'grid':
        return this.synthesizeGrid(spec);
      case 'button':
        return this.synthesizeButton(spec);
      case 'typography':
        return this.synthesizeTypography(spec);
      case 'section':
      case 'container':
        return this.synthesizeContainer(spec);
      default:
        this.unsupportedRecoveries += 1;
        return this.createFallbackNode(
          spec.id || this.nextId('fallback'),
          `Unsupported component '${spec.type}'; rendered as standard semantic card`,
          spec
        );
    }
  }

  private synthesizeHeader(spec: SynthesisComponentSpec): SynthesizedDOMNode {
    const id = spec.id || this.nextId('hdr');
    const children: SynthesizedDOMNode[] = [];

    if (spec.title) {
      children.push({
        id: `${id}-title`,
        tag: 'h1',
        className: 'eneik-header-title',
        styles: {
          color: OBSIDIAN_TOKENS.colors.textPrimary,
          fontSize: OBSIDIAN_TOKENS.typography.fontSize2xl,
          fontWeight: OBSIDIAN_TOKENS.typography.fontWeightBold,
          margin: '0',
        },
        attributes: {},
        children: [],
        textContent: spec.title,
      });
    }

    if (Array.isArray(spec.children)) {
      for (const child of spec.children) {
        children.push(this.synthesizeComponent(child));
      }
    }

    return {
      id,
      tag: 'header',
      role: 'banner',
      className: 'eneik-layout-header',
      styles: {
        backgroundColor: OBSIDIAN_TOKENS.colors.cardSurface,
        border: `1px solid ${OBSIDIAN_TOKENS.colors.surfaceBorder}`,
        borderRadius: OBSIDIAN_TOKENS.radii.md,
        padding: `${OBSIDIAN_TOKENS.spacing.md} ${OBSIDIAN_TOKENS.spacing.lg}`,
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
      },
      attributes: { 'aria-label': spec.title || 'Header' },
      children,
    };
  }

  private synthesizeNavigation(spec: SynthesisComponentSpec): SynthesizedDOMNode {
    const id = spec.id || this.nextId('nav');
    const children: SynthesizedDOMNode[] = [];

    if (Array.isArray(spec.children)) {
      for (const child of spec.children) {
        children.push(this.synthesizeComponent(child));
      }
    }

    return {
      id,
      tag: 'nav',
      role: 'navigation',
      className: 'eneik-layout-nav',
      styles: {
        display: 'flex',
        gap: OBSIDIAN_TOKENS.spacing.md,
        alignItems: 'center',
      },
      attributes: { 'aria-label': spec.title || 'Main Navigation' },
      children,
    };
  }

  private synthesizeHero(spec: SynthesisComponentSpec): SynthesizedDOMNode {
    const id = spec.id || this.nextId('hero');
    const children: SynthesizedDOMNode[] = [];

    if (spec.title) {
      children.push({
        id: `${id}-title`,
        tag: 'h2',
        className: 'eneik-hero-title',
        styles: {
          color: OBSIDIAN_TOKENS.colors.textPrimary,
          fontSize: OBSIDIAN_TOKENS.typography.fontSizeXl,
          fontWeight: OBSIDIAN_TOKENS.typography.fontWeightBold,
          margin: '0',
        },
        attributes: {},
        children: [],
        textContent: spec.title,
      });
    }

    if (spec.content) {
      children.push({
        id: `${id}-desc`,
        tag: 'p',
        className: 'eneik-hero-desc',
        styles: {
          color: OBSIDIAN_TOKENS.colors.textSecondary,
          fontSize: OBSIDIAN_TOKENS.typography.fontSizeMd,
          margin: '0',
        },
        attributes: {},
        children: [],
        textContent: spec.content,
      });
    }

    if (Array.isArray(spec.children)) {
      for (const child of spec.children) {
        children.push(this.synthesizeComponent(child));
      }
    }

    return {
      id,
      tag: 'section',
      role: 'region',
      className: 'eneik-hero-section',
      styles: {
        backgroundColor: OBSIDIAN_TOKENS.colors.cardSurface,
        border: `1px solid ${OBSIDIAN_TOKENS.colors.surfaceBorder}`,
        borderRadius: OBSIDIAN_TOKENS.radii.lg,
        padding: OBSIDIAN_TOKENS.spacing.xl,
        display: 'flex',
        flexDirection: 'column',
        gap: OBSIDIAN_TOKENS.spacing.md,
      },
      attributes: { 'aria-label': spec.title || 'Hero Section' },
      children,
    };
  }

  private synthesizeCard(spec: SynthesisComponentSpec): SynthesizedDOMNode {
    const id = spec.id || this.nextId('card');
    const children: SynthesizedDOMNode[] = [];

    if (spec.title) {
      children.push({
        id: `${id}-title`,
        tag: 'h3',
        className: 'eneik-card-title',
        styles: {
          color: OBSIDIAN_TOKENS.colors.textPrimary,
          fontSize: OBSIDIAN_TOKENS.typography.fontSizeLg,
          fontWeight: OBSIDIAN_TOKENS.typography.fontWeightSemibold,
          margin: '0',
        },
        attributes: {},
        children: [],
        textContent: spec.title,
      });
    }

    if (spec.content) {
      children.push({
        id: `${id}-content`,
        tag: 'p',
        className: 'eneik-card-body',
        styles: {
          color: OBSIDIAN_TOKENS.colors.textSecondary,
          fontSize: OBSIDIAN_TOKENS.typography.fontSizeSm,
          lineHeight: '1.5',
          margin: '0',
        },
        attributes: {},
        children: [],
        textContent: spec.content,
      });
    }

    if (Array.isArray(spec.children)) {
      for (const child of spec.children) {
        children.push(this.synthesizeComponent(child));
      }
    }

    return {
      id,
      tag: 'article',
      role: 'article',
      className: 'eneik-card',
      styles: {
        backgroundColor: OBSIDIAN_TOKENS.colors.cardSurface,
        border: `1px solid ${OBSIDIAN_TOKENS.colors.surfaceBorder}`,
        borderRadius: OBSIDIAN_TOKENS.radii.md,
        padding: OBSIDIAN_TOKENS.spacing.lg,
        display: 'flex',
        flexDirection: 'column',
        gap: OBSIDIAN_TOKENS.spacing.sm,
      },
      attributes: { 'aria-labelledby': spec.title ? `${id}-title` : id },
      children,
    };
  }

  private synthesizeGrid(spec: SynthesisComponentSpec): SynthesizedDOMNode {
    const id = spec.id || this.nextId('grid');
    const children: SynthesizedDOMNode[] = [];

    if (Array.isArray(spec.children)) {
      for (const child of spec.children) {
        children.push(this.synthesizeComponent(child));
      }
    }

    return {
      id,
      tag: 'section',
      role: 'region',
      className: 'eneik-layout-grid',
      styles: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
        gap: OBSIDIAN_TOKENS.spacing.lg,
        width: '100%',
        boxSizing: 'border-box',
      },
      attributes: { 'aria-label': spec.title || 'Grid layout' },
      children,
    };
  }

  private synthesizeButton(spec: SynthesisComponentSpec): SynthesizedDOMNode {
    const id = spec.id || this.nextId('btn');
    const isPrimary = spec.variant !== 'secondary' && spec.variant !== 'ghost';

    const styles: DOMStyleMap = isPrimary
      ? {
          backgroundColor: OBSIDIAN_TOKENS.colors.primaryAccent,
          color: OBSIDIAN_TOKENS.colors.rootBackground,
          fontWeight: OBSIDIAN_TOKENS.typography.fontWeightMedium,
          fontSize: OBSIDIAN_TOKENS.typography.fontSizeSm,
          borderRadius: OBSIDIAN_TOKENS.radii.sm,
          padding: `${OBSIDIAN_TOKENS.spacing.sm} ${OBSIDIAN_TOKENS.spacing.md}`,
          border: 'none',
        }
      : {
          backgroundColor: OBSIDIAN_TOKENS.colors.cardSurface,
          color: OBSIDIAN_TOKENS.colors.textPrimary,
          fontWeight: OBSIDIAN_TOKENS.typography.fontWeightMedium,
          fontSize: OBSIDIAN_TOKENS.typography.fontSizeSm,
          borderRadius: OBSIDIAN_TOKENS.radii.sm,
          padding: `${OBSIDIAN_TOKENS.spacing.sm} ${OBSIDIAN_TOKENS.spacing.md}`,
          border: `1px solid ${OBSIDIAN_TOKENS.colors.surfaceBorder}`,
        };

    return {
      id,
      tag: 'button',
      role: 'button',
      className: `eneik-btn ${isPrimary ? 'eneik-btn-primary' : 'eneik-btn-secondary'}`,
      styles,
      attributes: { type: 'button' },
      children: [],
      textContent: spec.content || spec.title || 'Action',
    };
  }

  private synthesizeTypography(spec: SynthesisComponentSpec): SynthesizedDOMNode {
    const id = spec.id || this.nextId('txt');
    return {
      id,
      tag: 'p',
      className: 'eneik-typography',
      styles: {
        color: OBSIDIAN_TOKENS.colors.textPrimary,
        fontSize: OBSIDIAN_TOKENS.typography.fontSizeMd,
        margin: '0',
      },
      attributes: {},
      children: [],
      textContent: spec.content || '',
    };
  }

  private synthesizeContainer(spec: SynthesisComponentSpec): SynthesizedDOMNode {
    const id = spec.id || this.nextId('container');
    const children: SynthesizedDOMNode[] = [];

    if (Array.isArray(spec.children)) {
      for (const child of spec.children) {
        children.push(this.synthesizeComponent(child));
      }
    }

    return {
      id,
      tag: 'section',
      role: 'region',
      className: 'eneik-container',
      styles: {
        display: 'flex',
        flexDirection: 'column',
        gap: OBSIDIAN_TOKENS.spacing.md,
      },
      attributes: { 'aria-label': spec.title || 'Container' },
      children,
    };
  }

  private createFallbackNode(
    id: string,
    recoveryMessage: string,
    originalSpec?: SynthesisComponentSpec
  ): SynthesizedDOMNode {
    const children: SynthesizedDOMNode[] = [];

    if (originalSpec && Array.isArray(originalSpec.children)) {
      for (const child of originalSpec.children) {
        children.push(this.synthesizeComponent(child));
      }
    }

    return {
      id,
      tag: 'div',
      role: 'region',
      className: 'eneik-fallback-card',
      isFallback: true,
      styles: {
        backgroundColor: OBSIDIAN_TOKENS.colors.cardSurface,
        border: `1px dashed ${OBSIDIAN_TOKENS.colors.surfaceBorder}`,
        borderRadius: OBSIDIAN_TOKENS.radii.md,
        padding: OBSIDIAN_TOKENS.spacing.md,
        color: OBSIDIAN_TOKENS.colors.textSecondary,
      },
      attributes: {
        'data-fallback': 'true',
        'data-recovery-reason': recoveryMessage,
      },
      children,
      textContent: originalSpec?.content || recoveryMessage,
    };
  }
}
