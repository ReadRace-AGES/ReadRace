# ChamaSequencia

```tsx
import { ChamaSequencia } from '@/components/ChamaSequencia';

<ChamaSequencia acesa={true} />
```

Ícone estático da chama da sequência de leitura, usado no badge do `AppHeader`:

- **Acesa** (`acesa=true`, a leitura de hoje já foi registrada): chama preenchida com degradê vermelho, da ponta mais clara (`streakFireTip`) passando por `streakFire` até a base mais escura (`streakFireBase`).
- **Apagada** (`acesa=false`): só o contorno, em `textInverse`.
