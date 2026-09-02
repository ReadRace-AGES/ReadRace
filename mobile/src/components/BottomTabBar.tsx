import { Ionicons } from "@expo/vector-icons";
import type { BottomTabBarProps } from "expo-router/js-tabs";
import { Pressable, View } from "react-native";

const MOCK_COLORS = {
  active: "#732634",
  inactive: "#9CA3AF",
  background: "#FFFFFF",
  border: "#ECECEC",
} as const;

type IconName = keyof typeof Ionicons.glyphMap;

type TabDefinition = {
  routeName: string;
  iconActive: IconName;
  iconInactive: IconName;
  accessibilityLabel: string;
};

const TAB_ORDER: readonly TabDefinition[] = [
  { routeName: "feed", iconActive: "home", iconInactive: "home-outline", accessibilityLabel: "Feed" },
  {
    routeName: "meus-livros",
    iconActive: "book",
    iconInactive: "book-outline",
    accessibilityLabel: "Meus Livros",
  },
  { routeName: "buscar", iconActive: "search", iconInactive: "search-outline", accessibilityLabel: "Buscar" },
  { routeName: "desafios", iconActive: "star", iconInactive: "star-outline", accessibilityLabel: "Desafios" },
  { routeName: "perfil", iconActive: "person", iconInactive: "person-outline", accessibilityLabel: "Perfil" },
];

export function BottomTabBar({ state, descriptors, navigation, insets }: BottomTabBarProps) {
  const focusedRouteName = state.routes[state.index]?.name;

  return (
    <View
      className="flex-row border-t"
      style={{
        backgroundColor: MOCK_COLORS.background,
        borderTopColor: MOCK_COLORS.border,
        paddingBottom: insets.bottom || 8,
      }}
    >
      {TAB_ORDER.map((tab) => {
        const route = state.routes.find((r) => r.name === tab.routeName);
        if (!route) return null;

        const isFocused = focusedRouteName === tab.routeName;

        const onPress = () => {
          const event = navigation.emit({
            type: "tabPress",
            target: route.key,
            canPreventDefault: true,
          });

          if (!isFocused && !event.defaultPrevented) {
            navigation.navigate(route.name, route.params);
          }
        };

        return (
          <Pressable
            key={route.key}
            onPress={onPress}
            accessibilityRole="button"
            accessibilityState={isFocused ? { selected: true } : {}}
            accessibilityLabel={tab.accessibilityLabel}
            className="flex-1 items-center justify-center py-2"
          >
            <Ionicons
              name={isFocused ? tab.iconActive : tab.iconInactive}
              size={24}
              color={isFocused ? MOCK_COLORS.active : MOCK_COLORS.inactive}
            />
            <View
              className="mt-1 h-[3px] w-6 rounded-full"
              style={{ backgroundColor: isFocused ? MOCK_COLORS.active : "transparent" }}
            />
          </Pressable>
        );
      })}
    </View>
  );
}

export default BottomTabBar;
