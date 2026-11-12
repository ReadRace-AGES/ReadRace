import type { BottomTabBarProps } from 'expo-router/js-tabs';
import type { ComponentType } from 'react';
import { Pressable, Text, View } from 'react-native';

import {
  BookTabIcon,
  HomeTabIcon,
  PersonTabIcon,
  SearchTabIcon,
  StarTabIcon,
} from '@/components/TabIcons';
import { colors, shadows, sizes, spacing } from '@/theme';

type IconComponent = ComponentType<{ active: boolean; size?: number }>;

type TabDefinition = {
  routeName: string;
  Icon: IconComponent;
  accessibilityLabel: string;
};

const TAB_ORDER: readonly TabDefinition[] = [
  { routeName: '(feed)', Icon: HomeTabIcon, accessibilityLabel: 'Feed' },
  {
    routeName: '(biblioteca)',
    Icon: BookTabIcon,
    accessibilityLabel: 'Meus Livros',
  },
  { routeName: '(busca)', Icon: SearchTabIcon, accessibilityLabel: 'Buscar' },
  { routeName: 'desafios', Icon: StarTabIcon, accessibilityLabel: 'Desafios' },
  { routeName: '(perfil)', Icon: PersonTabIcon, accessibilityLabel: 'Perfil' },
];

type RouteOf<Props> = Props extends { state: { routes: readonly (infer R)[] } }
  ? R
  : never;
type TabWithRoute = TabDefinition & { route: RouteOf<BottomTabBarProps> };

export function BottomTabBar({
  state,
  navigation,
  insets,
  onHeightChange,
}: BottomTabBarProps & { onHeightChange: (height: number) => void }) {
  const focusedRouteName = state.routes[state.index]?.name;

  const visibleTabs: TabWithRoute[] = TAB_ORDER.map((tab) => ({
    ...tab,
    route: state.routes.find((r) => r.name === tab.routeName),
  })).filter((tab): tab is TabWithRoute => tab.route !== undefined);

  return (
    <View
      className="items-center px-5"
      onLayout={(event) => onHeightChange(event.nativeEvent.layout.height)}
      style={{
        paddingTop: spacing[3],
        paddingBottom: insets.bottom || spacing[6],
      }}
    >
      <View
        className="flex-row items-center justify-center rounded-pill border border-border bg-surface"
        style={{
          height: sizes.navHeight,
          width: '100%',
          maxWidth: 420,
          paddingHorizontal: spacing[2],
          ...shadows.floating,
        }}
      >
        {visibleTabs.map(({ route, Icon, accessibilityLabel, routeName }) => {
          const isFocused = focusedRouteName === routeName;

          const onPress = () => {
            const event = navigation.emit({
              type: 'tabPress',
              target: route.key,
              canPreventDefault: true,
            });

            if (!isFocused && !event.defaultPrevented) {
              navigation.navigate(routeName, route.params);
            }
          };

          return (
            <Pressable
              key={route.key}
              onPress={onPress}
              accessibilityRole="button"
              accessibilityState={isFocused ? { selected: true } : {}}
              accessibilityLabel={accessibilityLabel}
              className="items-center justify-center"
              style={{
                flex: 1,
                minHeight: sizes.navHeight - 2 * sizes.borderWidth,
              }}
            >
              <Icon active={isFocused} size={sizes.icon} />
              <Text
                className="mt-1 font-inter-semibold"
                style={{
                  fontSize: 10,
                  color: isFocused ? colors.primary : colors.textSecondary,
                }}
              >
                {routeName === '(biblioteca)' ? 'Livros' : accessibilityLabel}
              </Text>
              <View
                className="mt-1 rounded-pill"
                style={{
                  width: sizes.navIndicatorWidth,
                  height: sizes.navIndicatorHeight,
                  backgroundColor: isFocused ? colors.primary : 'transparent',
                }}
              />
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

export default BottomTabBar;
