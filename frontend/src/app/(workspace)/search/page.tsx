"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ArrowUpRight, Building2, FileText, Search, UsersRound } from "lucide-react";
import { api, errorMessage } from "@/lib/api";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";

type Result = { kind: "CONTACT" | "COMPANY" | "MESSAGE"; id: string; title: string; subtitle: string; snippet: string };
const meta = {
  CONTACT: { label: "Contact", icon: UsersRound, href: (id: string) => `/contacts?open=${encodeURIComponent(id)}` },
  COMPANY: { label: "Company", icon: Building2, href: () => "/companies" },
  MESSAGE: { label: "Message", icon: FileText, href: () => "/contacts" },
} as const;

export default function SearchPage() {
  const [term, setTerm] = useState("");
  const [results, setResults] = useState<Result[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => { setTerm(new URLSearchParams(window.location.search).get("q") ?? ""); }, []);
  useEffect(() => {
    const q = term.trim();
    if (q.length < 2) { setResults([]); setLoading(false); return; }
    let cancelled = false;
    setLoading(true); setError("");
    api<Result[]>(`search?q=${encodeURIComponent(q)}`).then((data) => { if (!cancelled) setResults(data); })
      .catch((cause) => { if (!cancelled) setError(errorMessage(cause)); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [term]);

  return <div className="mx-auto max-w-4xl space-y-7">
    <div><p className="text-xs font-semibold uppercase tracking-[0.16em] text-emerald-800 dark:text-emerald-300">Workspace search</p><h1 className="mt-2 text-3xl font-semibold tracking-tight">Search your CRM</h1><p className="mt-2 text-sm text-slate-500 dark:text-slate-400">Find people, companies, notes, and saved outreach messages.</p></div>
    <div className="relative"><Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" /><input autoFocus value={term} onChange={(event) => setTerm(event.target.value)} placeholder="Search names, notes, or message text…" aria-label="Search your CRM" className="h-12 w-full rounded-2xl border border-slate-200 bg-white pl-11 pr-4 text-sm shadow-sm outline-none focus:border-emerald-700 dark:border-slate-800 dark:bg-slate-900" /></div>
    {error && <p role="alert" className="text-sm text-rose-700">{error}</p>}
    <div className="flex items-center justify-between"><h2 className="text-sm font-semibold">{term.trim().length < 2 ? "Enter at least 2 characters" : loading ? "Searching…" : `${results.length} results`}</h2>{term.length >= 2 && <Badge>{results.length} found</Badge>}</div>
    <div className="space-y-3">{results.map((item) => { const itemMeta = meta[item.kind]; const Icon = itemMeta.icon; return <Link key={`${item.kind}-${item.id}`} href={itemMeta.href(item.id)} className="block"><Card className="transition-colors hover:border-emerald-300"><CardContent className="flex items-start gap-4 p-5"><div className="grid size-10 shrink-0 place-items-center rounded-xl bg-emerald-50 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300"><Icon size={18} /></div><div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-2"><h3 className="font-semibold">{item.title}</h3><Badge>{itemMeta.label}</Badge></div><p className="mt-1 text-xs text-slate-500">{item.subtitle}</p>{item.snippet && <p className="mt-3 line-clamp-2 whitespace-pre-wrap text-sm leading-6 text-slate-600 dark:text-slate-300">{item.snippet}</p>}</div><ArrowUpRight size={16} className="mt-1 shrink-0 text-slate-400" /></CardContent></Card></Link>; })}</div>
    {!loading && term.trim().length >= 2 && results.length === 0 && !error && <Card><CardContent className="py-14 text-center"><p className="font-medium">No matches for “{term.trim()}”</p><p className="mt-1 text-sm text-slate-500">Try a name, a company, or a phrase from an outreach message.</p></CardContent></Card>}
  </div>;
}
