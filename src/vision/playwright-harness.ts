/**
 * Playwright Multi-Viewport Vision Rendering Harness
 * Role: BARCAN-TAG-02 - Backend API
 * JTBD: Executes visual layout verification across desktop (1440x900) and mobile (375x812)
 * viewports, extracting DOM geometries, reflow differentials, and pixel buffers.
 */

export const VIEWPORTS = {
  DESKTOP: { width: 1440, height: 900 },
  MOBILE: { width: 375, height: 812 },
} as const;

export const OBSIDIAN_TOKENS = {
  bgPrimary: '#08090C',
  bgSurface: '#11141C',
  accentEmerald: '#10b981',
} as const;

export interface DOMRectBox {
  x: number;
  y: number;
  width: number;
  height: number;
  top: number;
  right: number;
  bottom: number;
  left: number;
}

export interface ElementGeometry {
  selector: string;
  tag: string;
  id?: string;
  classes: string[];
  boundingBox: DOMRectBox;
  computedStyles?: {
    color?: string;
    backgroundColor?: string;
    display?: string;
  };
}

export interface RenderViewportResult {
  viewport: { width: number; height: number };
  elements: ElementGeometry[];
  screenshotBuffer: Buffer;
  renderedAt: string;
  durationMs: number;
}

export interface ReflowDifferential {
  selector: string;
  desktopBox: DOMRectBox;
  mobileBox: DOMRectBox;
  widthChange: number;
  heightChange: number;
  isReflowed: boolean;
}

export interface DiagnosticLog {
  level: 'info' | 'warn' | 'error';
  message: string;
  attempt?: number;
  timestamp: string;
  errorDetail?: string;
}

export interface MultiViewportAuditResult {
  layoutId: string;
  timestamp: string;
  desktop: RenderViewportResult;
  mobile: RenderViewportResult;
  reflows: ReflowDifferential[];
  diagnostics: DiagnosticLog[];
}

export interface SyntheticLayoutInput {
  html: string;
  css?: string;
  targetSelectors?: string[];
}

export interface PageLike {
  setViewportSize(size: { width: number; height: number }): Promise<void>;
  setContent(html: string, options?: { waitUntil?: string }): Promise<void>;
  evaluate<T>(fn: (selectors: string[]) => T, arg: string[]): Promise<T>;
  screenshot(options?: { type?: string }): Promise<Buffer>;
  close(): Promise<void>;
}

export interface BrowserLike {
  newPage(): Promise<PageLike>;
  close(): Promise<void>;
}

export interface BrowserLauncherLike {
  launch(options?: { headless?: boolean }): Promise<BrowserLike>;
}

export interface VisionHarnessOptions {
  maxRetries?: number;
  timeoutMs?: number;
  retryDelayMs?: number;
  browserLauncher?: BrowserLauncherLike;
  nowProvider?: () => Date;
  idGenerator?: () => string;
}

export class PlaywrightVisionHarness {
  private readonly maxRetries: number;
  private readonly timeoutMs: number;
  private readonly retryDelayMs: number;
  private readonly launcher?: BrowserLauncherLike;
  private readonly nowProvider: () => Date;
  private readonly idGenerator: () => string;

  constructor(options: VisionHarnessOptions = {}) {
    this.maxRetries = options.maxRetries ?? 2;
    this.timeoutMs = options.timeoutMs ?? 15000;
    this.retryDelayMs = options.retryDelayMs ?? 100;
    this.launcher = options.browserLauncher;
    this.nowProvider = options.nowProvider ?? (() => new Date());
    this.idGenerator = options.idGenerator ?? (() => `audit-${Math.random().toString(36).substring(2, 10)}`);
  }

  public async executeAudit(layout: SyntheticLayoutInput): Promise<MultiViewportAuditResult> {
    const diagnostics: DiagnosticLog[] = [];
    const auditId = this.idGenerator();
    const auditTimestamp = this.nowProvider().toISOString();

    const fullHtml = this.composeHtmlDocument(layout);
    const targetSelectors = layout.targetSelectors && layout.targetSelectors.length > 0
      ? layout.targetSelectors
      : ['header', 'main', 'nav', 'aside', 'footer', '.container', '[data-component]'];

    const desktopResult = await this.renderWithRetry(
      fullHtml,
      VIEWPORTS.DESKTOP,
      targetSelectors,
      diagnostics
    );

    const mobileResult = await this.renderWithRetry(
      fullHtml,
      VIEWPORTS.MOBILE,
      targetSelectors,
      diagnostics
    );

    const reflows = this.computeReflowDifferentials(desktopResult.elements, mobileResult.elements);

    diagnostics.push({
      level: 'info',
      message: `Vision audit ${auditId} completed across desktop and mobile viewports.`,
      timestamp: this.nowProvider().toISOString(),
    });

    return {
      layoutId: auditId,
      timestamp: auditTimestamp,
      desktop: desktopResult,
      mobile: mobileResult,
      reflows,
      diagnostics,
    };
  }

  private composeHtmlDocument(layout: SyntheticLayoutInput): string {
    const cssBlock = layout.css ? `<style>${layout.css}</style>` : '';
    return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <style>
    :root {
      --bg-primary: ${OBSIDIAN_TOKENS.bgPrimary};
      --bg-surface: ${OBSIDIAN_TOKENS.bgSurface};
      --accent-emerald: ${OBSIDIAN_TOKENS.accentEmerald};
    }
    body {
      margin: 0;
      padding: 0;
      background-color: var(--bg-primary);
      color: #FFFFFF;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    }
  </style>
  ${cssBlock}
</head>
<body>
  ${layout.html}
</body>
</html>`;
  }

  private async renderWithRetry(
    html: string,
    viewport: { width: number; height: number },
    selectors: string[],
    diagnostics: DiagnosticLog[]
  ): Promise<RenderViewportResult> {
    let attempt = 0;
    let lastError: Error | undefined;

    while (attempt <= this.maxRetries) {
      attempt += 1;
      const startTime = this.nowProvider().getTime();

      try {
        const result = await this.renderOnce(html, viewport, selectors, startTime);
        return result;
      } catch (err: unknown) {
        lastError = err instanceof Error ? err : new Error(String(err));
        const isLastAttempt = attempt > this.maxRetries;

        diagnostics.push({
          level: isLastAttempt ? 'error' : 'warn',
          message: isLastAttempt
            ? `Failed to render viewport ${viewport.width}x${viewport.height} after ${attempt} attempt(s).`
            : `Render timeout/failure on viewport ${viewport.width}x${viewport.height}. Retrying attempt ${attempt}...`,
          attempt,
          timestamp: this.nowProvider().toISOString(),
          errorDetail: lastError.message,
        });

        if (isLastAttempt) {
          throw new Error(
            `Viewport rendering failed for ${viewport.width}x${viewport.height} after ${this.maxRetries + 1} attempts: ${lastError.message}`
          );
        }

        await this.delay(this.retryDelayMs);
      }
    }

    throw lastError || new Error(`Unexpected failure rendering viewport ${viewport.width}x${viewport.height}`);
  }

  private async renderOnce(
    html: string,
    viewport: { width: number; height: number },
    selectors: string[],
    startTime: number
  ): Promise<RenderViewportResult> {
    if (!this.launcher) {
      throw new Error('No browser launcher configured for PlaywrightVisionHarness.');
    }

    const browser = await this.launcher.launch({ headless: true });
    try {
      const page = await browser.newPage();
      await page.setViewportSize(viewport);
      await page.setContent(html, { waitUntil: 'domcontentloaded' });

      const elements = await page.evaluate((targetSelectors: string[]) => {
        const matched: ElementGeometry[] = [];
        for (const sel of targetSelectors) {
          const found = document.querySelectorAll(sel);
          found.forEach((el, index) => {
            const rect = el.getBoundingClientRect();
            const computed = window.getComputedStyle(el);
            matched.push({
              selector: `${sel}:nth-of-type(${index + 1})`,
              tag: el.tagName.toLowerCase(),
              id: el.id || undefined,
              classes: Array.from(el.classList),
              boundingBox: {
                x: rect.x,
                y: rect.y,
                width: rect.width,
                height: rect.height,
                top: rect.top,
                right: rect.right,
                bottom: rect.bottom,
                left: rect.left,
              },
              computedStyles: {
                color: computed.color,
                backgroundColor: computed.backgroundColor,
                display: computed.display,
              },
            });
          });
        }
        return matched;
      }, selectors);

      const screenshotBuffer = await page.screenshot({ type: 'png' });
      const durationMs = this.nowProvider().getTime() - startTime;

      return {
        viewport,
        elements,
        screenshotBuffer,
        renderedAt: this.nowProvider().toISOString(),
        durationMs,
      };
    } finally {
      await browser.close();
    }
  }

  public computeReflowDifferentials(
    desktopElements: ElementGeometry[],
    mobileElements: ElementGeometry[]
  ): ReflowDifferential[] {
    const mobileMap = new Map<string, ElementGeometry>();
    for (const el of mobileElements) {
      mobileMap.set(el.selector, el);
    }

    const differentials: ReflowDifferential[] = [];
    for (const dEl of desktopElements) {
      const mEl = mobileMap.get(dEl.selector);
      if (mEl) {
        const widthChange = Number((mEl.boundingBox.width - dEl.boundingBox.width).toFixed(4));
        const heightChange = Number((mEl.boundingBox.height - dEl.boundingBox.height).toFixed(4));
        const isReflowed = Math.abs(widthChange) > 0.001 || Math.abs(heightChange) > 0.001;

        differentials.push({
          selector: dEl.selector,
          desktopBox: dEl.boundingBox,
          mobileBox: mEl.boundingBox,
          widthChange,
          heightChange,
          isReflowed,
        });
      }
    }
    return differentials;
  }

  private delay(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }
}
