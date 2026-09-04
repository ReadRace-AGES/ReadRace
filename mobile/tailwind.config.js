const { colors, typography, spacing, radius } = require('./src/theme/tokens');

const kebab = (name) => name.replace(/([a-z0-9])([A-Z])/g, '$1-$2').toLowerCase();

const themeColors = Object.fromEntries(Object.entries(colors).map(([k, v]) => [kebab(k), v]));

const fontSize = Object.fromEntries(
  Object.entries(typography.fontSize).map(([k, size]) => [
    k,
    [
      `${size}px`,
      `${Math.round(size * (k === 'display' ? typography.lineHeight.tight : typography.lineHeight.normal))}px`,
    ],
  ])
);

module.exports = {
  content: ['./App.tsx', './src/**/*.{js,jsx,ts,tsx}'],
  presets: [require('nativewind/preset')],
  theme: {
    colors: {
      transparent: 'transparent',
      ...themeColors,
    },
    fontFamily: {
      inter: [typography.fontFamily.regular],
      'inter-semibold': [typography.fontFamily.semibold],
      'inter-bold': [typography.fontFamily.bold],
    },
    fontSize,
    spacing: Object.fromEntries(Object.entries(spacing).map(([k, v]) => [k, `${v}px`])),
    borderRadius: {
      none: '0px',
      ...Object.fromEntries(Object.entries(radius).map(([k, v]) => [k, `${v}px`])),
    },
    extend: {},
  },
  plugins: [],
};
