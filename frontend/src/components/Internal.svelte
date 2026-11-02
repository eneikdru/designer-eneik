<script lang="ts">
  import { onMount } from 'svelte';

  export let mockupRoot: HTMLElement | null = null;

  export interface GateViolation {
    type: 'PALETTE' | 'CONTRAST' | 'OPTICAL_NOISE';
    severity: 'BLOCKING' | 'WARNING';
    message: string;
    selector?: string;
    actual?: string | number;
    expected?: string | number;
  }

  export interface AuditResult {
    passed: boolean;
    opticalNoiseScore: number;
    violations: GateViolation[];
    evaluatedAt: string;
  }

  export let auditResult: AuditResult = {
    passed: true,
    opticalNoiseScore: 0,
    violations: [],
    evaluatedAt: ''
  };

  // Approved Obsidian Tokens and Slop Blacklist
  const APPROVED_PALETTE = new Set([
    '#08090c',
    '#11141c',
    '#10b981', // emerald-500
    '#059669', // emerald-600
    '#34d399', // emerald-400
    '#ffffff',
    '#000000',
    '#1f2937',
    '#111827',
    '#0f172a',
    '#e2e8f0',
    '#f8fafc'
  ]);

  const SLOP_PALETTE_PATTERNS = [
    /^#ff00/i, // crude saturated primaries
    /^#00ff/i,
    /^#ffff00/i,
    /^#808080/i // default unstyled gray
  ];

  function rgbToHex(rgbStr: string): string {
    const match = rgbStr.match(/^rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)$/);
    if (!match) return rgbStr.toLowerCase();
    const r = parseInt(match[1], 10).toString(16).padStart(2, '0');
    const g = parseInt(match[2], 10).toString(16).padStart(2, '0');
    const b = parseInt(match[3], 10).toString(16).padStart(2, '0');
    return `#${r}${g}${b}`.toLowerCase();
  }

  function parseRgb(color: string): [number, number, number] | null {
    if (color.startsWith('#')) {
      let hex = color.slice(1);
      if (hex.length === 3) hex = hex.split('').map(c => c + c).join('');
      if (hex.length === 6) {
        return [
          parseInt(hex.substring(0, 2), 16),
          parseInt(hex.substring(2, 4), 16),
          parseInt(hex.substring(4, 6), 16)
        ];
      }
    }
    const match = color.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/);
    if (match) {
      return [parseInt(match[1], 10), parseInt(match[2], 10), parseInt(match[3], 10)];
    }
    return null;
  }

  function getLuminance(r: number, g: number, b: number): number {
    const a = [r, g, b].map(v => {
      const s = v / 255;
      return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    });
    return a[0] * 0.2126 + a[1] * 0.7152 + a[2] * 0.0722;
  }

  function calculateContrastRatio(fgRgb: [number, number, number], bgRgb: [number, number, number]): number {
    const l1 = getLuminance(fgRgb[0], fgRgb[1], fgRgb[2]);
    const l2 = getLuminance(bgRgb[0], bgRgb[1], bgRgb[2]);
    const lighter = Math.max(l1, l2);
    const darker = Math.min(l1, l2);
    return (lighter + 0.05) / (darker + 0.05);
  }

  export function runDeonticGate(root: HTMLElement | null = mockupRoot): AuditResult {
    const violations: GateViolation[] = [];
    let opticalNoiseScore = 0;

    if (!root) {
      const result: AuditResult = {
        passed: false,
        opticalNoiseScore: 1.0,
        violations: [{
          type: 'OPTICAL_NOISE',
          severity: 'BLOCKING',
          message: 'No DOM geometry or mockup container available for deontic gate evaluation.'
        }],
        evaluatedAt: new Date().toISOString()
      };
      auditResult = result;
      return result;
    }

    const elements = Array.from(root.querySelectorAll<HTMLElement>('*'));
    elements.push(root);

    const renderedBoxes: DOMRect[] = [];

    for (let i = 0; i < elements.length; i++) {
      const el = elements[i];
      const style = window.getComputedStyle(el);
      const rect = el.getBoundingClientRect();

      if (rect.width > 0 && rect.height > 0) {
        renderedBoxes.push(rect);
      }

      // 1. Color Palette & Slop Check
      const bgHex = rgbToHex(style.backgroundColor);
      const colorHex = rgbToHex(style.color);

      if (style.backgroundColor && style.backgroundColor !== 'transparent' && style.backgroundColor !== 'rgba(0, 0, 0, 0)') {
        const isSlop = SLOP_PALETTE_PATTERNS.some(pat => pat.test(bgHex));
        if (isSlop) {
          violations.push({
            type: 'PALETTE',
            severity: 'BLOCKING',
            selector: el.tagName.toLowerCase() + (el.id ? '#' + el.id : ''),
            message: `Slop color pattern detected in background: ${bgHex}`,
            actual: bgHex,
            expected: 'Obsidian token (#08090C, #11141C, emerald-500)'
          });
        } else if (!APPROVED_PALETTE.has(bgHex)) {
          violations.push({
            type: 'PALETTE',
            severity: 'BLOCKING',
            selector: el.tagName.toLowerCase() + (el.id ? '#' + el.id : ''),
            message: `Non-approved palette color in background: ${bgHex}`,
            actual: bgHex,
            expected: 'Obsidian token (#08090C, #11141C, emerald-500)'
          });
        }
      }

      // 2. Contrast Auditor (WCAG AAA: 7:1 for normal, 4.5:1 for large text >= 18pt or bold >= 14pt)
      const hasDirectText = Array.from(el.childNodes).some(node => node.nodeType === Node.TEXT_NODE && (node.textContent?.trim()?.length ?? 0) > 0);
      if (hasDirectText) {
        const fgRgb = parseRgb(colorHex);
        let effectiveBgHex = bgHex;
        let parent = el.parentElement;
        while ((effectiveBgHex === 'transparent' || effectiveBgHex === 'rgba(0, 0, 0, 0)' || effectiveBgHex === '#00000000') && parent) {
          const pStyle = window.getComputedStyle(parent);
          if (pStyle.backgroundColor && pStyle.backgroundColor !== 'transparent' && pStyle.backgroundColor !== 'rgba(0, 0, 0, 0)') {
            effectiveBgHex = rgbToHex(pStyle.backgroundColor);
            break;
          }
          parent = parent.parentElement;
        }
        if (effectiveBgHex === 'transparent' || effectiveBgHex === 'rgba(0, 0, 0, 0)') {
          effectiveBgHex = '#08090c'; // default obsidian canvas
        }
        const bgRgb = parseRgb(effectiveBgHex);

        if (fgRgb && bgRgb) {
          const ratio = calculateContrastRatio(fgRgb, bgRgb);
          const fontSize = parseFloat(style.fontSize) || 16;
          const fontWeight = parseInt(style.fontWeight, 10) || 400;
          const isLargeText = fontSize >= 24 || (fontSize >= 18.66 && fontWeight >= 700);
          const minRatio = isLargeText ? 4.5 : 7.0;

          if (ratio < minRatio) {
            violations.push({
              type: 'CONTRAST',
              severity: 'BLOCKING',
              selector: el.tagName.toLowerCase() + (el.id ? '#' + el.id : ''),
              message: `WCAG AAA contrast failure: ratio is ${ratio.toFixed(2)}:1 (required ${minRatio}:1)`,
              actual: `${ratio.toFixed(2)}:1`,
              expected: `>= ${minRatio}:1`
            });
          }
        }
      }
    }

    // 3. Optical Noise & Sub-pixel Misalignment Analysis
    // Measures fraction of adjacent geometries with off-grid floating misalignment (fractional offsets < 0.2px or odd offsets causing visual vibration)
    let misalignmentCount = 0;
    for (let j = 0; j < renderedBoxes.length; j++) {
      const boxA = renderedBoxes[j];
      const subPixelX = Math.abs(boxA.left - Math.round(boxA.left));
      const subPixelY = Math.abs(boxA.top - Math.round(boxA.top));
      if ((subPixelX > 0.05 && subPixelX < 0.95) || (subPixelY > 0.05 && subPixelY < 0.95)) {
        misalignmentCount++;
      }

      for (let k = j + 1; k < renderedBoxes.length; k++) {
        const boxB = renderedBoxes[k];
        // Check adjacent margins for 1px jitter/optical noise
        const horizGap = Math.abs(boxA.right - boxB.left);
        const vertGap = Math.abs(boxA.bottom - boxB.top);
        if ((horizGap > 0 && horizGap < 2 && horizGap !== 1) || (vertGap > 0 && vertGap < 2 && vertGap !== 1)) {
          opticalNoiseScore += 0.25;
        }
      }
    }

    if (misalignmentCount > 0) {
      opticalNoiseScore += misalignmentCount * 0.1;
    }

    // Zero Optical Noise strict enforcement
    if (opticalNoiseScore > 0) {
      violations.push({
        type: 'OPTICAL_NOISE',
        severity: 'BLOCKING',
        message: `Non-zero optical noise score detected: ${opticalNoiseScore.toFixed(3)}. Geometric alignment must be zero-jitter.`,
        actual: opticalNoiseScore.toFixed(3),
        expected: 0
      });
    }

    const passed = violations.filter(v => v.severity === 'BLOCKING').length === 0;
    auditResult = {
      passed,
      opticalNoiseScore,
      violations,
      evaluatedAt: new Date().toISOString()
    };
    return auditResult;
  }

  onMount(() => {
    if (mockupRoot) {
      runDeonticGate(mockupRoot);
    }
  });
</script>

<div class="deontic-gate-panel" data-testid="deontic-gate-panel">
  <div class="header flex items-center justify-between border-b border-[#11141c] pb-3 mb-4">
    <div class="flex items-center space-x-2">
      <span class="status-indicator w-3 h-3 rounded-full {auditResult.passed ? 'bg-emerald-500' : 'bg-red-500'}"></span>
      <h3 class="text-sm font-semibold tracking-wide text-slate-100 uppercase">BARCAN-TAG-11 Deontic Gate</h3>
    </div>
    <button
      type="button"
      on:click={() => runDeonticGate(mockupRoot)}
      class="px-3 py-1 text-xs font-mono font-medium rounded border border-[#10b981] text-[#10b981] hover:bg-[#10b981]/10 transition-colors"
      data-testid="run-audit-btn"
    >
      Audit Geometry
    </button>
  </div>

  <div class="metrics grid grid-cols-3 gap-2 mb-4 font-mono text-xs">
    <div class="p-2.5 rounded bg-[#11141c] border border-slate-800">
      <span class="text-slate-400 block">Status</span>
      <span class="font-bold {auditResult.passed ? 'text-emerald-400' : 'text-red-400'}" data-testid="gate-status">
        {auditResult.passed ? 'PASSED (AAA)' : 'BLOCKED'}
      </span>
    </div>
    <div class="p-2.5 rounded bg-[#11141c] border border-slate-800">
      <span class="text-slate-400 block">Optical Noise</span>
      <span class="font-bold {auditResult.opticalNoiseScore === 0 ? 'text-emerald-400' : 'text-red-400'}" data-testid="optical-noise-score">
        {auditResult.opticalNoiseScore.toFixed(2)}
      </span>
    </div>
    <div class="p-2.5 rounded bg-[#11141c] border border-slate-800">
      <span class="text-slate-400 block">Violations</span>
      <span class="font-bold {auditResult.violations.length === 0 ? 'text-emerald-400' : 'text-amber-400'}" data-testid="violation-count">
        {auditResult.violations.length}
      </span>
    </div>
  </div>

  {#if auditResult.violations.length > 0}
    <div class="violations-list space-y-2" data-testid="violations-container">
      {#each auditResult.violations as v}
        <div class="p-2 rounded text-xs border {v.severity === 'BLOCKING' ? 'border-red-900 bg-red-950/40 text-red-200' : 'border-amber-900 bg-amber-950/40 text-amber-200'}">
          <div class="flex justify-between items-center mb-1">
            <span class="font-mono font-bold text-[10px] uppercase tracking-wider px-1 rounded bg-black/40">{v.type}</span>
            {#if v.selector}<span class="font-mono text-slate-400 text-[10px]">{v.selector}</span>{/if}
          </div>
          <p class="mb-1">{v.message}</p>
          {#if v.actual !== undefined && v.expected !== undefined}
            <div class="font-mono text-[10px] text-slate-300">
              <span>Actual: {v.actual}</span> | <span>Expected: {v.expected}</span>
            </div>
          {/if}
        </div>
      {/each}
    </div>
  {:else}
    <div class="p-3 rounded bg-[#11141c]/50 border border-emerald-900/50 text-emerald-400 text-xs font-mono flex items-center space-x-2" data-testid="gate-clean">
      <svg class="w-4 h-4 fill-current" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/></svg>
      <span>Zero Optical Noise &amp; WCAG AAA Verified</span>
    </div>
  {/if}
</div>

<style>
  .deontic-gate-panel {
    background-color: #08090c;
    color: #e2e8f0;
    padding: 1rem;
    border-radius: 0.5rem;
    border: 1px solid #11141c;
  }
</style>
