import { useEffect, useState } from 'react';
import { Keyboard, Platform } from 'react-native';

/**
 * Altura do teclado aberto (0 com ele fechado), para a tela abrir espaço e rolar até o campo.
 *
 * No lugar do KeyboardAvoidingView: no Android 16 o app é sempre edge-to-edge, a janela não
 * encolhe com o teclado e o KeyboardAvoidingView não calcula a sobreposição (a tela ficava
 * parada, com o ENTRAR coberto). Os eventos do teclado chegam com a altura certa (testado na
 * #156), então a tela usa a altura deles.
 */
export function useAlturaTeclado(): number {
  const [altura, setAltura] = useState(0);

  useEffect(() => {
    // No iOS o "will" acompanha a animação do teclado; o Android só emite o "did".
    const mostrar = Platform.OS === 'ios' ? 'keyboardWillShow' : 'keyboardDidShow';
    const esconder = Platform.OS === 'ios' ? 'keyboardWillHide' : 'keyboardDidHide';
    const abriu = Keyboard.addListener(mostrar, (e) => setAltura(e.endCoordinates.height));
    const fechou = Keyboard.addListener(esconder, () => setAltura(0));
    return () => {
      abriu.remove();
      fechou.remove();
    };
  }, []);

  return altura;
}
