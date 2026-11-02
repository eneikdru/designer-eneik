<script lang="ts">
  import Internal from './components/Internal.svelte';

  let mockupContainer: HTMLElement | null = null;
  let gateRef: any = null;

  function triggerEvaluation() {
    if (gateRef && mockupContainer) {
      gateRef.runDeonticGate(mockupContainer);
    }
  }
</script>

<main class="min-h-screen bg-[#08090c] text-slate-100 p-6 flex flex-col gap-6">
  <header class="border-b border-[#11141c] pb-4 flex justify-between items-center">
    <div>
      <h1 class="text-xl font-bold tracking-tight text-white flex items-center space-x-2">
        <span class="text-[#10b981]">Eneik</span> Designer Optical Synthesis
      </h1>
      <p class="text-xs text-slate-400 mt-1 font-mono">BARCAN-TAG-11 Deontic Gate Verification (Raz D006)</p>
    </div>
    <div class="flex space-x-3 items-center">
      <button
        type="button"
        on:click={triggerEvaluation}
        class="px-4 py-2 bg-[#10b981] hover:bg-[#059669] text-black font-semibold text-xs rounded transition-colors"
      >
        Re-evaluate Gate
      </button>
    </div>
  </header>

  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
    <!-- Mockup Canvas Area -->
    <section class="lg:col-span-2 flex flex-col gap-3">
      <h2 class="text-xs font-mono uppercase tracking-wider text-slate-400">Synthesized Mockup Canvas</h2>
      <div
        bind:this={mockupContainer}
        id="mockup-viewport"
        class="w-full min-h-[480px] bg-[#11141c] border border-slate-800 rounded-lg p-6 relative flex flex-col justify-between overflow-hidden shadow-2xl"
      >
        <div class="space-y-4">
          <div class="flex items-center justify-between border-b border-slate-800 pb-3">
            <div class="h-6 w-32 bg-[#10b981] rounded flex items-center justify-center text-[10px] font-bold text-black">
              DESIGNER-ENEIK
            </div>
            <nav class="flex space-x-4 text-xs font-mono text-slate-200">
              <span class="text-white font-semibold">Canvas</span>
              <span class="text-slate-300">Tokens</span>
              <span class="text-slate-300">Layout</span>
            </nav>
          </div>

          <div class="p-4 rounded bg-[#08090c] border border-slate-800 text-slate-200 text-sm leading-relaxed">
            Obsidian tokens active: Canvas base (#08090C), Container (#11141C), and emerald accent (#10B981). Elements adhere to 0 optical noise geometry standards with verified WCAG AAA contrast.
          </div>
        </div>

        <div class="grid grid-cols-2 gap-4 mt-6">
          <div class="p-4 rounded bg-[#08090c] border border-slate-800">
            <span class="text-xs font-mono text-slate-300 block mb-1 font-semibold">Ratio Spec</span>
            <p class="text-sm font-semibold text-white">WCAG AAA &gt;= 7.0:1</p>
          </div>
          <div class="p-4 rounded bg-[#08090c] border border-slate-800">
            <span class="text-xs font-mono text-slate-300 block mb-1 font-semibold">Optical Jitter</span>
            <p class="text-sm font-semibold text-emerald-400">0.000 (Zero Noise)</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Gate Audit Inspector -->
    <section class="flex flex-col gap-3">
      <h2 class="text-xs font-mono uppercase tracking-wider text-slate-400">Deontic Gate Inspector</h2>
      <Internal bind:this={gateRef} mockupRoot={mockupContainer} />
    </section>
  </div>
</main>
