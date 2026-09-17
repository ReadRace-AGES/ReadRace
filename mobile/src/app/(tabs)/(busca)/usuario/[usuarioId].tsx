import { useLocalSearchParams } from 'expo-router';
import { PerfilScreen } from '@/features/perfil/PerfilScreen';

export default function PerfilUsuarioRoute() {
  const { usuarioId } = useLocalSearchParams<{ usuarioId: string }>();
  return <PerfilScreen usuarioId={usuarioId} />;
}
