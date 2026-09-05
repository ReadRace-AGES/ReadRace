import { router } from 'expo-router';
import { Pressable, Text, View } from 'react-native';

type TabPlaceholderProps = {
  title: string;
};

export function TabPlaceholder({ title }: TabPlaceholderProps) {
  return (
    <View className="flex-1 items-center justify-center bg-[#FEFEFE] p-6">
      <Text className="text-[28px] font-bold text-[#732634]">{title}</Text>
      <Text className="mt-2 text-[16px] text-[#666666]">Tela raiz da aba</Text>
      <Pressable
        className="mt-6 rounded-lg bg-[#732634] px-[18px] py-3"
        onPress={() => router.push('../teste')}
      >
        <Text className="text-[15px] font-semibold text-white">Abrir tela de teste</Text>
      </Pressable>
    </View>
  );
}
