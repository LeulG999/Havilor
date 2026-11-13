import { useRef, useState, type DragEvent } from 'react';

export const MAX_FILES = 5;
const MAX_FILE_SIZE = 10 * 1024 * 1024; // the backend's limit per file (10 MB)
const ACCEPT = '.eml,.txt,image/png,image/jpeg,image/gif,image/webp,application/pdf';

// One drop zone for several files: drag files onto it, or click it (or press Enter) to choose them. The chosen files
// are listed under it, each with a remove button. It only keeps the list; the page sends the files.
// Checked here already so she doesn't wait for the AI to hear it: at most 5 files, at most 10 MB each.
export default function DropZone({ files, onChange }: { files: File[]; onChange: (files: File[]) => void }) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);
  const [problem, setProblem] = useState<string | null>(null);

  function add(newFiles: File[]) {
    const tooBig = newFiles.filter((f) => f.size > MAX_FILE_SIZE);
    const fitting = newFiles.filter((f) => f.size <= MAX_FILE_SIZE);
    const all = [...files, ...fitting];
    setProblem(tooBig.length > 0 ? `Too big (max 10 MB each): ${tooBig.map((f) => f.name).join(', ')}`
      : all.length > MAX_FILES ? `At most ${MAX_FILES} files: the extra ones were left out.` : null);
    onChange(all.slice(0, MAX_FILES));
  }

  function onDrop(event: DragEvent) {
    event.preventDefault(); // otherwise the browser would open the file
    setDragging(false);
    add(Array.from(event.dataTransfer.files));
  }

  return (
    <div>
      <div className={dragging ? 'drop-zone dragging' : 'drop-zone'} role="button" tabIndex={0}
           onClick={() => inputRef.current?.click()}
           onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && inputRef.current?.click()}
           onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
           onDragLeave={() => setDragging(false)}
           onDrop={onDrop}>
        <b>Drop files here, or click to choose</b>
        <span className="muted">.eml, .txt with headers, screenshots (PNG, JPEG, GIF, WebP), PDF · up to {MAX_FILES} files</span>
        <input ref={inputRef} type="file" multiple accept={ACCEPT} hidden
               onChange={(e) => { add(Array.from(e.target.files ?? [])); e.target.value = ''; }} />
      </div>
      {problem && <p className="error">{problem}</p>}
      {files.length > 0 && (
        <ul className="file-list">
          {files.map((file, i) => (
            <li key={`${file.name}-${i}`}>
              <span className="mono">{file.name}</span>
              <span className="faint">{Math.max(1, Math.round(file.size / 1024))} KB</span>
              <button type="button" className="secondary small" aria-label={`Remove ${file.name}`}
                      onClick={() => onChange(files.filter((_, j) => j !== i))}>Remove</button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
