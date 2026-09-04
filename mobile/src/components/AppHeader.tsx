import { Pressable, Text, View } from 'react-native';
import { useRouter } from 'expo-router';
import Svg, { Path } from 'react-native-svg';

type AppHeaderProps = {
  title: string;
  subtitle?: string;
  showBack?: boolean;
  onBackPress?: () => void;
  streakDays?: number;
};

export function AppHeader({
  title,
  subtitle,
  showBack,
  onBackPress,
  streakDays,
}: AppHeaderProps) {
  const router = useRouter();

  function voltar() {
    if (onBackPress) {
      onBackPress();
      return;
    }
    router.back();
  }

  return (
    <View className="rounded-b-3xl bg-[#732634] px-5 pt-14 pb-6">
      <View className="flex-row items-center">
        {showBack && (
          <Pressable onPress={voltar} hitSlop={12} className="mr-3">
            <Text className="text-2xl text-white">←</Text>
          </Pressable>
        )}
        <Text numberOfLines={1} className="flex-1 text-2xl font-bold text-white">
          {title}
        </Text>
        {streakDays !== undefined && (
          <View className="ml-3 flex-row shrink-0 items-center rounded-full bg-white/10 px-3 py-1.5">
            <Svg width={14} height={16} viewBox="0 0 24 24" fill="#FFFFFF">
              <Path d="M13.5 0.67s.74 2.65.74 4.8c0 2.06-1.35 3.73-3.41 3.73-2.07 0-3.63-1.67-3.63-3.73l.03-.36C5.21 7.51 4 10.62 4 14c0 4.42 3.58 8 8 8s8-3.58 8-8C20 8.61 17.41 3.8 13.5.67zM11.71 19c-1.78 0-3.22-1.4-3.22-3.14 0-1.62 1.05-2.76 2.81-3.12 1.77-.36 3.6-1.21 4.62-2.58.39 1.29.59 2.65.59 4.04 0 2.65-2.15 4.8-4.8 4.8z" />
            </Svg>
            <Text className="ml-1.5 font-bold text-white">{streakDays} dias</Text>
          </View>
        )}
      </View>
      {subtitle && <Text className="mt-1 text-white/80">{subtitle}</Text>}
    </View>
  );
}