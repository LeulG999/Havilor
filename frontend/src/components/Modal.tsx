import { useEffect, useRef, type ReactNode } from 'react';

// A pop-up window built on the browser's own <dialog> element (no library).
// showModal() makes it a real modal: it sits on top, dims the page, keeps keyboard focus inside, and Esc closes it.
// The parent decides when it is open (the `open` prop) and is told when it closes (`onClose`).
// `wide`: a bigger window, for a form with a preview next to it.
export default function Modal({ open, title, wide = false, onClose, children }: {
  open: boolean;
  title: string;
  wide?: boolean;
  onClose: () => void;
  children: ReactNode;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  // React state says open/closed; this keeps the real <dialog> in step with it.
  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) {
      return;
    }
    if (open && !dialog.open) {
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  return (
    // onClose fires for every way of closing: our button, or the Esc key.
    <dialog ref={dialogRef} className={wide ? 'modal wide' : 'modal'} onClose={onClose}>
      <div className="modal-header">
        <h2>{title}</h2>
        <button type="button" className="secondary" onClick={onClose} aria-label="Close">✕</button>
      </div>
      {open && children}
    </dialog>
  );
}
