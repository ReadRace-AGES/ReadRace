import Svg, { Circle, Path } from 'react-native-svg';

type SadFaceIconProps = {
  size: number;
  color: string;
};

export function SadFaceIcon({
  size,
  color,
}: SadFaceIconProps) {
  return (
    <Svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
    >
      <Circle
        cx="12"
        cy="12"
        r="9"
        stroke={color}
        strokeWidth={2}
      />

      <Path
        d="M9 9h.01M15 9h.01"
        stroke={color}
        strokeWidth={2}
        strokeLinecap="round"
      />

      <Path
        d="M8.5 16c1-1.5 2.2-2 3.5-2s2.5.5 3.5 2"
        stroke={color}
        strokeWidth={2}
        strokeLinecap="round"
      />
    </Svg>
  );
}

export default SadFaceIcon;