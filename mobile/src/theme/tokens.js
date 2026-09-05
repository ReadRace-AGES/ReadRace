// Todo valor aqui foi medido nos frames do designer (node-id < 2011).
// Os 12 frames com node-id >= 2011 foram feitos as pressas e NAO servem de fonte.
// A tabela token -> valor -> frame de origem esta no README da raiz.
const colors = {
  primary: '#732634',
  primarySoft: '#823E4A',
  accent: '#C9425B',

  surface: '#FEFEFE',
  surfaceMuted: '#F5F5F5',
  surfaceAlt: '#E4E4E4',
  surfacePink: '#F6E2E2',
  surfacePinkStrong: '#EFC7CF',
  surfaceDisabled: '#EDEDED',
  overlay: 'rgba(0, 0, 0, 0.65)',
  chipOnPrimary: 'rgba(255, 255, 255, 0.11)',

  border: '#EEEEEE',
  borderStrong: '#D1D1D6',
  inputBorder: '#CBD5E1',

  text: '#1E1E1E',
  textSecondary: '#888888',
  textMuted: '#A3A3A3',
  textInverse: '#FEFEFE',

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
};

const typography = {
  fontFamily: {
    regular: 'Inter_400Regular',
    semibold: 'Inter_600SemiBold',
    bold: 'Inter_700Bold',
    extrabold: 'Inter_800ExtraBold',
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
    // `heading` e o titulo dentro do cabecalho vermelho: 21.395px em fonte de 24px
    // (`menu - comunidades` 3-7 e `desafios` 3-8, ambos com caixa de texto de 22px).
    heading: 0.9,
    tight: 1.25,
    normal: 1.5,
  },
  // O Figma aplica -0.011em em todo texto: -0.176px@16, -0.154px@14, -0.132px@12
  // (`card` 403-404). Sem isso cada linha do app sai mais larga que o design.
  letterSpacingRatio: -0.011,
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
  inputHeight: 40,
  navHeight: 76,
  navIcon: 28,
  navIndicatorWidth: 24,
  navIndicatorHeight: 3,
  headerHeight: 192,
  progressTrackHeight: 8,
  chipHeight: 23,
  icon: 24,
  iconSmall: 16,
  // Dois tamanhos distintos, nao um so: 39-42px no post, no card de desafio e na
  // linha do ranking; 50px no item de lista de comunidade (`menu - comunidades` 3-7).
  avatar: 40,
  avatarLarge: 50,
  borderWidth: 1,
};

// Pares de parada dos gradientes do design, na ordem que o LinearGradient espera.
const gradients = {
  navInactive: [colors.navInactiveStart, colors.navInactiveEnd],
  rankGold: [colors.rankGoldStart, colors.rankGoldEnd],
  rankSilver: [colors.rankSilverStart, colors.rankSilverEnd],
  rankBronze: [colors.rankBronzeStart, colors.rankBronzeEnd],
};

module.exports = { colors, typography, spacing, radius, shadows, sizes, gradients };
