import { useLocalSearchParams } from 'expo-router';
import { PerfilScreen } from '@/features/perfil/PerfilScreen';

// O mesmo nome de rota que a #202 usa para abrir o perfil pelo ranking do clube.
export default function PerfilUsuarioRoute() {
  const { usuarioId } = useLocalSearchParams<{ usuarioId: string }>();
  return <PerfilScreen key={usuarioId} usuarioId={usuarioId} origem="feed" />;
}
