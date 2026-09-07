import type { BottomTabBarProps } from "expo-router/js-tabs";
import type { ComponentType } from "react";
import { Pressable, View } from "react-native";

import { BookTabIcon, HomeTabIcon, PersonTabIcon, SearchTabIcon, StarTabIcon } from "@/components/TabIcons";

// Valores extraídos do componente "navbar" no Figma via Copy as SVG / Dev Mode
// (referência oficial "navbar.png" do design), não aproximados.
const TAB_COLORS = {
  active: "#732634",
  background: "#FEFEFE",
  border: "#F4EEEE",
} as const;

const PILL_SHADOW = [
  { offsetX: 0, offsetY: 4, blurRadius: 4, color: "rgba(0,0,0,0.07)" },
  { offsetX: 0, offsetY: -2, blurRadius: 4, color: "rgba(0,0,0,0.07)" },
];

// A pill tem paddingHorizontal: 25 e columnGap: 24 entre ícones.
// GAP_HIT_SLOP (12 = 24/2) cobre exatamente o vão entre duas abas vizinhas.
// EDGE_HIT_SLOP (25) estende a primeira/última aba até a borda externa da pill,
// senão sobra uma faixa morta de 13px (25 - 12) nas pontas da barra.
const GAP_HIT_SLOP = 12;
const EDGE_HIT_SLOP = 25;

type IconComponent = ComponentType<{ active: boolean; size?: number }>;

type TabDefinition = {
  routeName: string;
  Icon: IconComponent;
  accessibilityLabel: string;
};

const TAB_ORDER: readonly TabDefinition[] = [
  { routeName: "feed", Icon: HomeTabIcon, accessibilityLabel: "Feed" },
  { routeName: "meus-livros", Icon: BookTabIcon, accessibilityLabel: "Meus Livros" },
  { routeName: "buscar", Icon: SearchTabIcon, accessibilityLabel: "Buscar" },
  { routeName: "desafios", Icon: StarTabIcon, accessibilityLabel: "Desafios" },
  { routeName: "perfil", Icon: PersonTabIcon, accessibilityLabel: "Perfil" },
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
    <View className="items-center px-5" style={{ paddingTop: 12, paddingBottom: insets.bottom || 22 }}>
      <View
        className="flex-row items-center rounded-full border"
        style={{
          backgroundColor: TAB_COLORS.background,
          borderColor: TAB_COLORS.border,
          columnGap: 24,
          paddingVertical: 14,
          paddingHorizontal: 25,
          boxShadow: PILL_SHADOW,
        }}
      >
        {visibleTabs.map(({ route, Icon, accessibilityLabel, routeName }, index) => {
          const isFocused = focusedRouteName === routeName;
          const isFirst = index === 0;
          const isLast = index === visibleTabs.length - 1;

          const onPress = () => {
            const event = navigation.emit({
              type: "tabPress",
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
                top: GAP_HIT_SLOP,
                bottom: GAP_HIT_SLOP,
                left: isFirst ? EDGE_HIT_SLOP : GAP_HIT_SLOP,
                right: isLast ? EDGE_HIT_SLOP : GAP_HIT_SLOP,
              }}
              accessibilityRole="button"
              accessibilityState={isFocused ? { selected: true } : {}}
              accessibilityLabel={accessibilityLabel}
              className="items-center justify-center"
            >
              <Icon active={isFocused} size={22} />
              <View
                className="mt-1 rounded-full"
                style={{
                  width: 24,
                  height: 3.5,
                  backgroundColor: isFocused ? TAB_COLORS.active : "transparent",
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
