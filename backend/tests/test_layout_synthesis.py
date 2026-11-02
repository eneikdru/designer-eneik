import pytest
from backend.src.layout_synthesis.engine import SemanticLayoutSynthesisEngine
from backend.src.layout_synthesis.models import SynthesisRequest

def test_synthesize_valid_layout_applies_obsidian_tokens():
    engine = SemanticLayoutSynthesisEngine()
    request = SynthesisRequest(
        view_type="dashboard",
        components=["header", "card", "action_button"]
    )
    response = engine.synthesize(request)

    assert response.synthesis_status == "success"
    assert not response.fallback_applied
    assert response.root.tag == "main"
    assert response.root.styles.background_color == "#08090C"

    # Verify surface card token binding
    card_node = next(node for node in response.root.children if "semantic-card" in node.classes)
    assert card_node.styles.background_color == "#11141C"

    # Verify accent token binding (#10b981 emerald-500)
    btn_node = next(node for node in response.root.children if "semantic-action-btn" in node.classes)
    assert btn_node.styles.background_color == "#10b981"

def test_synthesize_malformed_or_unknown_component_recovers_gracefully():
    engine = SemanticLayoutSynthesisEngine()
    request = SynthesisRequest(
        view_type="custom_view",
        components=["unknown_widget_v99"]
    )
    response = engine.synthesize(request)

    assert response.synthesis_status == "success"
    assert response.fallback_applied is True
    assert len(response.root.children) == 1
    fallback_node = response.root.children[0]
    assert "semantic-fallback-container" in fallback_node.classes
    assert fallback_node.styles.background_color == "#11141C"
    assert "unknown_widget_v99" in fallback_node.text_content
