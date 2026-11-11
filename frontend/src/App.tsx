import { Route, Routes } from 'react-router';

// Which page shows for which URL. The real pages arrive in Flows F2–F4; for now one placeholder.
export default function App() {
  return (
    <Routes>
      <Route path="*" element={<main className="page"><h1>Havilor</h1></main>} />
    </Routes>
  );
}
