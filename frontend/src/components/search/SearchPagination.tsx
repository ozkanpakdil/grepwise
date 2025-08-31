import { Button } from '@/components/ui/button';

interface Props {
  totalCount: number | null;
  pageSize: number;
  currentPage: number;
  onPageChange: (page: number) => void;
}

export default function SearchPagination({ totalCount, pageSize, currentPage, onPageChange }: Props) {
  if (totalCount === null || totalCount <= pageSize) return null;
  const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
  return (
    <div className="flex items-center justify-between mt-3" data-testid="pagination">
      <div className="text-sm text-muted-foreground">
        Page {currentPage} of {totalPages}
      </div>
      <div className="flex gap-2">
        <Button
          variant="outline"
          size="sm"
          disabled={currentPage <= 1}
          onClick={() => onPageChange(Math.max(1, currentPage - 1))}
          data-testid="prev-page"
        >
          Previous
        </Button>
        <Button
          variant="outline"
          size="sm"
          disabled={currentPage >= totalPages}
          onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
          data-testid="next-page"
        >
          Next
        </Button>
      </div>
    </div>
  );
}
