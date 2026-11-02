import { test, expect } from '@playwright/test';

export interface ObsidianTokens {
  background: string;
  surface: string;
  accent: string;
  textPrimary: string;
  textMuted: string;
  border: string;
}

export const OBSIDIAN_TOKENS: ObsidianTokens = {
  background: '#08090C',
  surface: '#11141C',
  accent: '#10B981',
  textPrimary: '#F9FAFB',
  textMuted: '#9CA3AF',
  border: '#1F2430',
};

export function hexToRgb(hex: string): { r: number; g: number; b: number } {
  const cleanHex = hex.replace('#', '').trim();
  const num = parseInt(cleanHex, 16);
  return {
    r: (num >> 16) & 255,
    g: (num >> 8) & 255,
    b: num & 255,
  };
}

export function calculateRelativeLuminance(rgb: { r: number; g: number; b: number }): number {
  const [rs, gs, bs] = [rgb.r / 255, rgb.g / 255, rgb.b / 255].map((val) => {
    return val <= 0.03928 ? val / 12.92 : Math.pow((val + 0.055) / 1.055, 2.4);
  });
  return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs;
}

export function calculateContrastRatio(hexForeground: string, hexBackground: string): number {
  const lumForeground = calculateRelativeLuminance(hexToRgb(hexForeground));
  const lumBackground = calculateRelativeLuminance(hexToRgb(hexBackground));
  const lighter = Math.max(lumForeground, lumBackground);
  const darker = Math.min(lumForeground, lumBackground);
  return (lighter + 0.05) / (darker + 0.05);
}

export function validateAntiSlopPalette(palette: Record<string, string>): { valid: boolean; violations: string[] } {
  const FORBIDDEN_SLOP_PREFIXES = ['#888888', '#737373', '#555555', '#404040', '#cccccc'];
  const violations: string[] = [];

  for (const [key, value] of Object.entries(palette)) {
    const normalized = value.toLowerCase();
    if (FORBIDDEN_SLOP_PREFIXES.includes(normalized)) {
      violations.push(`Token '${key}' uses forbidden slop color: ${value}`);
    }
    if (!/^#[0-9a-fA-F]{6}$/.test(normalized)) {
      violations.push(`Token '${key}' has invalid hex syntax: ${value}`);
    }
  }

  return {
    valid: violations.length === 0,
    violations,
  };
}

test.describe('Unit: Obsidian Token Binding and Contrast Verification', () => {
  test('Obsidian design tokens adhere strictly to canonical hex specs', () => {
    expect(OBSIDIAN_TOKENS.background.toUpperCase()).toBe('#08090C');
    expect(OBSIDIAN_TOKENS.surface.toUpperCase()).toBe('#11141C');
    expect(OBSIDIAN_TOKENS.accent.toUpperCase()).toBe('#10B981');
  });

  test('Anti-slop palette validator passes canonical Obsidian palette', () => {
    const audit = validateAntiSlopPalette(OBSIDIAN_TOKENS as unknown as Record<string, string>);
    expect(audit.valid).toBe(true);
    expect(audit.violations).toHaveLength(0);
  });

  test('Anti-slop palette validator flags forbidden slop colors', () => {
    const slopPalette = {
      ...OBSIDIAN_TOKENS,
      slopBorder: '#888888',
    };
    const audit = validateAntiSlopPalette(slopPalette as unknown as Record<string, string>);
    expect(audit.valid).toBe(false);
    expect(audit.violations.length).toBeGreaterThan(0);
  });

  test('Calculates relative luminance accurately for base colors', () => {
    const blackLum = calculateRelativeLuminance({ r: 0, g: 0, b: 0 });
    const whiteLum = calculateRelativeLuminance({ r: 255, g: 255, b: 255 });
    expect(blackLum).toBeCloseTo(0.0, 4);
    expect(whiteLum).toBeCloseTo(1.0, 4);
  });

  test('Mathematical contrast assertions satisfy WCAG AAA standards (>= 7.0:1)', () => {
    const textToBgContrast = calculateContrastRatio(OBSIDIAN_TOKENS.textPrimary, OBSIDIAN_TOKENS.background);
    expect(textToBgContrast).toBeGreaterThanOrEqual(7.0);
    expect(textToBgContrast).toBeCloseTo(18.5, 0);

    const textToSurfaceContrast = calculateContrastRatio(OBSIDIAN_TOKENS.textPrimary, OBSIDIAN_TOKENS.surface);
    expect(textToSurfaceContrast).toBeGreaterThanOrEqual(7.0);
    expect(textToSurfaceContrast).toBeCloseTo(17.4, 0);
  });

  test('Emerald-500 accent satisfies high-contrast graphics indicator requirement (>= 4.5:1)', () => {
    const accentToBgContrast = calculateContrastRatio(OBSIDIAN_TOKENS.accent, OBSIDIAN_TOKENS.background);
    expect(accentToBgContrast).toBeGreaterThanOrEqual(4.5);
    expect(accentToBgContrast).toBeGreaterThanOrEqual(7.0);
  });
});
