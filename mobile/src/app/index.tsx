import { Redirect } from 'expo-router';

//redireciona para outras telas (não é tela visual)

export default function Index() {
  return <Redirect href="../identidade-visual" />;
}