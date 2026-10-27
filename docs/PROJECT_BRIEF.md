# Project Brief

## Customer Job

The client's own entries, quoted verbatim and in the client's own language. This file is
maintained by the factory from the wishlist and must not be edited by hand or translated -
it is the referent every later artifact is checked against.

1. Построить автономную подсистему «Суверенный Дизайнер» (Autonomous Sovereign Designer Engine) для Фабрики EneikProductionSys без зависимости от платного Stitch.
   
   Цель: Автономная генерация UI/UX дизайн-макетов (mockup.html) уровня сайтов-миллионников (Stripe, Linear, Apple, Vercel, Mercury) для любых продуктов и архетипов (DevTools, FinTech, Editorial, Consumer).
   
   Ключевые компоненты к реализации:
   1. DesignTokenCompiler: классификатор архетипа продукта, расчет адаптивной типографики clamp(...) и контрастности по APCA/WCAG AAA (минимум 7:1 к фону).
   2. LayoutPatternRegistry: библиотека композиционных сеток сайтов-миллионников (швейцарская журнальная асимметрия 7/5 и 8/4, Bento Grid 2.0, многоуровневый обсидиан #08090C -> #11141C с 1px границами border-white/[0.07]).
   3. SemanticLayoutSynthesizer: семантический HTML5/Tailwind DOM-генератор с поддержкой реальных медиа-ассетов проекта и доступностью по Tab (focus-visible).
   4. PlaywrightVisionHarness: аппаратный рендеринг скриншотов на двух обязательных вьюпортах: 1440px десктоп и 375px мобайл.
   5. VisionAuditLoop: зрительный аудит по деонтическим гейтам BARCAN-TAG-11 (нулевой шум Leica/GPS, отсутствие горизонтального скролла на 375px) с итеративным циклом исправления дефектов (до 3 раундов, целевой score >= 9.2).
   6. Интеграция в конвейер Фабрики: создание сервиса SovereignDesignSynthesisService и подключение к DesignAssetService.java.
   
   Полная спецификация зафиксирована в docs/design/TZ_AUTONOMOUS_DESIGNER_ENGINE.md.

---

Entries: 1. Anything this product claims - a page
heading, a filter, a capability - must trace to one of them or to a declared route.
