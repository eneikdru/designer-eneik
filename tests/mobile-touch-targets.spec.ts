import { test, expect } from '@playwright/test';

const MOBILE_VIEWPORTS = [
  { name: 'Mobile 375px', width: 375, height: 667 },
  { name: 'Mobile 414px', width: 414, height: 896 },
];

for (const viewport of MOBILE_VIEWPORTS) {
  test.describe(`Mobile Touch Target 44x44px Verification - ${viewport.name}`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('all interactive buttons, inputs, and links meet 44x44px minimum target bounding box', async ({ page }) => {
      await page.goto('/');
      await page.waitForLoadState('domcontentloaded');

      const violations = await page.evaluate(() => {
        const interactiveSelector = [
          'button',
          'a[href]',
          'input:not([type="hidden"])',
          'select',
          'textarea',
          '[role="button"]',
          '[role="link"]',
          '[role="checkbox"]',
          '[role="radio"]',
          '[role="tab"]',
          '[role="switch"]',
          '[tabindex]:not([tabindex="-1"])',
        ].join(', ');

        const elements = Array.from(document.querySelectorAll<HTMLElement>(interactiveSelector));
        const nonCompliant: Array<{
          tag: string;
          id: string;
          className: string;
          text: string;
          width: number;
          height: number;
        }> = [];

        for (const el of elements) {
          const style = window.getComputedStyle(el);
          if (
            style.display === 'none' ||
            style.visibility === 'hidden' ||
            style.opacity === '0' ||
            el.getAttribute('aria-hidden') === 'true'
          ) {
            continue;
          }

          const rect = el.getBoundingClientRect();
          if (rect.width === 0 && rect.height === 0) {
            continue;
          }

          let effectiveWidth = rect.width;
          let effectiveHeight = rect.height;

          const beforeStyle = window.getComputedStyle(el, '::before');
          const afterStyle = window.getComputedStyle(el, '::after');

          if (beforeStyle.content !== 'none' && beforeStyle.position === 'absolute') {
            const beforeWidth = parseFloat(beforeStyle.width) || 0;
            const beforeHeight = parseFloat(beforeStyle.height) || 0;
            effectiveWidth = Math.max(effectiveWidth, beforeWidth);
            effectiveHeight = Math.max(effectiveHeight, beforeHeight);
          }

          if (afterStyle.content !== 'none' && afterStyle.position === 'absolute') {
            const afterWidth = parseFloat(afterStyle.width) || 0;
            const afterHeight = parseFloat(afterStyle.height) || 0;
            effectiveWidth = Math.max(effectiveWidth, afterWidth);
            effectiveHeight = Math.max(effectiveHeight, afterHeight);
          }

          if (effectiveWidth < 43.5 || effectiveHeight < 43.5) {
            nonCompliant.push({
              tag: el.tagName.toLowerCase(),
              id: el.id,
              className: el.className,
              text: (el.innerText || el.getAttribute('aria-label') || '').slice(0, 30).trim(),
              width: Math.round(effectiveWidth * 10) / 10,
              height: Math.round(effectiveHeight * 10) / 10,
            });
          }
        }

        return nonCompliant;
      });

      expect(violations, `Interactive elements below 44x44px minimum: ${JSON.stringify(violations, null, 2)}`).toEqual([]);
    });

    test('touch target padding or touch expansion pseudo-elements maintain at least 44x44px dimensions without overlapping adjacent controls', async ({ page }) => {
      await page.goto('/');
      await page.waitForLoadState('domcontentloaded');

      const overlaps = await page.evaluate(() => {
        const interactiveSelector = 'button, a[href], input:not([type="hidden"]), select, textarea, [role="button"], [role="link"]';
        const elements = Array.from(document.querySelectorAll<HTMLElement>(interactiveSelector)).filter((el) => {
          const style = window.getComputedStyle(el);
          const rect = el.getBoundingClientRect();
          return (
            style.display !== 'none' &&
            style.visibility !== 'hidden' &&
            style.opacity !== '0' &&
            rect.width > 0 &&
            rect.height > 0
          );
        });

        const overlappingPairs: Array<{ first: string; second: string }> = [];

        for (let i = 0; i < elements.length; i++) {
          const rectA = elements[i].getBoundingClientRect();
          for (let j = i + 1; j < elements.length; j++) {
            if (elements[i].contains(elements[j]) || elements[j].contains(elements[i])) {
              continue;
            }

            const rectB = elements[j].getBoundingClientRect();
            const intersects = !(
              rectA.right <= rectB.left ||
              rectA.left >= rectB.right ||
              rectA.bottom <= rectB.top ||
              rectA.top >= rectB.bottom
            );

            if (intersects) {
              overlappingPairs.push({
                first: `${elements[i].tagName.toLowerCase()}#${elements[i].id || elements[i].className}`,
                second: `${elements[j].tagName.toLowerCase()}#${elements[j].id || elements[j].className}`,
              });
            }
          }
        }

        return overlappingPairs;
      });

      expect(overlaps, `Overlapping interactive controls detected: ${JSON.stringify(overlaps, null, 2)}`).toEqual([]);
    });
  });
}
