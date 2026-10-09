import { useLocalSearchParams } from 'expo-router';

import { ChatClubeScreen } from '@/features/chat/ChatClubeScreen';

export default function ChatRoute() {
  const { clubeId } = useLocalSearchParams<{ clubeId: string }>();

  return <ChatClubeScreen key={clubeId} clubeId={clubeId} />;
}