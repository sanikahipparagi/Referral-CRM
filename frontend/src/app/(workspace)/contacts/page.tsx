"use client";

import { useEffect, useMemo, useState, type FormEvent } from "react";
import { ArrowDownUp, ChevronLeft, ChevronRight, ExternalLink, Mail, MapPin, MoreHorizontal, Plus, Search, UserRound, UsersRound } from "lucide-react";
import { api, errorMessage } from "@/lib/api";
import type { Company, Contact, ContactStatus, Page } from "@/lib/types";
import { contactStatuses } from "@/lib/types";
import { initials, titleCase } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Modal } from "@/components/ui/modal";

type ContactInput = { name: string; companyId: string | null; linkedinUrl: string | null; designation: string | null; location: string | null; email: string | null; source: string | null; dateAdded: string; notes: string | null; status: ContactStatus };
const localDate = () => { const now = new Date(); return `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,"0")}-${String(now.getDate()).padStart(2,"0")}`; };
const emptyInput: ContactInput = { name: "", companyId: "", linkedinUrl: "", designation: "", location: "", email: "", source: "", dateAdded: localDate(), notes: "", status: "NOT_CONTACTED" };

function ContactForm({ initial, companies, busy, onCancel, onSave }: { initial?: Contact; companies: Company[]; busy: boolean; onCancel: () => void; onSave: (value: ContactInput) => void }) {
  const [form, setForm] = useState<ContactInput>(initial ? {
    name: initial.name, companyId: initial.companyId, linkedinUrl: initial.linkedinUrl, designation: initial.designation,
    location: initial.location, email: initial.email, source: initial.source, dateAdded: initial.dateAdded,
    notes: initial.notes, status: initial.status,
  } : emptyInput);
  const change = <K extends keyof ContactInput>(key: K, value: ContactInput[K]) => setForm((current) => ({ ...current, [key]: value }));
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSave({ ...form, companyId: form.companyId || null, email: form.email || null, linkedinUrl: form.linkedinUrl || null, designation: form.designation || null, location: form.location || null, source: form.source || null, notes: form.notes || null });
  }
  return <form onSubmit={submit} className="space-y-4">
    <div className="grid gap-4 sm:grid-cols-2">
      <label className="form-label">Name<Input required maxLength={160} value={form.name} onChange={(e) => change("name",e.target.value)} placeholder="e.g. Ananya Sharma" /></label>
      <label className="form-label">Company<select className="form-select" value={form.companyId ?? ""} onChange={(e) => change("companyId",e.target.value)}><option value="">Choose a company</option>{companies.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
      <label className="form-label">Designation<Input maxLength={200} value={form.designation ?? ""} onChange={(e) => change("designation",e.target.value)} placeholder="e.g. Engineering Manager" /></label>
      <label className="form-label">Location<Input maxLength={200} value={form.location ?? ""} onChange={(e) => change("location",e.target.value)} placeholder="e.g. Bengaluru" /></label>
      <label className="form-label">Email<Input type="email" maxLength={320} value={form.email ?? ""} onChange={(e) => change("email",e.target.value)} placeholder="name@company.com" /></label>
      <label className="form-label">LinkedIn profile<Input type="url" maxLength={2048} value={form.linkedinUrl ?? ""} onChange={(e) => change("linkedinUrl",e.target.value)} placeholder="https://linkedin.com/in/…" /></label>
      <label className="form-label">Source<Input maxLength={120} value={form.source ?? ""} onChange={(e) => change("source",e.target.value)} placeholder="e.g. Alumni network" /></label>
      <label className="form-label">Status<select className="form-select" value={form.status} onChange={(e) => change("status",e.target.value as ContactStatus)}>{contactStatuses.map((status) => <option key={status} value={status}>{titleCase(status)}</option>)}</select></label>
    </div>
    <label className="form-label">Notes<textarea rows={3} maxLength={5000} value={form.notes ?? ""} onChange={(e) => change("notes",e.target.value)} placeholder="A personal detail, shared context, or reminder…" className="form-textarea" /></label>
    <div className="flex justify-end gap-2 border-t border-slate-100 pt-4 dark:border-slate-800"><Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button><Button type="submit" disabled={busy}>{busy ? "Saving…" : initial ? "Save changes" : "Add contact"}</Button></div>
  </form>;
}

export default function ContactsPage() {
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [companies, setCompanies] = useState<Company[]>([]);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [pages, setPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [selected, setSelected] = useState<Contact | undefined>();
  const [refresh, setRefresh] = useState(0);
  const companyNames = useMemo(() => new Map(companies.map((company) => [company.id, company.name])), [companies]);

  useEffect(() => { const params = new URLSearchParams(window.location.search); setSearch(params.get("search") ?? ""); setStatus(params.get("status") ?? ""); }, []);
  useEffect(() => {
    const openId = new URLSearchParams(window.location.search).get("open");
    if (!openId) return;
    api<Contact>(`contacts/${encodeURIComponent(openId)}`).then((contact) => { setSelected(contact); setModalOpen(true); }).catch(() => {});
  }, []);
  useEffect(() => { api<Page<Company>>("companies?size=100&sort=name&direction=asc").then((result) => setCompanies(result.content)).catch(() => setCompanies([])); }, []);
  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setLoading(true); setError("");
      const query = new URLSearchParams({ page: String(page), size: "20", sort: "createdAt", direction: "desc" });
      if (search.trim()) query.set("search",search.trim());
      if (status) query.set("status",status);
      api<Page<Contact>>(`contacts?${query}`).then((result) => { setContacts(result.content); setPages(result.totalPages); setTotal(result.totalElements); }).catch((cause) => setError(errorMessage(cause))).finally(() => setLoading(false));
    }, 160);
    return () => window.clearTimeout(timeout);
  }, [page, search, status, refresh]);

  async function saveContact(input: ContactInput) {
    setBusy(true); setError("");
    try {
      await api(selected ? `contacts/${selected.id}` : "contacts", { method: selected ? "PUT" : "POST", body: JSON.stringify(input) });
      setModalOpen(false); setSelected(undefined); setRefresh((value) => value + 1);
    } catch (cause) { setError(errorMessage(cause)); } finally { setBusy(false); }
  }
  function edit(contact: Contact) { setSelected(contact); setModalOpen(true); }
  function add() { setSelected(undefined); setModalOpen(true); }
  async function remove(contact: Contact) {
    if (!window.confirm(`Remove ${contact.name} from your contacts?`)) return;
    try { await api(`contacts/${contact.id}`, { method: "DELETE" }); setRefresh((value) => value + 1); }
    catch (cause) { setError(errorMessage(cause)); }
  }

  return <div className="space-y-6">
    <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end"><div><p className="text-xs font-semibold text-emerald-800 dark:text-emerald-400">Your network</p><h1 className="mt-1 text-[29px] font-semibold tracking-[-0.04em]">Contacts</h1><p className="mt-1.5 text-sm text-slate-500">Keep track of the people behind your next opportunity.</p></div><Button onClick={add}><Plus size={16} /> Add contact</Button></div>
    <Card><CardContent className="p-4 sm:p-5">
      <div className="grid grid-cols-1 items-center gap-3 sm:grid-cols-[minmax(0,1fr)_205px_auto]"><div className="relative min-w-0"><Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" /><Input aria-label="Search contacts" placeholder="Search by name, notes, or role…" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} className="pl-10" /></div><select aria-label="Filter by status" className="form-select" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}><option value="">All statuses</option>{contactStatuses.map((value) => <option key={value} value={value}>{titleCase(value)}</option>)}</select><span className="text-xs text-slate-400 sm:whitespace-nowrap">{total} {total === 1 ? "contact" : "contacts"}</span></div>
    </CardContent></Card>

    {error && <p role="alert" className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:bg-rose-950/50 dark:text-rose-300">{error}</p>}
    <Card className="overflow-hidden">
      <div className="overflow-x-auto"><table className="w-full min-w-[790px] text-left text-sm"><thead className="bg-slate-50/80 text-[10px] font-bold uppercase tracking-[0.12em] text-slate-400 dark:bg-slate-950/60"><tr><th className="px-5 py-3.5">Contact</th><th className="px-4 py-3.5">Company</th><th className="px-4 py-3.5">Location</th><th className="px-4 py-3.5">Status</th><th className="px-4 py-3.5">Added</th><th className="w-20 px-4 py-3.5 text-right"><ArrowDownUp size={13} className="ml-auto" /></th></tr></thead>
      <tbody className="divide-y divide-slate-100 dark:divide-slate-800">{loading ? Array.from({ length: 5 }, (_,i) => <tr key={i}><td colSpan={6} className="px-5 py-5"><div className="h-5 animate-pulse rounded bg-slate-100 dark:bg-slate-800" /></td></tr>) : contacts.map((contact) => <tr key={contact.id} className="group transition hover:bg-[#f7faf8] dark:hover:bg-slate-800/40">
        <td className="px-5 py-4"><div className="flex items-center gap-3"><span className="grid size-9 shrink-0 place-items-center rounded-full bg-emerald-50 text-xs font-bold text-emerald-900 dark:bg-emerald-950 dark:text-emerald-200">{initials(contact.name)}</span><div className="min-w-0"><button onClick={() => edit(contact)} className="max-w-[220px] truncate text-left text-[13px] font-semibold text-slate-800 hover:text-emerald-800 hover:underline dark:text-slate-100 dark:hover:text-emerald-300">{contact.name}</button><p className="mt-0.5 flex items-center gap-1.5 truncate text-[11px] text-slate-500">{contact.designation || "Role not added"}{contact.email && <><span>·</span><Mail size={11} /><span className="max-w-[140px] truncate">{contact.email}</span></>}</p></div></div></td>
        <td className="px-4 py-4"><span className="text-xs font-medium text-slate-700 dark:text-slate-300">{companyNames.get(contact.companyId ?? "") ?? "—"}</span></td>
        <td className="px-4 py-4"><span className="flex items-center gap-1.5 text-xs text-slate-500">{contact.location ? <><MapPin size={12} />{contact.location}</> : "—"}</span></td>
        <td className="px-4 py-4"><Badge tone={contact.status}>{titleCase(contact.status)}</Badge></td>
        <td className="px-4 py-4 text-xs text-slate-500">{contact.dateAdded ? new Intl.DateTimeFormat("en-IN",{day:"numeric",month:"short",year:"numeric"}).format(new Date(`${contact.dateAdded}T00:00:00`)) : "—"}</td>
        <td className="px-4 py-4"><div className="flex justify-end gap-1 opacity-70 transition group-hover:opacity-100"><button aria-label={`Edit ${contact.name}`} title="Edit contact" onClick={() => edit(contact)} className="grid size-8 place-items-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-emerald-800 dark:hover:bg-slate-700"><MoreHorizontal size={16} /></button>{contact.linkedinUrl && <a aria-label={`${contact.name} LinkedIn profile`} title="Open profile" href={contact.linkedinUrl} target="_blank" rel="noreferrer" className="grid size-8 place-items-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-emerald-800 dark:hover:bg-slate-700"><ExternalLink size={15} /></a>}<button aria-label={`Remove ${contact.name}`} title="Remove contact" onClick={() => void remove(contact)} className="grid size-8 place-items-center rounded-lg text-slate-400 hover:bg-rose-50 hover:text-rose-700 dark:hover:bg-rose-950/50"><span className="text-lg leading-none">×</span></button></div></td>
      </tr>)}</tbody></table></div>
      {!loading && !contacts.length && <div className="px-5 py-16 text-center"><span className="mx-auto grid size-12 place-items-center rounded-2xl bg-emerald-50 text-emerald-900 dark:bg-emerald-950 dark:text-emerald-200"><UserRound size={21} /></span><h2 className="mt-4 text-sm font-semibold">{search || status ? "No matching contacts" : "Your network is ready to grow"}</h2><p className="mx-auto mt-1 max-w-sm text-xs leading-5 text-slate-500">{search || status ? "Try another name, note, or status filter." : "Add someone you’ve spoken with or would like to reach out to."}</p>{!search && !status && <Button onClick={add} size="sm" className="mt-4"><Plus size={14} /> Add first contact</Button>}</div>}
      {!loading && total > 0 && <div className="flex items-center justify-between border-t border-slate-100 px-5 py-3.5 text-xs text-slate-500 dark:border-slate-800"><span>Page {page + 1} of {Math.max(pages,1)}</span><div className="flex gap-1"><Button size="icon" variant="secondary" aria-label="Previous page" disabled={page <= 0} onClick={() => setPage((p) => Math.max(0,p-1))}><ChevronLeft size={15} /></Button><Button size="icon" variant="secondary" aria-label="Next page" disabled={page + 1 >= pages} onClick={() => setPage((p) => p+1)}><ChevronRight size={15} /></Button></div></div>}
    </Card>
    <Modal open={modalOpen} onClose={() => { setModalOpen(false); setSelected(undefined); }} title={selected ? "Edit contact" : "Add a contact"} description={selected ? "Update the details you want to keep close." : "Start with the basics. You can add more context later."}>
      <ContactForm key={selected?.id ?? "new-contact"} initial={selected} companies={companies} busy={busy} onCancel={() => setModalOpen(false)} onSave={(value) => void saveContact(value)} />
    </Modal>
  </div>;
}
