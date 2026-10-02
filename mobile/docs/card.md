# Card (#17)

Container genérico sem conteúdo próprio, altura fixa ou rolagem interna. O tema define a superfície (`colors.surfaceMuted`), o raio (`radius.lg`), a sombra (`shadows.floating`) e o padding (`spacing[4]`). A superfície interna recorta conteúdo excedente; a camada externa preserva a sombra.

```tsx
import { Card } from '@/components/Card';

<Card>
  <Text>Conteúdo informado pela tela</Text>
</Card>

<Card onPress={abrirClube} accessibilityLabel="Abrir clube de leitura">
  <Text>Clube de leitura</Text>
</Card>
```

Sem `onPress`, não há botão nem feedback de toque no container; componentes interativos fornecidos como filhos mantêm seu próprio comportamento. Com `onPress`, o fundo muda para `colors.surfaceAlt` enquanto pressionado e a ação é chamada pelo evento `onPress`. Navegação e efeitos pertencem ao consumidor.

`style` configura o layout externo (por exemplo, margem e largura). Não há variante selecionada, cabeçalho, rodapé ou estado de carregamento. `testID` identifica a camada externa.

Sem filhos, com `null`, booleanos, arrays vazios, strings em branco ou fragments sem conteúdo, o componente retorna `null`. Um componente filho que decide internamente retornar `null` não pode ser detectado pelo pai: nesse caso o consumidor deve condicionar o próprio `Card` à existência de conteúdo. Textos e números devem estar dentro de `Text`, conforme o contrato do React Native.

## Validação manual

Abra `/teste-card` no Expo. Confira cartões curto e longo com a mesma moldura; toque no cartão interativo e confirme um incremento por toque e feedback enquanto pressionado; toque nos demais e confirme ausência de ação. Confira que o bloco largo é recortado e que os exemplos vazios não desenham moldura. Repita com largura estreita e fonte ampliada. Compare visualmente com as referências da issue #17 antes do aceite visual.

## Uso pelo PostCard (#22 / PR #75)

Quando as branches forem integradas, importe `Card` de `@/components/Card` e substitua `CardProvisorio` pelo componente, removendo o wrapper provisório. O conteúdo do post continua sendo responsabilidade do `PostCard`.

## Verificações executadas

- TypeScript: `tsc --noEmit` aprovado.
- Formatação dos arquivos novos com Prettier e `git diff --check` aprovados.
- Renderização estática com React DOM e React Native Web: nove formas de conteúdo vazio não produzem markup; o conteúdo do consumidor aparece; apenas o card com ação recebe `role="button"` e seu rótulo acessível.
- Validação de gestos, feedback pressionado e comparação visual com Figma permanecem manuais; a renderização estática não verifica layout ou interação no dispositivo.
