import { describe, it, expect } from 'vitest';

interface ColorRGB {
  r: number;
  g: number;
  b: number;
  a?: number;
}

interface ThemeTokens {
  name: string;
  background: string;
  surface: string;
  metadata: {
    text: string;
    background: string;
  };
  mutedBadge: {
    text: string;
    background: string;
  };
}

/**
 * Parses 3-digit or 6-digit hex color strings into RGB components.
 */
function parseHexColor(hex: string): ColorRGB {
  const cleanHex = hex.replace('#', '').trim();
  if (cleanHex.length === 3) {
    const r = parseInt(cleanHex[0] + cleanHex[0], 16);
    const g = parseInt(cleanHex[1] + cleanHex[1], 16);
    const b = parseInt(cleanHex[2] + cleanHex[2], 16);
    return { r, g, b, a: 1 };
  }
  if (cleanHex.length === 6) {
    const r = parseInt(cleanHex.substring(0, 2), 16);
    const g = parseInt(cleanHex.substring(2, 4), 16);
    const b = parseInt(cleanHex.substring(4, 6), 16);
    return { r, g, b, a: 1 };
  }
  throw new Error(`Unsupported hex color format: ${hex}`);
}

/**
 * Computes relative luminance per WCAG 2.1 specifications.
 * Formula: L = 0.2126 * R + 0.7152 * G + 0.0722 * B
 */
function getRelativeLuminance(color: ColorRGB): number {
  const normalize = (channel: number): number => {
    const s = channel / 255;
    return s <= 0.04045 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
  };

  const rLinear = normalize(color.r);
  const gLinear = normalize(color.g);
  const bLinear = normalize(color.b);

  return 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear;
}

/**
 * Calculates the contrast ratio between foreground and background colors.
 * Formula: (L1 + 0.05) / (L2 + 0.05), where L1 is the lighter luminance.
 */
function calculateContrastRatio(fgHex: string, bgHex: string): number {
  const fg = parseHexColor(fgHex);
  const bg = parseHexColor(bgHex);

  const lum1 = getRelativeLuminance(fg);
  const lum2 = getRelativeLuminance(bg);

  const lighter = Math.max(lum1, lum2);
  const darker = Math.min(lum1, lum2);

  const ratio = (lighter + 0.05) / (darker + 0.05);
  return Math.round(ratio * 100) / 100;
}

// Verified theme tokens designed for strict WCAG AA (>= 4.5:1) compliance
const themeVariants: Record<string, ThemeTokens> = {
  light: {
    name: 'light',
    background: '#FFFFFF',
    surface: '#F8FAFC',
    metadata: {
      text: '#475569', // slate-600 (~5.89:1 vs #FFFFFF)
      background: '#FFFFFF',
    },
    mutedBadge: {
      text: '#334155', // slate-700 (~7.46:1 vs #F1F5F9)
      background: '#F1F5F9', // slate-100
    },
  },
  dark: {
    name: 'dark',
    background: '#0F172A', // slate-900
    surface: '#1E293B', // slate-800
    metadata: {
      text: '#94A3B8', // slate-400 (~5.78:1 vs #0F172A)
      background: '#0F172A',
    },
    mutedBadge: {
      text: '#E2E8F0', // slate-200 (~8.41:1 vs #1E293B)
      background: '#1E293B',
    },
  },
  highContrast: {
    name: 'high-contrast',
    background: '#000000',
    surface: '#000000',
    metadata: {
      text: '#FFFFFF', // 21:1 vs #000000
      background: '#000000',
    },
    mutedBadge: {
      text: '#FFFF00', // yellow 19.56:1 vs #000000
      background: '#000000',
    },
  },
};

class RuntimeThemeManager {
  private currentTheme: ThemeTokens;

  constructor(initialTheme: ThemeTokens) {
    this.currentTheme = initialTheme;
  }

  public switchTheme(theme: ThemeTokens): void {
    this.currentTheme = theme;
  }

  public getTokens(): ThemeTokens {
    return this.currentTheme;
  }
}

describe('WCAG AA 4.5:1 Contrast Ratio Automated Verification', () => {
  const WCAG_AA_MIN_RATIO = 4.5;

  describe('Static Theme Variants Evaluation', () => {
    Object.values(themeVariants).forEach((theme) => {
      it(`should satisfy WCAG AA >= 4.5:1 for subtle metadata text in '${theme.name}' theme`, () => {
        const ratio = calculateContrastRatio(theme.metadata.text, theme.metadata.background);
        expect(ratio).toBeGreaterThanOrEqual(WCAG_AA_MIN_RATIO);
      });

      it(`should satisfy WCAG AA >= 4.5:1 for muted badge elements in '${theme.name}' theme`, () => {
        const ratio = calculateContrastRatio(theme.mutedBadge.text, theme.mutedBadge.background);
        expect(ratio).toBeGreaterThanOrEqual(WCAG_AA_MIN_RATIO);
      });
    });
  });

  describe('Dynamic Runtime Theme Switching Evaluation', () => {
    it('should maintain zero WCAG AA contrast violations during dynamic theme transitions', () => {
      const manager = new RuntimeThemeManager(themeVariants.light);
      const themesToTest = [themeVariants.dark, themeVariants.highContrast, themeVariants.light];

      for (const nextTheme of themesToTest) {
        manager.switchTheme(nextTheme);
        const tokens = manager.getTokens();

        const metadataRatio = calculateContrastRatio(tokens.metadata.text, tokens.metadata.background);
        const badgeRatio = calculateContrastRatio(tokens.mutedBadge.text, tokens.mutedBadge.background);

        expect(metadataRatio).toBeGreaterThanOrEqual(WCAG_AA_MIN_RATIO);
        expect(badgeRatio).toBeGreaterThanOrEqual(WCAG_AA_MIN_RATIO);
      }
    });
  });
});
