import { View } from 'react-native';

import { useAuth } from '@/auth/AuthProvider';
import { authHabilitada } from '@/auth/config';
import { PrimaryButton } from '@/components/PrimaryButton';
import { PerfilScreen } from '@/features/perfil/PerfilScreen';

export default function MeuPerfilRoute() {
  const { sair } = useAuth();

  return (
    <PerfilScreen
      rodape={
        authHabilitada && (
          <View className="p-6">
            <PrimaryButton label="Sair" variant="outline" onPress={sair} />
          </View>
        )
      }
    />
  );
}
