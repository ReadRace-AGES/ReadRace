# Slider (#28)

Componente controlado para selecionar inteiros. Limites, rótulos e valor são
informados pela tela. Não busca dados nem cria desafios.

```tsx
const [pages, setPages] = useState(150);

<Slider
  minimumValue={10}
  maximumValue={500}
  minimumLabel="10"
  maximumLabel="500+"
  value={pages}
  onValueChange={setPages}
  accessibilityLabel="Meta de páginas"
/>;
```

Importe `Slider` de `@/components/Slider`. Renderize o número selecionado e a
unidade fora do componente. O `+` é parte do rótulo: não aumenta o limite.

- O arrasto notifica valores inteiros, presos aos limites, sem repetir o mesmo
  valor a cada movimento do dedo na mesma posição.
- Um valor externo fora da faixa é corrigido visualmente e comunicado por
  `onValueChange`, inclusive no primeiro render. Frações são arredondadas.
- Os limites precisam ser inteiros seguros, com máximo maior que mínimo; o
  valor precisa ser finito.
- Leitores de tela recebem o intervalo e o valor atual. As ações de aumentar e
  diminuir alteram uma unidade, respeitando os limites.
- Cores, tipografia e dimensões usam o tema existente: trilha
  `progressTrackHeight`, marcador `icon` e área de toque `buttonHeight`. A largura
  acompanha o container, reservando espaço para o marcador nas duas pontas.

## Validação

Com Node 22 e as dependências instaladas:

```sh
npm run test:slider
npx tsc --noEmit
```

A rota existente `/teste` contém os exemplos de validação. Abra-a com o app
rodando e confira:

1. Valor 150 na faixa 10–500: preenchimento de aproximadamente 28,6%, com os
   rótulos `10` e `500+` nas pontas.
2. Arraste de ponta a ponta e além dos limites: só saem inteiros de 10 a 500.
3. Toque em `Receber valor 700`: marcador, valor comunicado e texto ficam em 500.
4. Toque em `Receber valor -20`: todos ficam em 10.
5. Use a segunda faixa, de −10 a 10, para conferir que os limites não são fixos.
6. Use as ações de ajuste do leitor de tela e repita com outra largura de tela.

O frame `desafiar amigo` serve como referência de layout. A trilha proporcional
e o marcador visível são a divergência explícita aprovada na issue #28.
