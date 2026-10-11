const { colors } = require('./src/theme/tokens');

module.exports = {
  expo: {
    name: 'ReadRace',
    slug: 'readrace',
    version: '0.1.0',
    orientation: 'portrait',
    icon: './assets/images/icon.png',
    scheme: 'readrace',
    userInterfaceStyle: 'automatic',
    android: {
      package: 'com.readrace.app',
      versionCode: 1,
      adaptiveIcon: {
        // TEMPORÁRIO (#159): o foreground é o ícone inteiro do Figma (arte + fundo embutido)
        // e o background é a cor sólida desse fundo (#A1312F, não o #732634 da marca). Solução
        // rápida sem recorte. O ideal é exportar o foreground isolado (transparente) do Figma e
        // usar backgroundColor = colors.primary. O monochromeImage ainda é o antigo.
        backgroundColor: '#A1312F',
        foregroundImage: './assets/images/android-icon-foreground.png',
        backgroundImage: './assets/images/android-icon-background.png',
        monochromeImage: './assets/images/android-icon-monochrome.png',
      },
      predictiveBackGestureEnabled: false,
    },
    web: {
      output: 'static',
      favicon: './assets/images/favicon.png',
    },
    plugins: [
      'expo-router',
      'expo-secure-store',
      [
        'expo-splash-screen',
        {
          backgroundColor: colors.primary,
          image: './assets/images/splash-icon.png',
          imageWidth: 76,
        },
      ],
    ],
    experiments: {
      typedRoutes: true,
      reactCompiler: true,
    },
  },
};
