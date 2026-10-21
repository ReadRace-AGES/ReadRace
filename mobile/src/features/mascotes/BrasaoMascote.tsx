import { StyleSheet } from 'react-native';
import Svg, { Path } from 'react-native-svg';

// Brasão dourado que emoldura o mascote no topo do perfil. O interno é mais escuro que o dourado
// da borda para o mascote se destacar na frente dele.
const OURO = '#B8862F';
const FILETE = '#FFF8E7';
const INTERNO_CLARO = '#4F3A14';
const INTERNO_ESCURO = '#3A2A0D';

// Contorno no meio da borda: o traço de 6 cobre de 0 a 6 para dentro do viewBox.
const CONTORNO = 'M3 3H97V32C97 66 78 88 50 103C22 88 3 66 3 32Z';
const METADE_ESQUERDA = 'M3 3H50V103C22 88 3 66 3 32Z';

/** Largura / altura do brasão (viewBox 100 x 108). */
export const PROPORCAO_BRASAO = 100 / 108;

/** Preenche o pai; quem usa dá o tamanho com `PROPORCAO_BRASAO`. */
export function BrasaoMascote() {
  return (
    <Svg
      width="100%"
      height="100%"
      viewBox="0 0 100 108"
      style={StyleSheet.absoluteFill}
    >
      <Path d={CONTORNO} fill={INTERNO_ESCURO} />
      <Path d={METADE_ESQUERDA} fill={INTERNO_CLARO} />
      <Path d={CONTORNO} fill="none" stroke={OURO} strokeWidth={6} />
      <Path d={CONTORNO} fill="none" stroke={FILETE} strokeWidth={2} />
    </Svg>
  );
}
