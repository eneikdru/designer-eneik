from typing import List, Dict, Any, Optional
from pydantic import BaseModel, Field

class StyleProperties(BaseModel):
    background_color: Optional[str] = None
    color: Optional[str] = None
    padding: Optional[str] = None
    margin: Optional[str] = None
    border_radius: Optional[str] = None
    border: Optional[str] = None
    display: Optional[str] = None
    flex_direction: Optional[str] = None
    gap: Optional[str] = None

class DomNode(BaseModel):
    id: str
    tag: str
    role: Optional[str] = None
    classes: List[str] = Field(default_factory=list)
    styles: StyleProperties = Field(default_factory=StyleProperties)
    attributes: Dict[str, str] = Field(default_factory=dict)
    children: List['DomNode'] = Field(default_factory=list)
    text_content: Optional[str] = None

DomNode.update_forward_refs()

class SynthesisRequest(BaseModel):
    view_type: str
    components: List[str] = Field(default_factory=list)
    theme: Optional[str] = "obsidian"
    metadata: Dict[str, Any] = Field(default_factory=dict)

class SynthesisResponse(BaseModel):
    view_type: str
    theme: str
    root: DomNode
    synthesis_status: str
    fallback_applied: bool = False
