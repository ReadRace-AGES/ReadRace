// O NativeWind processa `global.css` no bundler, mas nao declara o modulo para o
// TypeScript — sem isto, `import '../../global.css'` em src/app/_layout.tsx quebra
// o `tsc --noEmit` que a mobile-ci.yml roda em todo PR.
// Fica fora de nativewind-env.d.ts de proposito: aquele arquivo e gerado e pode ser
// sobrescrito pelo NativeWind.
declare module '*.css';
