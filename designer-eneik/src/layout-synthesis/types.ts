export type ComponentType =
  | 'root'
  | 'container'
  | 'header'
  | 'navigation'
  | 'hero'
  | 'card'
  | 'grid'
  | 'button'
  | 'typography'
  | 'badge'
  | 'section'
  | 'fallback';

export interface SynthesisComponentSpec {
  id?: string;
  type: ComponentType | string;
  role?: string;
  title?: string;
  content?: string;
  variant?: 'primary' | 'secondary' | 'surface' | 'ghost';
  children?: SynthesisComponentSpec[];
  properties?: Record<string, unknown>;
}

export interface LayoutSynthesisRequest {
  viewId: string;
  title: string;
  components: SynthesisComponentSpec[];
  themeOverrides?: Partial<Record<string, string>>;
  seed?: number;
}

export interface DOMStyleMap {
  backgroundColor?: string;
  color?: string;
  borderColor?: string;
  padding?: string;
  margin?: string;
  borderRadius?: string;
  display?: string;
  flexDirection?: 'row' | 'column' | 'row-reverse' | 'column-reverse';
  gap?: string;
  gridTemplateColumns?: string;
  fontFamily?: string;
  fontSize?: string;
  fontWeight?: string;
  lineHeight?: string;
  minHeight?: string;
  width?: string;
  boxSizing?: 'border-box' | 'content-box';
  alignItems?: string;
  justifyContent?: string;
  border?: string;
}

export interface SynthesizedDOMNode {
  id: string;
  tag: string;
  role?: string;
  className: string;
  styles: DOMStyleMap;
  attributes: Record<string, string>;
  children: SynthesizedDOMNode[];
  textContent?: string;
  isFallback?: boolean;
}

export interface LayoutSynthesisResult {
  viewId: string;
  rootNode: SynthesizedDOMNode;
  nodeCount: number;
  unsupportedRecoveries: number;
  contrastValidation: {
    rootBg: string;
    surfaceBg: string;
    primaryAccent: string;
    wcagCompliant: boolean;
  };
}
