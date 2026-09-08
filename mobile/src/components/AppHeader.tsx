import { useState } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { Image } from 'expo-image';
import { useRouter } from 'expo-router';
import Svg, { Path } from 'react-native-svg';

const CHAMA_PATH =
  'M4.5 12.5C5.16304 12.5 5.79893 12.2366 6.26777 11.7678C6.73661 11.2989 7 10.6631 7 10C7 8.62002 6.5 8.00002 6 7.00002C4.928 4.85702 5.776 2.94602 8 1.00002C8.5 3.50002 10 5.90002 12 7.50002C14 9.10002 15 11 15 13C15 13.9193 14.8189 14.8295 14.4672 15.6788C14.1154 16.5281 13.5998 17.2998 12.9497 17.9498C12.2997 18.5998 11.5281 19.1154 10.6788 19.4672C9.82951 19.819 8.91925 20 8 20C7.08075 20 6.17049 19.819 5.32122 19.4672C4.47194 19.1154 3.70026 18.5998 3.05025 17.9498C2.40024 17.2998 1.88463 16.5281 1.53284 15.6788C1.18106 14.8295 1 13.9193 1 13C1 11.847 1.433 10.706 2 10C2 10.6631 2.26339 11.2989 2.73223 11.7678C3.20107 12.2366 3.83696 12.5 4.5 12.5Z';

const VOLTAR_PATH = 'M7.42497 1.0083L1.0083 7.42497L7.42497 13.8416';

type AppHeaderProps = {
  title: string;
  subtitle?: string;
  titleAlign?: 'center' | 'left';
  titleOverflow?: 'truncate' | 'wrap' | 'scroll' | 'expand';
  titleNumberOfLines?: number;
  showBack?: boolean;
  onBackPress?: () => void;
  streakDays?: number;
  streakActive?: boolean;
  coverUrl?: string;
};

export function AppHeader({
  title,
  subtitle,
  titleAlign = 'left',
  titleOverflow = 'truncate',
  titleNumberOfLines = 1,
  showBack,
  onBackPress,
  streakDays,
  streakActive = false,
  coverUrl,
}: AppHeaderProps) {
  const router = useRouter();
  const [larguraVoltar, setLarguraVoltar] = useState(0);
  const [larguraBadge, setLarguraBadge] = useState(0);
  const [expandido, setExpandido] = useState(false);

  function voltar() {
    if (onBackPress) {
      onBackPress();
      return;
    }
    router.back();
  }

  const centralizado = titleAlign === 'center';
  const temBadge = streakDays !== undefined;
  const larguraLateral = Math.max(larguraVoltar, larguraBadge);
  const classesTitulo = `text-2xl font-bold text-white ${centralizado ? 'text-center' : ''}`;

  function linhasDoTitulo() {
    if (titleOverflow === 'wrap') return titleNumberOfLines;
    if (titleOverflow === 'expand') return expandido ? undefined : 1;
    return 1;
  }

  return (
    <View className="rounded-b-3xl bg-[#732634] px-5 pt-14 pb-6">
      {coverUrl && (
        <View className="mb-4 items-center">
          <Image
            source={{ uri: coverUrl }}
            style={{ width: 150, height: 210, borderRadius: 12 }}
            contentFit="cover"
          />
        </View>
      )}

      <View className="flex-row items-center">
        <View
          className="items-start"
          style={centralizado ? { minWidth: larguraLateral } : undefined}
        >
          {showBack && (
            <Pressable
              onPress={voltar}
              hitSlop={12}
              className="mr-3"
              onLayout={(e) => setLarguraVoltar(e.nativeEvent.layout.width)}
            >
              <Svg width={9} height={15} viewBox="0 0 9 15" fill="none">
                <Path
                  d={VOLTAR_PATH}
                  stroke="#FFFFFF"
                  strokeWidth={2.01667}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </Svg>
            </Pressable>
          )}
        </View>

        {titleOverflow === 'scroll' ? (
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            className="flex-1"
            contentContainerStyle={
              centralizado ? { flexGrow: 1, justifyContent: 'center' } : undefined
            }
          >
            <Text numberOfLines={1} className={classesTitulo}>
              {title}
            </Text>
          </ScrollView>
        ) : titleOverflow === 'expand' ? (
          <Pressable className="flex-1" onPress={() => setExpandido(!expandido)}>
            <Text numberOfLines={linhasDoTitulo()} className={classesTitulo}>
              {title}
            </Text>
          </Pressable>
        ) : (
          <Text numberOfLines={linhasDoTitulo()} className={`flex-1 ${classesTitulo}`}>
            {title}
          </Text>
        )}

        <View
          className="items-end"
          style={centralizado ? { minWidth: larguraLateral } : undefined}
        >
          {temBadge && (
            <View
              className="ml-3 flex-row shrink-0 items-center rounded-full bg-white/10 px-3 py-1.5"
              onLayout={(e) => setLarguraBadge(e.nativeEvent.layout.width)}
            >
              <Svg width={14} height={18} viewBox="0 0 16 21" fill="none">
                <Path
                  d={CHAMA_PATH}
                  fill={streakActive ? '#FEFEFE' : 'none'}
                  stroke="#FEFEFE"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </Svg>
              <Text className="ml-1.5 font-bold text-white">{streakDays} dias</Text>
            </View>
          )}
        </View>
      </View>

      {subtitle && (
        <Text
          className={`mt-1 text-white/80 ${centralizado ? 'text-center' : ''}`}
          style={!centralizado && showBack ? { paddingLeft: larguraVoltar } : undefined}
        >
          {subtitle}
        </Text>
      )}
    </View>
  );
}