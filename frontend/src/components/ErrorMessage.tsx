// Shows an error from the backend (or the network) to the user. Errors are never hidden.
export default function ErrorMessage({ error }: { error: unknown }) {
  if (!error) {
    return null;
  }
  const message = error instanceof Error ? error.message : String(error);
  return <p className="error" role="alert">{message}</p>;
}
