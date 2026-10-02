import { Redirect } from 'expo-router';
import * as WebBrowser from 'expo-web-browser';

// Destino do redirect do OAuth (readrace://auth no app, /auth no web). No web, fecha o popup do
// login e devolve o resultado para a tela de login.
WebBrowser.maybeCompleteAuthSession();

export default function AuthCallback() {
  return <Redirect href="/" />;
}
