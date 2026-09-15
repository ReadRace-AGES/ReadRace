import type { BottomTabBarProps } from 'expo-router/js-tabs';
import type { ComponentType } from 'react';
import { Pressable, View } from 'react-native';

import { BookTabIcon, HomeTabIcon, PersonTabIcon, SearchTabIcon, StarTabIcon } from '@/components/TabIcons';
import { colors, shadows, sizes, spacing } from '@/theme';

// A pill tem paddingHorizontal: spacing[6] e columnGap: spacing[6] entre ícones.
// GAP_HIT_SLOP cobre exatamente o vão entre duas abas vizinhas.
// EDGE_HIT_SLOP estende a primeira/última aba até a borda externa da pill,
// senão sobra uma faixa morta nas pontas da barra.
const GAP_HIT_SLOP = spacing[6] / 2;
const EDGE_HIT_SLOP = spacing[6];
// A pill tem height fixo (sizes.navHeight) com borda de sizes.borderWidth em cima
// e embaixo; sem isso o Pressable (mesma altura da pill) deixa essa borda fora da
// área tocável, criando uma faixa morta de 2 * sizes.borderWidth no topo/base da aba.
const VERTICAL_HIT_SLOP = sizes.borderWidth;

type IconComponent = ComponentType<{ active: boolean; size?: number }>;

type TabDefinition = {
  routeName: string;
  Icon: IconComponent;
  accessibilityLabel: string;
};

const TAB_ORDER: readonly TabDefinition[] = [
  { routeName: 'feed', Icon: HomeTabIcon, accessibilityLabel: 'Feed' },
  { routeName: 'meus-livros', Icon: BookTabIcon, accessibilityLabel: 'Meus Livros' },
  { routeName: 'buscar', Icon: SearchTabIcon, accessibilityLabel: 'Buscar' },
  { routeName: 'desafios', Icon: StarTabIcon, accessibilityLabel: 'Desafios' },
  { routeName: 'perfil', Icon: PersonTabIcon, accessibilityLabel: 'Perfil' },
];

type RouteOf<Props> = Props extends { state: { routes: readonly (infer R)[] } } ? R : never;
type TabWithRoute = TabDefinition & { route: RouteOf<BottomTabBarProps> };

export function BottomTabBar({ state, navigation, insets }: BottomTabBarProps) {
  const focusedRouteName = state.routes[state.index]?.name;

  const visibleTabs: TabWithRoute[] = TAB_ORDER.map((tab) => ({
    ...tab,
    route: state.routes.find((r) => r.name === tab.routeName),
  })).filter((tab): tab is TabWithRoute => tab.route !== undefined);

  return (
    <View
      className="items-center px-5"
      style={{ paddingTop: spacing[3], paddingBottom: insets.bottom || spacing[6] }}
    >
      <View
        className="flex-row items-center justify-center rounded-pill border border-border bg-surface"
        style={{
          height: sizes.navHeight,
          columnGap: spacing[6],
          paddingHorizontal: spacing[6],
          ...shadows.floating,
        }}
      >
        {visibleTabs.map(({ route, Icon, accessibilityLabel, routeName }, index) => {
          const isFocused = focusedRouteName === routeName;
          const isFirst = index === 0;
          const isLast = index === visibleTabs.length - 1;

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
              hitSlop={{
                top: VERTICAL_HIT_SLOP,
                bottom: VERTICAL_HIT_SLOP,
                left: isFirst ? EDGE_HIT_SLOP : GAP_HIT_SLOP,
                right: isLast ? EDGE_HIT_SLOP : GAP_HIT_SLOP,
              }}
              accessibilityRole="button"
              accessibilityState={isFocused ? { selected: true } : {}}
              accessibilityLabel={accessibilityLabel}
              className="items-center justify-center"
              style={{ height: sizes.navHeight }}
            >
              <Icon active={isFocused} size={sizes.navIcon} />
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
