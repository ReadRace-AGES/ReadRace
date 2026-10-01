import Svg, { Circle, G, Rect } from 'react-native-svg';
import { colors, sizes } from '@/theme';

const DENTES = [0, 45, 90, 135];

export function GearIcon({
  size = sizes.icon,
  color = colors.primary,
}: {
  size?: number;
  color?: string;
}) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24">
      <G fill={color}>
        {DENTES.map((angulo) => (
          <Rect
            key={angulo}
            x={10}
            y={1.5}
            width={4}
            height={21}
            rx={1}
            transform={`rotate(${angulo} 12 12)`}
          />
        ))}
        <Circle cx={12} cy={12} r={7.5} />
      </G>
      <Circle cx={12} cy={12} r={3} fill={colors.surfacePink} />
    </Svg>
  );
}
