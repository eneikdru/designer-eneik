package com.eneik.generated.designer.service;

import com.eneik.generated.designer.domain.CorrectionDirective;
import com.eneik.generated.designer.domain.DesignTokenSet;
import com.eneik.generated.designer.domain.LayoutGridPattern;
import com.eneik.generated.designer.domain.SynthesizedDomDocument;

import java.util.List;
import java.util.Map;

/**
 * Default implementation for synthesizing semantic HTML5 / Tailwind DOM documents.
 */
public class DefaultSemanticLayoutSynthesizer implements SemanticLayoutSynthesizer {

    @Override
    public SynthesizedDomDocument synthesizeLayout(
            DesignTokenSet tokenSet,
            LayoutGridPattern gridPattern,
            Map<String, String> projectAssetUrls,
            int roundIndex
    ) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Synthesized Layout</title>\n");
        html.append("    <style>\n");
        html.append("        :root {\n");
        if (tokenSet.typographyClamps() != null) {
            for (Map.Entry<String, String> entry : tokenSet.typographyClamps().entrySet()) {
                html.append("            --").append(entry.getKey()).append(": ").append(entry.getValue()).append(";\n");
            }
        }
        html.append("        }\n");
        html.append("    </style>\n");
        html.append("</head>\n");

        html.append("<body class=\"bg-[").append(tokenSet.baseBackgroundColor()).append("] text-white\">\n");
        html.append("    <main class=\"").append(gridPattern.gridClasses()).append(" p-4\">\n");

        html.append("        <div class=\"bg-[").append(tokenSet.surfaceBackgroundColor()).append("] border ").append(tokenSet.borderColor()).append(" p-6 rounded-lg\">\n");

        // Semantic, accessible form per requirements (WCAG 2.1 AA keyboard operability, visible focus, labelled controls)
        html.append("            <form>\n");
        html.append("                <label for=\"search-input\" class=\"sr-only\">Search</label>\n");
        html.append("                <input id=\"search-input\" type=\"text\" aria-label=\"Search\" class=\"focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white bg-transparent border ").append(gridPattern.borderToken()).append(" p-2\" placeholder=\"Search...\" tabindex=\"0\" />\n");
        html.append("                <button type=\"submit\" aria-label=\"Submit Search\" class=\"focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white bg-blue-600 p-2 rounded\" tabindex=\"0\">Submit</button>\n");
        html.append("            </form>\n");

        if (projectAssetUrls != null && !projectAssetUrls.isEmpty()) {
            html.append("            <div class=\"mt-4\">\n");
            for (Map.Entry<String, String> entry : projectAssetUrls.entrySet()) {
                html.append("                <img src=\"").append(entry.getValue()).append("\" alt=\"").append(entry.getKey()).append("\" class=\"max-w-full h-auto rounded\" />\n");
            }
            html.append("            </div>\n");
        }

        html.append("        </div>\n");
        html.append("    </main>\n");
        html.append("</body>\n");
        html.append("</html>");

        return SynthesizedDomDocument.create(html.toString(), roundIndex);
    }

    @Override
    public SynthesizedDomDocument refineLayout(
            SynthesizedDomDocument previousDocument,
            DesignTokenSet tokenSet,
            List<CorrectionDirective> directives,
            int roundIndex
    ) {
        if (previousDocument == null) {
            throw new IllegalArgumentException("previousDocument cannot be null");
        }

        String html = previousDocument.htmlContent();

        if (directives != null && !directives.isEmpty()) {
            StringBuilder extraStyle = new StringBuilder();
            extraStyle.append("\n    <style>\n");
            for (CorrectionDirective directive : directives) {
                extraStyle.append("        /* ").append(directive.reason()).append(" */\n");
                extraStyle.append("        ").append(directive.targetCssSelector()).append(" { ").append(directive.suggestedRule()).append("; }\n");
            }
            extraStyle.append("    </style>\n");
            html = html.replace("</head>", extraStyle.toString() + "</head>");
        }

        return SynthesizedDomDocument.create(html, roundIndex);
    }
}
