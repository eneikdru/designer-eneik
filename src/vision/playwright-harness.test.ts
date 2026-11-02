import {
  PlaywrightVisionHarness,
  VIEWPORTS,
  OBSIDIAN_TOKENS,
  BrowserLauncherLike,
  BrowserLike,
  PageLike,
  ElementGeometry,
  DOMRectBox,
} from './playwright-harness';

function approxEqual(actual: number, expected: number, epsilon = 0.001): boolean {
  return Math.abs(actual - expected) <= epsilon;
}

class MockPage implements PageLike {
  public currentViewport = { width: 0, height: 0 };
  public currentHtml = '';

  constructor(
    private readonly elementsProvider: (viewport: { width: number; height: number }) => ElementGeometry[],
    private readonly shouldFailCount = 0
  ) {}

  private static failureCounter = 0;

  async setViewportSize(size: { width: number; height: number }): Promise<void> {
    this.currentViewport = size;
  }

  async setContent(html: string): Promise<void> {
    this.currentHtml = html;
    if (MockPage.failureCounter < this.shouldFailCount) {
      MockPage.failureCounter += 1;
      throw new Error(`Simulated page timeout (attempt ${MockPage.failureCounter})`);
    }
  }

  async evaluate<T>(fn: (selectors: string[]) => T, arg: string[]): Promise<T> {
    return this.elementsProvider(this.currentViewport) as unknown as T;
  }

  async screenshot(): Promise<Buffer> {
    return Buffer.from('mock-png-pixel-buffer');
  }

  async close(): Promise<void> {}

  static resetFailureCounter(): void {
    MockPage.failureCounter = 0;
  }
}

class MockBrowser implements BrowserLike {
  constructor(
    private readonly elementsProvider: (viewport: { width: number; height: number }) => ElementGeometry[],
    private readonly failAttempts = 0
  ) {}

  async newPage(): Promise<PageLike> {
    return new MockPage(this.elementsProvider, this.failAttempts);
  }

  async close(): Promise<void> {}
}

class MockBrowserLauncher implements BrowserLauncherLike {
  constructor(
    private readonly elementsProvider: (viewport: { width: number; height: number }) => ElementGeometry[],
    private readonly failAttempts = 0
  ) {}

  async launch(): Promise<BrowserLike> {
    return new MockBrowser(this.elementsProvider, this.failAttempts);
  }
}

describe('PlaywrightVisionHarness', () => {
  const fixedTime = new Date('2026-11-02T12:00:00.000Z');
  const nowProvider = () => fixedTime;
  const idGenerator = () => 'audit-fixed-seed-1234';

  beforeEach(() => {
    MockPage.resetFailureCounter();
  });

  test('renders desktop view with 1440x900 viewport and extracts element bounding boxes', async () => {
    const mockElements = (viewport: { width: number; height: number }): ElementGeometry[] => [
      {
        selector: 'main:nth-of-type(1)',
        tag: 'main',
        classes: ['container'],
        boundingBox: {
          x: 0,
          y: 0,
          width: viewport.width,
          height: viewport.height,
          top: 0,
          right: viewport.width,
          bottom: viewport.height,
          left: 0,
        },
        computedStyles: {
          backgroundColor: OBSIDIAN_TOKENS.bgPrimary,
          color: '#FFFFFF',
          display: 'block',
        },
      },
    ];

    const launcher = new MockBrowserLauncher(mockElements);
    const harness = new PlaywrightVisionHarness({
      browserLauncher: launcher,
      nowProvider,
      idGenerator,
    });

    const result = await harness.executeAudit({
      html: '<main class="container">Semantic Content</main>',
    });

    expect(result.layoutId).toBe('audit-fixed-seed-1234');
    expect(result.desktop.viewport.width).toBe(1440);
    expect(result.desktop.viewport.height).toBe(900);
    expect(result.desktop.elements).toHaveLength(1);

    const desktopBox = result.desktop.elements[0].boundingBox;
    expect(approxEqual(desktopBox.width, 1440.0)).toBe(true);
    expect(approxEqual(desktopBox.height, 900.0)).toBe(true);
    expect(result.desktop.screenshotBuffer.toString()).toBe('mock-png-pixel-buffer');
  });

  test('renders mobile view with 375x812 viewport and captures reflow differentials', async () => {
    const mockElements = (viewport: { width: number; height: number }): ElementGeometry[] => {
      const isMobile = viewport.width === VIEWPORTS.MOBILE.width;
      return [
        {
          selector: '.container:nth-of-type(1)',
          tag: 'div',
          classes: ['container'],
          boundingBox: {
            x: 0,
            y: 0,
            width: isMobile ? 375 : 1440,
            height: isMobile ? 812 : 900,
            top: 0,
            right: isMobile ? 375 : 1440,
            bottom: isMobile ? 812 : 900,
            left: 0,
          },
        },
      ];
    };

    const launcher = new MockBrowserLauncher(mockElements);
    const harness = new PlaywrightVisionHarness({
      browserLauncher: launcher,
      nowProvider,
      idGenerator,
    });

    const result = await harness.executeAudit({
      html: '<div class="container">Card layout</div>',
      targetSelectors: ['.container'],
    });

    expect(result.mobile.viewport.width).toBe(375);
    expect(result.mobile.viewport.height).toBe(812);
    expect(result.reflows).toHaveLength(1);

    const reflow = result.reflows[0];
    expect(reflow.selector).toBe('.container:nth-of-type(1)');
    expect(approxEqual(reflow.desktopBox.width, 1440.0)).toBe(true);
    expect(approxEqual(reflow.mobileBox.width, 375.0)).toBe(true);
    expect(approxEqual(reflow.widthChange, -1065.0)).toBe(true);
    expect(reflow.isReflowed).toBe(true);
  });

  test('retries on rendering failure and logs descriptive diagnostics', async () => {
    const mockElements = (): ElementGeometry[] => [];
    // 1 transient failure, then success (within maxRetries: 2)
    const launcher = new MockBrowserLauncher(mockElements, 1);
    const harness = new PlaywrightVisionHarness({
      browserLauncher: launcher,
      maxRetries: 2,
      retryDelayMs: 5,
      nowProvider,
      idGenerator,
    });

    const result = await harness.executeAudit({
      html: '<main>Retry Test</main>',
    });

    const warnLogs = result.diagnostics.filter((d) => d.level === 'warn');
    expect(warnLogs.length).toBeGreaterThanOrEqual(1);
    expect(warnLogs[0].attempt).toBe(1);
    expect(warnLogs[0].message).toContain('Retrying attempt 1');
    expect(warnLogs[0].errorDetail).toContain('Simulated page timeout');
  });

  test('throws and emits error diagnostics when retry limit is exceeded', async () => {
    const mockElements = (): ElementGeometry[] => [];
    // 3 failures exceeds maxRetries: 1
    const launcher = new MockBrowserLauncher(mockElements, 3);
    const harness = new PlaywrightVisionHarness({
      browserLauncher: launcher,
      maxRetries: 1,
      retryDelayMs: 5,
      nowProvider,
      idGenerator,
    });

    await expect(harness.executeAudit({ html: '<div>Failure</div>' })).rejects.toThrow(
      /Viewport rendering failed for 1440x900 after 2 attempts/
    );
  });

  test('injects Obsidian design tokens into synthesized semantic layout document', () => {
    const harness = new PlaywrightVisionHarness({ nowProvider, idGenerator });
    // Access private composeHtmlDocument via any cast for unit test verification
    const fullHtml = (harness as unknown as {
      composeHtmlDocument(layout: { html: string; css?: string }): string;
    }).composeHtmlDocument({ html: '<p>Obsidian</p>' });

    expect(fullHtml).toContain(OBSIDIAN_TOKENS.bgPrimary);
    expect(fullHtml).toContain(OBSIDIAN_TOKENS.bgSurface);
    expect(fullHtml).toContain(OBSIDIAN_TOKENS.accentEmerald);
  });
});
