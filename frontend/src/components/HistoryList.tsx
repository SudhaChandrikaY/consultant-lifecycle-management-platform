import type { HistoryEntry } from '../api/types';
import { formatDateTime } from '../labels';
import EmptyState from './EmptyState';

/** Immutable history: who, when, old → new, reason/note, and a readable description. */
export default function HistoryList({ entries, emptyMessage = 'No history yet.' }: {
  entries: HistoryEntry[];
  emptyMessage?: string;
}) {
  if (entries.length === 0) return <EmptyState message={emptyMessage} />;
  return (
    <ul className="history">
      {entries.map((e) => (
        <li key={e.id}>
          <div>{e.description}</div>
          <div className="history__meta">
            {e.actor} · {formatDateTime(e.occurredAt)}
          </div>
          {e.reason && <div className="small">Reason: {e.reason}</div>}
          {e.note && <div className="small">Note: {e.note}</div>}
        </li>
      ))}
    </ul>
  );
}
