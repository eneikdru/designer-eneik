import { test, expect } from '@playwright/test';

const SYNTHESIS_HTML = `
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Sovereign Designer Layout Synthesis</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background-color: #08090C;
      color: #F9FAFB;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      min-height: 100vh;
      overflow-x: hidden;
    }
    .container {
      display: flex;
      width: 100%;
      min-height: 100vh;
      flex-direction: row;
    }
    .sidebar {
      width: 280px;
      background-color: #11141C;
      border-right: 1px solid #1F2430;
      padding: 24px;
      flex-shrink: 0;
    }
    .canvas {
      flex: 1;
      padding: 32px;
      display: flex;
      flex-direction: column;
      gap: 24px;
    }
    .card {
      background-color: #11141C;
      border: 1px solid #1F2430;
      border-radius: 8px;
      padding: 24px;
    }
    .accent-badge {
      display: inline-block;
      color: #10B981;
      font-weight: 600;
      font-size: 14px;
      margin-bottom: 8px;
    }
    @media (max-width: 768px) {
      .container {
        flex-direction: column;
      }
      .sidebar {
        width: 100%;
        border-right: none;
        border-bottom: 1px solid #1F2430;
        padding: 16px;
      }
      .canvas {
        padding: 16px;
      }
    }
  </style>
</head>
<body>
  <div class="container" id="root-container">
    <aside class="sidebar" id="nav-sidebar">
      <div class="accent-badge">Sovereign Core</div>
      <h2>Navigation</h2>
    </aside>
    <main class="canvas" id="main-canvas">
      <section class="card" id="hero-card">
        <span class="accent-badge">Deontic Guard</span>
        <h1>Obsidian Layout Synthesis</h1>
        <p>Mathematical proof of zero optical noise and verified contrast.</p>
      </section>
    </main>
  </div>
</body>
</html>
`;

test.describe('Integration: Multi-Viewport DOM Geometry Validation', () => {
  test('Validates desktop DOM geometry matrix at 1440x900', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'desktop-1440x900', 'Desktop specific verification');

    await page.setContent(SYNTHESIS_HTML);
    await page.waitForLoadState('domcontentloaded');

    const containerBox = await page.locator('#root-container').boundingBox();
    expect(containerBox).not.toBeNull();
    expect(containerBox!.width).toBe(1440);
    expect(containerBox!.height).toBeGreaterThanOrEqual(900);

    const sidebarBox = await page.locator('#nav-sidebar').boundingBox();
    expect(sidebarBox).not.toBeNull();
    expect(sidebarBox!.width).toBe(280);
    expect(sidebarBox!.x).toBe(0);

    const canvasBox = await page.locator('#main-canvas').boundingBox();
    expect(canvasBox).not.toBeNull();
    expect(canvasBox!.x).toBe(280);
    expect(canvasBox!.width).toBe(1440 - 280);

    const scrollMetrics = await page.evaluate(() => ({
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
    }));
    expect(scrollMetrics.scrollWidth).toBeLessThanOrEqual(scrollMetrics.clientWidth);
  });

  test('Validates mobile DOM geometry matrix at 375x812', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'mobile-375x812', 'Mobile specific verification');

    await page.setContent(SYNTHESIS_HTML);
    await page.waitForLoadState('domcontentloaded');

    const containerBox = await page.locator('#root-container').boundingBox();
    expect(containerBox).not.toBeNull();
    expect(containerBox!.width).toBe(375);
    expect(containerBox!.height).toBeGreaterThanOrEqual(812);

    const sidebarBox = await page.locator('#nav-sidebar').boundingBox();
    expect(sidebarBox).not.toBeNull();
    expect(sidebarBox!.width).toBe(375);
    expect(sidebarBox!.x).toBe(0);

    const canvasBox = await page.locator('#main-canvas').boundingBox();
    expect(canvasBox).not.toBeNull();
    expect(canvasBox!.width).toBe(375);
    expect(canvasBox!.x).toBe(0);
    expect(canvasBox!.y).toBeGreaterThanOrEqual(sidebarBox!.height);

    const scrollMetrics = await page.evaluate(() => ({
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
    }));
    expect(scrollMetrics.scrollWidth).toBeLessThanOrEqual(scrollMetrics.clientWidth);
  });
});
