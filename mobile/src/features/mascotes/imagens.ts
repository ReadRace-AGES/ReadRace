import type { ImageSource } from 'expo-image';

// As artes vêm do frame Mascotes do Figma e vão empacotadas no app. A API manda só a chave
// (`imagem`), que é o nome do arquivo em assets/images/mascotes.
const ARTES: Record<string, ImageSource> = {
  'o-bruxo': require('../../../assets/images/mascotes/o-bruxo.png'),
  'o-cinzento': require('../../../assets/images/mascotes/o-cinzento.png'),
  'o-sonhador': require('../../../assets/images/mascotes/o-sonhador.png'),
  'a-bruxa-ma': require('../../../assets/images/mascotes/a-bruxa-ma.png'),
  'a-bruxa-boa': require('../../../assets/images/mascotes/a-bruxa-boa.png'),
};

/** Silhueta da carta bloqueada ("???"). */
export const SILHUETA: ImageSource = require('../../../assets/images/mascotes/bloqueado.png');

/**
 * Proporção da área onde a arte é desenhada: a da arte mais alta (a-bruxa-ma, 483 x 416), para
 * todas caberem apoiadas na base sem cortar o chapéu.
 */
export const PROPORCAO_ARTE = 483 / 416;

/** Uma chave que o app ainda não conhece mostra a silhueta em vez de quebrar. */
export function arteDoMascote(imagem: string): ImageSource {
  return ARTES[imagem] ?? SILHUETA;
}
