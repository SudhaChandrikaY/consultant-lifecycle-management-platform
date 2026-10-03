import { Link } from 'react-router';
import type { CountLink } from '../../api/types';

/** A dashboard figure linking to the list pre-filtered to exactly the counted records (FR-083). */
export default function CountTile({ label, value, link }: { label: string; value: number; link: CountLink }) {
  return (
    <Link className="tile" to={`/${link.list}?${link.query}`}>
      <div className="tile__value">{value}</div>
      <div className="tile__label">{label}</div>
    </Link>
  );
}
