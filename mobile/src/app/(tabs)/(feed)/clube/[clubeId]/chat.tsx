import { useLocalSearchParams } from 'expo-router';

import { AcessoClube } from '@/features/clube/AcessoClube';
import { ChatClubeScreen } from '@/features/chat/ChatClubeScreen';

export default function ChatRoute() {
  const { clubeId, nomeClube } = useLocalSearchParams<{
    clubeId: string;
    nomeClube?: string;
  }>();

  return (
    <AcessoClube clubeId={clubeId}>
      <ChatClubeScreen key={clubeId} clubeId={clubeId} nomeClube={nomeClube} />
    </AcessoClube>
  );
}
