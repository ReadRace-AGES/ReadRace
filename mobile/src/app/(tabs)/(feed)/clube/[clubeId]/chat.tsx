
import { useLocalSearchParams } from 'expo-router';

import { ChatClubeScreen } from '@/features/chat/ChatClubeScreen';

export default function ChatRoute() {
  const { clubeId, nomeClube } = useLocalSearchParams<{
    clubeId: string;
    nomeClube?: string;
  }>();

  return (
    <ChatClubeScreen
      key={clubeId}
      clubeId={clubeId}
      nomeClube={nomeClube}
    />
  );
}
