import Svg, { Circle, Path } from 'react-native-svg';

type UsersIconProps = {
  size: number;
  color: string;
};

export function UsersIcon({ size, color }: UsersIconProps) {
  return (
    <Svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
    >
      <Circle
        cx="9"
        cy="7"
        r="4"
        stroke={color}
        strokeWidth="2"
      />

      <Path
        d="M2 21C2 17.134 5.134 14 9 14C12.866 14 16 17.134 16 21"
        stroke={color}
        strokeWidth="2"
        strokeLinecap="round"
      />

      <Path
        d="M16 3.5C18.2091 3.5 20 5.29086 20 7.5C20 9.70914 18.2091 11.5 16 11.5"
        stroke={color}
        strokeWidth="2"
        strokeLinecap="round"
      />

      <Path
        d="M17 14.5C19.7614 14.5 22 16.7386 22 19.5"
        stroke={color}
        strokeWidth="2"
        strokeLinecap="round"
      />
    </Svg>
  );
}

export default UsersIcon;