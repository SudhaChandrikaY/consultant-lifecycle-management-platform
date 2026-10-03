import type { ReactNode } from 'react';
import NavBar from './NavBar';

interface Props {
  title: ReactNode;
  actions?: ReactNode;
  children?: ReactNode;
}

export default function PageLayout({ title, actions, children }: Props) {
  return (
    <div className="app-shell">
      <NavBar />
      <main className="page">
        <div className="page__header">
          <h1>{title}</h1>
          {actions && <div className="page__actions">{actions}</div>}
        </div>
        {children}
      </main>
    </div>
  );
}
