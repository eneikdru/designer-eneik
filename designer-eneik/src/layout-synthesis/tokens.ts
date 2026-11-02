/**
 * Obsidian Design Tokens
 * Anti-slop, zero optical noise design tokens enforcing WCAG AAA contrast.
 */
export const OBSIDIAN_TOKENS = {
  colors: {
    rootBackground: '#08090C',
    cardSurface: '#11141C',
    surfaceElevated: '#171B26',
    surfaceBorder: '#232938',
    primaryAccent: '#10B981', // emerald-500
    primaryAccentHover: '#059669', // emerald-600
    primaryAccentMuted: 'rgba(16, 185, 129, 0.15)',
    textPrimary: '#F9FAFB',
    textSecondary: '#9CA3AF',
    textMuted: '#6B7280',
    danger: '#EF4444',
  },
  typography: {
    fontFamilySans: 'Inter, system-ui, -apple-system, sans-serif',
    fontFamilyMono: 'JetBrains Mono, Menlo, monospace',
    fontSizeXs: '12px',
    fontSizeSm: '14px',
    fontSizeMd: '16px',
    fontSizeLg: '18px',
    fontSizeXl: '24px',
    fontSize2xl: '32px',
    fontWeightNormal: '400',
    fontWeightMedium: '500',
    fontWeightSemibold: '600',
    fontWeightBold: '700',
  },
  spacing: {
    xs: '4px',
    sm: '8px',
    md: '16px',
    lg: '24px',
    xl: '32px',
    xxl: '48px',
  },
  radii: {
    sm: '4px',
    md: '8px',
    lg: '12px',
    full: '9999px',
  },
} as const;

export type ObsidianTokens = typeof OBSIDIAN_TOKENS;
