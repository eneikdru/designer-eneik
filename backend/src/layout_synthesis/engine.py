import uuid
from typing import List
from .models import SynthesisRequest, SynthesisResponse, DomNode, StyleProperties
from .tokens import OBSIDIAN_TOKENS

class SemanticLayoutSynthesisEngine:
    """Synthesizes concrete semantic DOM trees bound with Obsidian design tokens."""

    KNOWN_COMPONENTS = {"header", "card", "hero", "sidebar", "action_button", "content_grid"}

    def __init__(self, token_provider: dict = None):
        self.tokens = token_provider or OBSIDIAN_TOKENS

    def synthesize(self, request: SynthesisRequest) -> SynthesisResponse:
        fallback_applied = False
        root = DomNode(
            id=f"root-{uuid.uuid4().hex[:8]}",
            tag="main",
            role="main",
            classes=["layout-root", "obsidian-theme"],
            styles=StyleProperties(
                background_color=self.tokens["color"]["background"],
                color=self.tokens["color"]["text_primary"],
                padding=self.tokens["spacing"]["root_padding"],
                display="flex",
                flex_direction="column",
                gap=self.tokens["spacing"]["layout_gap"]
            )
        )

        requested_components = request.components if request.components else ["header", "card", "action_button"]

        for comp_name in requested_components:
            if comp_name not in self.KNOWN_COMPONENTS:
                fallback_applied = True
                node = self._build_fallback_component(comp_name)
            else:
                node = self._build_component(comp_name)
            root.children.append(node)

        return SynthesisResponse(
            view_type=request.view_type,
            theme=request.theme or "obsidian",
            root=root,
            synthesis_status="success",
            fallback_applied=fallback_applied
        )

    def _build_component(self, name: str) -> DomNode:
        node_id = f"{name}-{uuid.uuid4().hex[:8]}"
        if name == "header":
            return DomNode(
                id=node_id,
                tag="header",
                role="banner",
                classes=["semantic-header"],
                styles=StyleProperties(
                    background_color=self.tokens["color"]["surface"],
                    border_radius=self.tokens["radius"]["card"],
                    padding=self.tokens["spacing"]["card_padding"],
                    display="flex",
                    border=f"1px solid {self.tokens['color']['border']}"
                ),
                text_content="Dashboard Overview"
            )
        elif name == "card":
            return DomNode(
                id=node_id,
                tag="section",
                role="region",
                classes=["semantic-card"],
                styles=StyleProperties(
                    background_color=self.tokens["color"]["surface"],
                    border_radius=self.tokens["radius"]["card"],
                    padding=self.tokens["spacing"]["card_padding"],
                    border=f"1px solid {self.tokens['color']['border']}"
                ),
                text_content="Primary synthesized content surface"
            )
        elif name == "action_button":
            return DomNode(
                id=node_id,
                tag="button",
                role="button",
                classes=["semantic-action-btn"],
                styles=StyleProperties(
                    background_color=self.tokens["color"]["accent_primary"],
                    color=self.tokens["color"]["background"],
                    border_radius=self.tokens["radius"]["button"],
                    padding="0.5rem 1rem"
                ),
                text_content="Execute Action"
            )
        elif name == "hero":
            return DomNode(
                id=node_id,
                tag="div",
                role="banner",
                classes=["semantic-hero"],
                styles=StyleProperties(
                    background_color=self.tokens["color"]["surface"],
                    padding=self.tokens["spacing"]["card_padding"],
                    border_radius=self.tokens["radius"]["card"]
                ),
                text_content="Hero Header Area"
            )
        elif name == "sidebar":
            return DomNode(
                id=node_id,
                tag="aside",
                role="complementary",
                classes=["semantic-sidebar"],
                styles=StyleProperties(
                    background_color=self.tokens["color"]["surface"],
                    padding=self.tokens["spacing"]["card_padding"]
                )
            )
        else:  # content_grid
            return DomNode(
                id=node_id,
                tag="div",
                role="grid",
                classes=["semantic-grid"],
                styles=StyleProperties(
                    display="grid",
                    gap=self.tokens["spacing"]["layout_gap"]
                )
            )

    def _build_fallback_component(self, name: str) -> DomNode:
        return DomNode(
            id=f"fallback-{uuid.uuid4().hex[:8]}",
            tag="div",
            role="region",
            classes=["semantic-fallback-container"],
            styles=StyleProperties(
                background_color=self.tokens["color"]["surface"],
                border=f"1px dashed {self.tokens['color']['border']}",
                padding=self.tokens["spacing"]["card_padding"],
                border_radius=self.tokens["radius"]["card"]
            ),
            text_content=f"Synthesized fallback container for unsupported node: {name}"
        )
