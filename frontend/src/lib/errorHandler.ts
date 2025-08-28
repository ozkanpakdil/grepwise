import { toast } from '@/components/ui/use-toast';

export function formatError(e: unknown, fallback = 'Something went wrong') {
  if (e instanceof Error) return e.message || fallback;
  if (typeof e === 'string') return e;
  try {
    return JSON.stringify(e);
  } catch {
    return fallback;
  }
}

export function notifyError(e: unknown, title = 'Error', fallback?: string) {
  const description = formatError(e, fallback);
  try {
    toast({ title, description, variant: 'destructive' as any });
  } catch (_) {
    // In tests or without toast context, avoid crashing
    console.error(`[toast-missing] ${title}: ${description}`);
  }
}
