"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ArrowRight, ArrowUpRight, Building2, CalendarClock, CheckCircle2, MessageCircle, RefreshCw, Send, Sparkles, UsersRound } from "lucide-react";
import { api, errorMessage } from "@/lib/api";
import type { DashboardData, OpportunityQueue } from "@/lib/types";
import { displayDate } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/components/auth-provider";

function MetricCard({ label, value, caption, icon: Icon, tone }: { label: string; value: string | number; caption: string; icon: typeof Send; tone: string }) {
  return <Card className="group relative overflow-hidden"><CardContent className="flex items-start justify-between p-5 sm:p-5.5">
    <div><p className="text-[12px] font-medium text-slate-500 dark:text-slate-400">{label}</p><p className="mt-3 text-[30px] font-semibold leading-none tracking-[-0.04em] text-slate-900 dark:text-white">{value}</p><p className="mt-3 text-[11px] text-slate-400 dark:text-slate-500">{caption}</p></div>
    <span className={`grid size-10 place-items-center rounded-xl ${tone}`}><Icon size={18} strokeWidth={1.8} /></span>
  </CardContent></Card>;
}

function DashboardSkeleton() {
  return <div className="animate-pulse space-y-6"><div className="h-7 w-56 rounded bg-slate-200 dark:bg-slate-800" /><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{[1,2,3,4].map((x) => <div key={x} className="h-32 rounded-2xl bg-slate-200 dark:bg-slate-800" />)}</div><div className="grid gap-5 lg:grid-cols-[1.45fr_1fr]"><div className="h-80 rounded-2xl bg-slate-200 dark:bg-slate-800" /><div className="h-80 rounded-2xl bg-slate-200 dark:bg-slate-800" /></div></div>;
}

export default function DashboardPage() {
  const { user } = useAuth();
  const [data, setData] = useState<DashboardData | null>(null);
  const [opportunityQueue, setOpportunityQueue] = useState<OpportunityQueue | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  async function load() {
    setLoading(true); setError("");
    try { setData(await api<DashboardData>("dashboard")); } catch (cause) { setError(errorMessage(cause)); } finally { setLoading(false); }
  }
  useEffect(() => { void load(); }, []);
  useEffect(() => { api<OpportunityQueue>("assistant/opportunities").then(setOpportunityQueue).catch(() => setOpportunityQueue(null)); }, []);
  const hour = new Date().getHours();
  const greeting = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
  const firstName = user?.fullName?.trim().split(/\s+/)[0] ?? "";

  if (loading) return <DashboardSkeleton />;
  if (error || !data) return <div className="mx-auto max-w-2xl py-16 text-center"><div className="mx-auto grid size-12 place-items-center rounded-2xl bg-rose-50 text-rose-600 dark:bg-rose-950/40 dark:text-rose-300"><RefreshCw size={20} /></div><h1 className="mt-4 text-xl font-semibold">Your dashboard couldn’t load</h1><p className="mt-2 text-sm text-slate-500">{error || "Please try again."}</p><Button onClick={() => void load()} className="mt-5"><RefreshCw size={15} /> Try again</Button></div>;

  const followUps = data.followUps ?? [];
  const companies = data.companies ?? [];
  const maxOutreach = Math.max(1, ...companies.map((company) => company.outreachCount));
  return <div className="space-y-7">
    <section className="flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
      <div><p className="text-xs font-semibold text-emerald-800 dark:text-emerald-400">{greeting}{firstName ? `, ${firstName}` : ""}</p><h1 className="mt-1 text-[27px] font-semibold tracking-[-0.04em] text-slate-900 dark:text-white sm:text-[32px]">Your outreach, at a glance</h1><p className="mt-1.5 text-sm text-slate-500 dark:text-slate-400">A steady path forward starts with the next conversation.</p></div>
      <Link href="/contacts" className="inline-flex h-10 items-center justify-center gap-2 rounded-xl bg-emerald-800 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 dark:bg-emerald-500 dark:text-emerald-950"><UsersRound size={16} /> Add a contact</Link>
    </section>

    <section className="grid gap-3.5 sm:grid-cols-2 xl:grid-cols-4" aria-label="Outreach summary">
      <MetricCard label="Today’s outreach" value={data.todayOutreach} caption={data.todayOutreach === 1 ? "message recorded today" : "messages recorded today"} icon={Send} tone="bg-emerald-50 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300" />
      <MetricCard label="People contacted" value={data.peopleContacted} caption="unique people you’ve reached" icon={UsersRound} tone="bg-sky-50 text-sky-800 dark:bg-sky-950 dark:text-sky-300" />
      <MetricCard label="Pending follow-ups" value={data.pendingFollowUps} caption={`no reply after ${data.followUpDays} days`} icon={CalendarClock} tone="bg-amber-50 text-amber-800 dark:bg-amber-950 dark:text-amber-300" />
      <MetricCard label="Replies" value={data.replies} caption={`${data.responseRate}% response rate`} icon={MessageCircle} tone="bg-violet-50 text-violet-800 dark:bg-violet-950 dark:text-violet-300" />
    </section>

    <Card><CardContent className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center"><span className="grid size-11 shrink-0 place-items-center rounded-xl bg-emerald-50 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300"><Sparkles size={20}/></span><div className="min-w-0 flex-1"><p className="text-sm font-semibold">Today’s Opportunities</p><p className="mt-1 text-xs text-slate-500">{opportunityQueue ? `${opportunityQueue.suggestedContacts.length} suggested contacts · ${opportunityQueue.contacts.filter(c=>c.needsFollowUp).length} follow-ups to review · ${opportunityQueue.suggestedCompanies.length} companies to explore` : "Review your saved contacts, follow-ups, and company priorities."}</p></div><Link href="/opportunities" className="inline-flex h-9 shrink-0 items-center justify-center gap-2 rounded-xl border border-slate-200 px-3 text-xs font-semibold text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800">Open opportunities <ArrowRight size={14}/></Link></CardContent></Card>

    <section className="grid gap-5 xl:grid-cols-[1.25fr_0.75fr]">
      <Card>
        <CardHeader className="flex-row items-center justify-between"><div><CardTitle className="text-[15px]">Follow-ups to revisit</CardTitle><CardDescription className="mt-1">A gentle nudge, when the time feels right.</CardDescription></div><Link href="/contacts?status=CONTACTED" className="inline-flex items-center gap-1 text-xs font-semibold text-emerald-800 hover:text-emerald-950 dark:text-emerald-400">View contacts <ArrowRight size={14} /></Link></CardHeader>
        <CardContent>
          {followUps.length ? <div className="divide-y divide-slate-100 dark:divide-slate-800">{followUps.slice(0, 5).map((item) => <div key={item.id} className="flex items-center gap-3 py-3.5 first:pt-1 last:pb-0">
            <div className="grid size-9 shrink-0 place-items-center rounded-full bg-emerald-50 text-xs font-bold text-emerald-900 dark:bg-emerald-950 dark:text-emerald-200">{item.name.split(/\s+/).map((part) => part[0]).slice(0,2).join("").toUpperCase()}</div>
            <div className="min-w-0 flex-1"><p className="truncate text-sm font-semibold text-slate-800 dark:text-slate-100">{item.name}</p><p className="mt-0.5 truncate text-xs text-slate-500">{item.designation || "Contact"}{item.companyName ? ` · ${item.companyName}` : ""}</p></div>
            <div className="hidden text-right sm:block"><Badge tone="NO_RESPONSE">Needs follow-up</Badge><p className="mt-1.5 text-[10px] text-slate-400">Last sent {displayDate(item.lastSentAt)}</p></div>
            <Link href={`/contacts?open=${item.id}`} aria-label={`Open ${item.name}`} className="grid size-8 place-items-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-emerald-800 dark:hover:bg-slate-800"><ArrowUpRight size={16} /></Link>
          </div>)}</div> : <div className="rounded-xl bg-slate-50 px-5 py-8 text-center dark:bg-slate-950"><span className="mx-auto grid size-10 place-items-center rounded-full bg-emerald-50 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300"><CheckCircle2 size={19} /></span><p className="mt-3 text-sm font-semibold">You’re all caught up</p><p className="mt-1 text-xs text-slate-500">Any contact without a reply after seven days will show up here.</p></div>}
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex-row items-center justify-between"><div><CardTitle className="text-[15px]">Your progress</CardTitle><CardDescription className="mt-1">Small steps add up.</CardDescription></div><Sparkles size={17} className="text-emerald-700 dark:text-emerald-400" /></CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between rounded-xl bg-[#f4f8f5] p-4 dark:bg-slate-950"><span className="flex items-center gap-2.5 text-sm text-slate-600 dark:text-slate-300"><span className="grid size-8 place-items-center rounded-lg bg-white text-violet-700 shadow-sm dark:bg-slate-900"><MessageCircle size={16} /></span>Response rate</span><span className="text-lg font-semibold text-slate-900 dark:text-white">{data.responseRate}<span className="text-sm">%</span></span></div>
          <div className="flex items-center justify-between rounded-xl bg-[#f4f8f5] p-4 dark:bg-slate-950"><span className="flex items-center gap-2.5 text-sm text-slate-600 dark:text-slate-300"><span className="grid size-8 place-items-center rounded-lg bg-white text-emerald-800 shadow-sm dark:bg-slate-900"><Building2 size={16} /></span>Referrals received</span><span className="text-lg font-semibold text-slate-900 dark:text-white">{data.referralsReceived}</span></div>
          <div className="flex items-center justify-between rounded-xl bg-[#f4f8f5] p-4 dark:bg-slate-950"><span className="flex items-center gap-2.5 text-sm text-slate-600 dark:text-slate-300"><span className="grid size-8 place-items-center rounded-lg bg-white text-amber-800 shadow-sm dark:bg-slate-900"><CalendarClock size={16} /></span>Interviews tracked</span><span className="text-lg font-semibold text-slate-900 dark:text-white">{data.interviews}</span></div>
          <p className="pt-1 text-[11px] leading-5 text-slate-400">Your response rate is based on people who replied divided by people contacted.</p>
        </CardContent>
      </Card>
    </section>

    <section>
      <div className="mb-3.5 flex items-end justify-between"><div><h2 className="text-[15px] font-semibold text-slate-900 dark:text-white">Company activity</h2><p className="mt-1 text-xs text-slate-500">The conversations you’re building across your shortlist.</p></div><Link href="/companies" className="text-xs font-semibold text-emerald-800 hover:underline dark:text-emerald-400">All companies</Link></div>
      {companies.length ? <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">{companies.slice(0,4).map((company) => <Card key={company.id}><CardContent className="p-4.5"><div className="flex items-center gap-3"><span className="grid size-9 place-items-center rounded-xl bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300"><Building2 size={17} /></span><span className="min-w-0 flex-1 truncate text-sm font-semibold">{company.name}</span></div><div className="mt-4 flex items-end justify-between"><p className="text-[11px] text-slate-500">{company.contactCount} {company.contactCount === 1 ? "contact" : "contacts"}</p><p className="text-xs font-semibold text-slate-700 dark:text-slate-300">{company.outreachCount} outreach</p></div><div className="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800"><div className="h-full rounded-full bg-emerald-700 dark:bg-emerald-500" style={{ width: `${Math.max(8, (company.outreachCount / maxOutreach) * 100)}%` }} /></div><p className="mt-2 text-[10px] text-slate-400">{company.referralCount} {company.referralCount === 1 ? "referral" : "referrals"}</p></CardContent></Card>)}</div> : <Card><CardContent className="flex flex-col items-center p-8 text-center"><Building2 size={22} className="text-slate-400" /><p className="mt-3 text-sm font-semibold">Your company list starts here</p><p className="mt-1 text-xs text-slate-500">Add the companies you’re excited about, then link your contacts.</p><Link href="/companies" className="mt-4 inline-flex h-9 items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-3 text-xs font-semibold text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200"><Building2 size={14} /> Add a company <ArrowRight size={14} /></Link></CardContent></Card>}
    </section>
    <p className="text-center text-[10px] text-slate-400">{data.date ? `Today · ${displayDate(data.date)}` : ""} &nbsp;·&nbsp; Outreach is always sent manually by you.</p>
  </div>;
}
