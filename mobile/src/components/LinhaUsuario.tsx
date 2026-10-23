import { Pressable, Text, View } from 'react-native';
import { Avatar } from './avatar';
import { sizes } from '@/theme';

export function LinhaUsuario({
  nome,
  nomeUsuario,
  avatarUrl,
  titulo,
  onPress,
}: {
  nome: string;
  nomeUsuario: string;
  avatarUrl: string | null;
  titulo?: string;
  onPress: () => void;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${nome}, @${nomeUsuario}`}
      onPress={onPress}
      className="flex-row items-center gap-4 border-b border-border px-6 py-3 active:bg-surface-muted"
    >
      <Avatar name={nome} photoUrl={avatarUrl} size={sizes.avatarLarge} />
      <View className="min-w-0 flex-1 gap-1">
        <Text numberOfLines={1} className="font-inter-bold text-body text-text">
          {nome}
        </Text>
        <Text
          numberOfLines={2}
          className="font-inter text-bodySmall text-text-secondary"
        >
          @{nomeUsuario}
        </Text>
        {!!titulo && (
          <Text
            numberOfLines={1}
            className="font-inter text-caption text-text-muted"
          >
            {titulo}
          </Text>
        )}
      </View>
    </Pressable>
  );
}
