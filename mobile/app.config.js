const { colors } = require('./src/theme/tokens');

// Config dinâmica: liga o tráfego HTTP (cleartext) no Android SÓ quando a API é http://,
// que é o caso dos builds de teste contra o backend local (perfil `preview-local`). Em
// produção a API é https:// e o cleartext fica desligado — o padrão seguro do Android.
module.exports = () => {
  const apiUrl = process.env.EXPO_PUBLIC_API_URL ?? '';
  const usaHttpLocal = apiUrl.startsWith('http://');

  return {
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
        // O cleartext (HTTP) é habilitado pelo plugin expo-build-properties abaixo, que
        // funciona no build de release — a propriedade `usesCleartextTraffic` solta aqui
        // é ignorada no release pelo network security config padrão do Android.
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
            // 150 deixa a capivara visível sem esticar demais (o PNG tem 279px de largura).
            imageWidth: 150,
          },
        ],
        // Cleartext (HTTP) só quando a API é http:// (backend local). Em produção (https://)
        // fica false, mantendo o padrão seguro. Funciona no build de release, ao contrário
        // de android.usesCleartextTraffic solto.
        [
          'expo-build-properties',
          {
            android: { usesCleartextTraffic: usaHttpLocal },
          },
        ],
      ],
      experiments: {
        typedRoutes: true,
        reactCompiler: true,
      },
      owner: 'readrace',
      extra: {
        eas: {
          projectId: '3362764b-9ce4-48c1-9b4e-9c5cbf2d2ab8',
        },
      },
    },
  };
};
