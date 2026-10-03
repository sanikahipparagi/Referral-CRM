"use client";

import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Building2, ChevronLeft, ChevronRight, ExternalLink, Heart, Plus, Search, Trash2 } from "lucide-react";
import { api, errorMessage } from "@/lib/api";
import type { Company, DashboardData, Page } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Modal } from "@/components/ui/modal";

type CompanyInput = { name: string; careerPage: string; priority: number; dreamCompany: boolean; applicationStatus: string; notes: string };
const applicationStatuses = ["NOT_APPLIED", "APPLIED", "SCREENING", "INTERVIEW", "OFFER", "REJECTED"];

function CompanyForm({ initial, busy, onCancel, onSave }: { initial?: Company; busy: boolean; onCancel: () => void; onSave: (value: CompanyInput) => void }) {
  const [form, setForm] = useState<CompanyInput>({ name: initial?.name ?? "", careerPage: initial?.careerPage ?? "", priority: initial?.priority ?? 3, dreamCompany: initial?.dreamCompany ?? false, applicationStatus: initial?.applicationStatus ?? "NOT_APPLIED", notes: initial?.notes ?? "" });
  function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); onSave({ ...form, careerPage: form.careerPage.trim(), notes: form.notes.trim() }); }
  return <form onSubmit={submit} className="space-y-4">
    <label className="form-label">Company name<Input required maxLength={200} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="e.g. Northstar Labs" /></label>
    <label className="form-label">Careers page <span className="font-normal text-slate-400">(optional)</span><Input type="url" maxLength={2048} value={form.careerPage} onChange={(e) => setForm({ ...form, careerPage: e.target.value })} placeholder="https://company.com/careers" /></label>
    <div className="grid gap-4 sm:grid-cols-2"><label className="form-label">Application status<select className="form-select" value={form.applicationStatus} onChange={(e) => setForm({ ...form, applicationStatus: e.target.value })}>{applicationStatuses.map((value) => <option key={value} value={value}>{value.toLowerCase().replaceAll("_"," ").replace(/\b\w/g,(s) => s.toUpperCase())}</option>)}</select></label><label className="form-label">Priority<select className="form-select" value={form.priority} onChange={(e) => setForm({ ...form, priority: Number(e.target.value) })}><option value={1}>1 · Low</option><option value={2}>2 · Relaxed</option><option value={3}>3 · Normal</option><option value={4}>4 · High</option><option value={5}>5 · Top priority</option></select></label></div>
    <label className="form-label">Notes<textarea rows={3} maxLength={5000} value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} placeholder="What draws you to this company?" className="form-textarea" /></label>
    <label className="flex cursor-pointer items-center gap-3 rounded-xl border border-slate-200 p-3.5 dark:border-slate-700"><input type="checkbox" checked={form.dreamCompany} onChange={(e) => setForm({ ...form, dreamCompany: e.target.checked })} className="size-4 accent-emerald-800" /><span><span className="block text-sm font-medium">Dream company</span><span className="mt-0.5 block text-xs text-slate-500">Keep this one near the top of your list.</span></span><Heart size={16} className={`ml-auto ${form.dreamCompany ? "fill-rose-400 text-rose-500" : "text-slate-300"}`} /></label>
    <div className="flex justify-end gap-2 border-t border-slate-100 pt-4 dark:border-slate-800"><Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button><Button type="submit" disabled={busy}>{busy ? "Saving…" : initial ? "Save changes" : "Add company"}</Button></div>
  </form>;
}

export default function CompaniesPage() {
  const [companies, setCompanies] = useState<Company[]>([]);
  const [stats, setStats] = useState<DashboardData["companies"]>([]);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [pages, setPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [open, setOpen] = useState(false);
  const [selected, setSelected] = useState<Company | undefined>();
  const [refresh, setRefresh] = useState(0);
  const companyStats = useMemo(() => new Map(stats.map((item) => [item.id, item])), [stats]);

  useEffect(() => { api<DashboardData>("dashboard").then((result) => setStats(result.companies)).catch(() => setStats([])); }, [refresh]);
  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setLoading(true); setError("");
      const query = new URLSearchParams({ page: String(page), size: "20", sort: "priority", direction: "desc" });
      if (search.trim()) query.set("search", search.trim());
      api<Page<Company>>(`companies?${query}`).then((result) => { setCompanies(result.content); setPages(result.totalPages); setTotal(result.totalElements); }).catch((cause) => setError(errorMessage(cause))).finally(() => setLoading(false));
    }, 140);
    return () => window.clearTimeout(timeout);
  }, [search, page, refresh]);

  async function save(value: CompanyInput) {
    setBusy(true); setError("");
    const payload = { ...value, careerPage: value.careerPage || null, notes: value.notes || null };
    try { await api(selected ? `companies/${selected.id}` : "companies", { method: selected ? "PUT" : "POST", body: JSON.stringify(payload) }); setOpen(false); setSelected(undefined); setRefresh((x) => x+1); }
    catch (cause) { setError(errorMessage(cause)); } finally { setBusy(false); }
  }
  async function remove(company: Company) {
    if (!window.confirm(`Remove ${company.name} from your company list?`)) return;
    try { await api(`companies/${company.id}`, { method: "DELETE" }); setRefresh((x) => x+1); }
    catch (cause) { setError(errorMessage(cause)); }
  }

  return <div className="space-y-6">
    <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end"><div><p className="text-xs font-semibold text-emerald-800 dark:text-emerald-400">Your shortlist</p><h1 className="mt-1 text-[29px] font-semibold tracking-[-0.04em]">Companies</h1><p className="mt-1.5 text-sm text-slate-500">Keep the places you’re excited about in view.</p></div><Button onClick={() => { setSelected(undefined); setOpen(true); }}><Plus size={16} /> Add company</Button></div>
    <Card><CardContent className="flex flex-col gap-4 p-4 sm:flex-row sm:items-center sm:p-5"><div className="relative min-w-0 flex-1"><Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" /><Input placeholder="Find a company…" aria-label="Search companies" className="pl-10" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} /></div><span className="text-xs text-slate-400">{total} {total === 1 ? "company" : "companies"}</span></CardContent></Card>
    {error && <p role="alert" className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:bg-rose-950/50 dark:text-rose-300">{error}</p>}
    <Card className="overflow-hidden"><div className="overflow-x-auto"><table className="w-full min-w-[720px] text-left text-sm"><thead className="bg-slate-50/80 text-[10px] font-bold uppercase tracking-[0.12em] text-slate-400 dark:bg-slate-950/60"><tr><th className="px-5 py-3.5">Company</th><th className="px-4 py-3.5">Status</th><th className="px-4 py-3.5">Priority</th><th className="px-4 py-3.5">Contacts</th><th className="px-4 py-3.5">Outreach</th><th className="px-4 py-3.5 text-right">Actions</th></tr></thead><tbody className="divide-y divide-slate-100 dark:divide-slate-800">
      {loading ? Array.from({length:5},(_,i)=><tr key={i}><td colSpan={6} className="px-5 py-5"><div className="h-5 animate-pulse rounded bg-slate-100 dark:bg-slate-800" /></td></tr>) : companies.map((company) => { const stat=companyStats.get(company.id); return <tr key={company.id} className="group transition hover:bg-[#f7faf8] dark:hover:bg-slate-800/40">
        <td className="px-5 py-4"><div className="flex items-center gap-3"><span className="grid size-9 shrink-0 place-items-center rounded-xl bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300"><Building2 size={17} /></span><div className="min-w-0"><button onClick={() => { setSelected(company); setOpen(true); }} className="flex max-w-[250px] items-center gap-2 truncate text-left text-[13px] font-semibold hover:text-emerald-800 dark:hover:text-emerald-300">{company.name}{company.dreamCompany && <Heart size={13} className="shrink-0 fill-rose-400 text-rose-500" />}</button>{company.careerPage && <a href={company.careerPage} target="_blank" rel="noreferrer" className="mt-1 inline-flex items-center gap-1 text-[10px] text-slate-400 hover:text-emerald-800">Careers page <ExternalLink size={10} /></a>}</div></div></td>
        <td className="px-4 py-4"><Badge tone={company.applicationStatus === "OFFER" ? "REFERRED" : company.applicationStatus === "REJECTED" ? "REJECTED" : company.applicationStatus === "INTERVIEW" ? "INTERVIEW" : "default"}>{company.applicationStatus.toLowerCase().replaceAll("_"," ").replace(/\b\w/g,(s)=>s.toUpperCase())}</Badge></td>
        <td className="px-4 py-4"><div aria-label={`Priority ${company.priority} out of 5`} className="flex gap-1">{[1,2,3,4,5].map((n)=><span key={n} className={`h-1.5 w-3 rounded-full ${n <= company.priority ? "bg-emerald-700 dark:bg-emerald-500" : "bg-slate-200 dark:bg-slate-700"}`} />)}</div></td>
        <td className="px-4 py-4 text-xs text-slate-600 dark:text-slate-300">{stat?.contactCount ?? 0}</td><td className="px-4 py-4 text-xs text-slate-600 dark:text-slate-300">{stat?.outreachCount ?? 0}</td>
        <td className="px-4 py-4"><div className="flex justify-end gap-1"><button onClick={() => { setSelected(company); setOpen(true); }} aria-label={`Edit ${company.name}`} className="rounded-lg px-2.5 py-1.5 text-xs font-medium text-slate-500 hover:bg-slate-100 hover:text-emerald-800 dark:hover:bg-slate-700">Edit</button><button onClick={() => void remove(company)} aria-label={`Remove ${company.name}`} className="grid size-8 place-items-center rounded-lg text-slate-400 hover:bg-rose-50 hover:text-rose-700 dark:hover:bg-rose-950/50"><Trash2 size={14} /></button></div></td>
      </tr>; })}</tbody></table></div>
      {!loading && !companies.length && <div className="px-5 py-16 text-center"><span className="mx-auto grid size-12 place-items-center rounded-2xl bg-emerald-50 text-emerald-900 dark:bg-emerald-950 dark:text-emerald-200"><Building2 size={21} /></span><h2 className="mt-4 text-sm font-semibold">{search ? "No companies found" : "Start your shortlist"}</h2><p className="mx-auto mt-1 max-w-sm text-xs leading-5 text-slate-500">{search ? "Try a different search." : "Save the organizations you want to learn about and connect with."}</p>{!search && <Button onClick={() => { setSelected(undefined); setOpen(true); }} size="sm" className="mt-4"><Plus size={14} /> Add first company</Button>}</div>}
      {!loading && total > 0 && <div className="flex items-center justify-between border-t border-slate-100 px-5 py-3.5 text-xs text-slate-500 dark:border-slate-800"><span>Page {page+1} of {Math.max(pages,1)}</span><div className="flex gap-1"><Button size="icon" variant="secondary" aria-label="Previous page" disabled={page<=0} onClick={()=>setPage((p)=>Math.max(0,p-1))}><ChevronLeft size={15}/></Button><Button size="icon" variant="secondary" aria-label="Next page" disabled={page+1>=pages} onClick={()=>setPage((p)=>p+1)}><ChevronRight size={15}/></Button></div></div>}
    </Card>
    <Modal open={open} onClose={()=>{setOpen(false);setSelected(undefined);}} title={selected ? "Edit company" : "Add a company"} description="Track your interest, application progress, and next steps."><CompanyForm key={selected?.id ?? "new-company"} initial={selected} busy={busy} onCancel={()=>setOpen(false)} onSave={(value)=>void save(value)} /></Modal>
  </div>;
}
