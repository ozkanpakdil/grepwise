import { useEffect, useState } from 'react';
import { getRedactionConfig, reloadRedaction, setRedactionGroupedConfig } from '@/api/redaction';
import { Button } from '@/components/ui/button';
import { useToast } from '@/components/ui/use-toast';
import Editor from '@monaco-editor/react';

export default function RedactionEditorPage() {
  const { toast } = useToast();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [serverGroups, setServerGroups] = useState<Record<string, { patterns: string[] }>>({});
  const [text, setText] = useState('');
  const [dirty, setDirty] = useState(false);

  const pretty = (obj: any) => JSON.stringify(obj, null, 2);

  const load = async () => {
    try {
      setLoading(true);
      const cfg = await getRedactionConfig();
      const groups = cfg.groups || {};
      setServerGroups(groups);
      setText(pretty(groups));
      setDirty(false);
    } catch (e: any) {
      toast({ title: 'Failed to load', description: e?.message || String(e), variant: 'destructive' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const validate = (): { ok: boolean; value?: Record<string, { patterns: string[] }>; error?: string } => {
    try {
      const obj = JSON.parse(text);
      if (obj === null || typeof obj !== 'object' || Array.isArray(obj)) {
        return { ok: false, error: 'Top-level must be an object (grouped map)' };
      }
      for (const [k, v] of Object.entries<any>(obj)) {
        if (v === null || typeof v !== 'object' || Array.isArray(v)) {
          return { ok: false, error: `Group "${k}" must be an object with a patterns array` };
        }
        if (!Array.isArray((v as any).patterns)) {
          return { ok: false, error: `Group "${k}" is missing patterns: []` };
        }
        const bad = (v as any).patterns.find((p: any) => typeof p !== 'string');
        if (bad !== undefined) {
          return { ok: false, error: `Group "${k}" has a non-string pattern` };
        }
      }
      return { ok: true, value: obj };
    } catch (e: any) {
      return { ok: false, error: `Invalid JSON: ${e?.message || String(e)}` };
    }
  };

  const onFormat = () => {
    const v = validate();
    if (!v.ok) {
      toast({ title: 'Invalid JSON', description: v.error, variant: 'destructive' });
      return;
    }
    setText(pretty(v.value));
  };

  const onRevert = () => {
    setText(pretty(serverGroups));
    setDirty(false);
  };

  const onSave = async () => {
    const v = validate();
    if (!v.ok) {
      toast({ title: 'Validation failed', description: v.error, variant: 'destructive' });
      return;
    }
    try {
      setSaving(true);
      await setRedactionGroupedConfig(v.value!);
      await load();
      toast({ title: 'Redaction config saved', description: 'Changes applied successfully' });
    } catch (e: any) {
      toast({ title: 'Save failed', description: e?.message || String(e), variant: 'destructive' });
    } finally {
      setSaving(false);
    }
  };

  const onReload = async () => {
    try {
      await reloadRedaction();
      await load();
      toast({ title: 'Reloaded', description: 'Config reloaded from disk' });
    } catch (e: any) {
      toast({ title: 'Reload failed', description: e?.message || String(e), variant: 'destructive' });
    }
  };

  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      if (dirty) {
        e.preventDefault();
        e.returnValue = '';
      }
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [dirty]);

  return (
    <div className="max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-xl font-semibold">Redaction Config Editor</h1>
        <div className="flex gap-2">
          <Button variant="outline" onClick={onReload} disabled={loading || saving}>
            Reload
          </Button>
          <Button variant="outline" onClick={onRevert} disabled={loading || saving || !dirty}>
            Revert
          </Button>
          <Button variant="outline" onClick={onFormat} disabled={loading || saving}>
            Format
          </Button>
          <Button onClick={onSave} disabled={loading || saving}>
            {saving ? 'Saving…' : 'Save'}
          </Button>
        </div>
      </div>

      <p className="text-sm text-muted-foreground mb-3">
        Edit the grouped redaction.json. Keys are object properties like "[\"password\",\"passwd\"]" or "cardnumber".
        Each value must include patterns: ["regex1", "regex2", ...]. Changes take effect immediately.
      </p>

      <div className="border rounded-md overflow-hidden">
        <Editor
          height="480px"
          language="json"
          value={text}
          onChange={(value) => {
            setText(value ?? '');
            setDirty(true);
          }}
          options={{
            minimap: { enabled: false },
            lineNumbers: 'on',
            wordWrap: 'on',
            tabSize: 2,
            insertSpaces: true,
            autoClosingBrackets: 'always',
            formatOnPaste: true,
            formatOnType: false,
            bracketPairColorization: { enabled: true },
          }}
          aria-label="Redaction JSON"
        />
      </div>

      {loading && <div className="mt-3 text-sm text-muted-foreground">Loading…</div>}
    </div>
  );
}
