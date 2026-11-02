export interface DesignTokens {
  background: {
    root: string;
    surface: string;
    elevated: string;
  };
  accent: {
    primary: string;
    hover: string;
  };
  text: {
    primary: string;
    secondary: string;
    muted: string;
  };
  border: {
    subtle: string;
    default: string;
  };
}

export const OBSIDIAN_TOKENS: DesignTokens = {
  background: {
    root: "#08090C",
    surface: "#11141C",
    elevated: "#171B26"
  },
  accent: {
    primary: "#10B981", // emerald-500
    hover: "#059669"
  },
  text: {
    primary: "#F9FAFB",
    secondary: "#E5E7EB",
    muted: "#9CA3AF"
  },
  border: {
    subtle: "#1F2430",
    default: "#2A3245"
  }
};
