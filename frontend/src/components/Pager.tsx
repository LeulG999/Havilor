import type { Page } from '../api/types';

// "Previous · Page 2 of 5 (43 notes) · Next" under a paged list. It only reports which page was asked for;
// the page itself loads it.
export default function Pager<T>({ page, noun, onPage }: {
  page: Page<T>;
  noun: string;
  onPage: (pageNumber: number) => void;
}) {
  return (
    <div className="pager">
      <button type="button" className="secondary" disabled={page.page === 0}
              onClick={() => onPage(page.page - 1)}>Previous</button>
      <span className="muted">
        Page {page.totalPages === 0 ? 0 : page.page + 1} of {page.totalPages} ({page.totalElements} {noun})
      </span>
      <button type="button" className="secondary" disabled={page.page + 1 >= page.totalPages}
              onClick={() => onPage(page.page + 1)}>Next</button>
    </div>
  );
}
