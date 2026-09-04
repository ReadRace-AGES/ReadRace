const colors = {
  primary: '#732634',
  primarySoft: '#823E4A',
  accent: '#C9425B',

  surface: '#FFFFFF',
  surfaceMuted: '#F5F5F5',
  surfaceAlt: '#E4E4E4',
  surfacePink: '#F6E2E2',
  surfacePinkStrong: '#EFC7CF',
  surfaceDisabled: '#EDEDED',
  overlay: 'rgba(0, 0, 0, 0.32)',

  border: '#EEEEEE',
  borderStrong: '#D1D1D6',

  text: '#1E1E1E',
  textSecondary: '#888888',
  textMuted: '#A3A3A3',
  textInverse: '#FFFFFF',

  navInactiveStart: '#D98B9A',
  navInactiveEnd: '#AE7E86',
  navInactive: '#C1848F',

  progressTrack: '#D9D9D9',
  rankGoldStart: '#EBE28D',
  rankGoldEnd: '#B7B57D',
  rankSilverStart: '#B3A9A9',
  rankSilverEnd: '#8A8787',
  rankBronzeStart: '#794E17',
  rankBronzeEnd: '#B07322',

  success: '#4CAF50',
  danger: '#E53935',
};

const typography = {
  fontFamily: {
    regular: 'Inter_400Regular',
    semibold: 'Inter_600SemiBold',
    bold: 'Inter_700Bold',
  },
  fontSize: {
    display: 32,
    h1: 24,
    h2: 20,
    h3: 18,
    body: 16,
    bodySmall: 14,
    caption: 12,
    micro: 10,
  },
  lineHeight: {
    tight: 1.25,
    normal: 1.5,
  },
};

const spacing = {
  0: 0,
  1: 4,
  2: 8,
  3: 12,
  4: 16,
  5: 20,
  6: 24,
  8: 32,
  10: 40,
};

const radius = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 32,
  pill: 9999,
};

const shadows = {
  button: {
    shadowColor: '#000000',
    shadowOpacity: 0.25,
    shadowRadius: 4,
    shadowOffset: { width: 0, height: 2 },
    elevation: 3,
  },
  floating: {
    shadowColor: '#000000',
    shadowOpacity: 0.08,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 2 },
    elevation: 6,
  },
};

const sizes = {
  buttonHeight: 40,
  inputHeight: 44,
  navHeight: 76,
  navIcon: 28,
  navIndicatorWidth: 24,
  navIndicatorHeight: 3,
  icon: 24,
  avatar: 40,
  borderWidth: 1,
};

module.exports = { colors, typography, spacing, radius, shadows, sizes };
