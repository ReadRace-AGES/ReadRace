import { Text, View } from 'react-native';

export default function TesteScreen() {
  return (
    <View className="flex-1 items-center justify-center bg-[#FEFEFE] p-6">
      <Text className="text-[28px] font-bold text-[#732634]">Tela de teste</Text>
      <Text className="mt-2 text-center text-[16px] text-[#666666]">
        A pilha de navegação está funcionando.
      </Text>
    </View>
  );
}
