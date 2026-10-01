import Svg, { Circle, Ellipse } from 'react-native-svg';
import type { PrimaryButtonIconProps } from '@/components/PrimaryButton';

export function PawIcon({ size, color }: PrimaryButtonIconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24">
      <Ellipse cx={12} cy={16} rx={5} ry={4} fill={color} />
      <Circle cx={5.5} cy={10} r={2} fill={color} />
      <Circle cx={9.5} cy={5.5} r={2} fill={color} />
      <Circle cx={14.5} cy={5.5} r={2} fill={color} />
      <Circle cx={18.5} cy={10} r={2} fill={color} />
    </Svg>
  );
}