"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { ArrowUpRight, BarChart3, Building2, ChevronDown, Command, FileText, Inbox, LayoutDashboard, LogOut, Menu, Moon, Search, Sun, UsersRound, WandSparkles, X } from "lucide-react";
import { useAuth } from "@/components/auth-provider";
import { initials } from "@/lib/utils";
import { Button } from "@/components/ui/button";

const navigation = [
  { href: "/dashboard", label: "Overview", icon: LayoutDashboard },
  { href: "/contacts", label: "Contacts", icon: UsersRound },
  { href: "/companies", label: "Companies", icon: Building2 },
  { href: "/opportunities", label: "Opportunities", icon: WandSparkles },
  { href: "/review", label: "Review queue", icon: Inbox },
  { href: "/networking-analytics", label: "Networking analytics", icon: BarChart3 },
  { href: "/prompts", label: "Prompts & profile", icon: FileText },
];

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, loading, signOut } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [dark, setDark] = useState(false);
  const [globalSearch, setGlobalSearch] = useState("");
  const profileRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const saved = window.localStorage.getItem("referral-theme");
    const isDark = saved === "dark";
    setDark(isDark);
    document.documentElement.classList.toggle("dark", isDark);
  }, []);
  useEffect(() => { if (!loading && !user) router.replace("/login"); }, [loading, user, router]);
  useEffect(() => { setMenuOpen(false); setProfileOpen(false); }, [pathname]);
  useEffect(() => {
    function closeOnOutsideClick(event: PointerEvent) {
      if (event.target instanceof Node && !profileRef.current?.contains(event.target)) setProfileOpen(false);
    }
    function closeOnEscape(event: KeyboardEvent) { if (event.key === "Escape") setProfileOpen(false); }
    document.addEventListener("pointerdown", closeOnOutsideClick);
    document.addEventListener("keydown", closeOnEscape);
    return () => { document.removeEventListener("pointerdown", closeOnOutsideClick); document.removeEventListener("keydown", closeOnEscape); };
  }, []);

  function toggleTheme() {
    const next = !dark;
    setDark(next);
    document.documentElement.classList.toggle("dark", next);
    window.localStorage.setItem("referral-theme", next ? "dark" : "light");
  }
  async function logout() { await signOut(); router.replace("/login"); }

  if (loading || !user) return <div className="grid min-h-screen place-items-center bg-[#f4f7f5] dark:bg-slate-950"><div className="size-7 animate-spin rounded-full border-2 border-emerald-800/20 border-t-emerald-800" aria-label="Loading" /></div>;

  return (
    <div className="min-h-screen bg-[#f5f7f6] text-slate-900 dark:bg-[#091210] dark:text-slate-100">
      {menuOpen && <button aria-label="Close navigation" className="fixed inset-0 z-30 bg-slate-950/35 lg:hidden" onClick={() => setMenuOpen(false)} />}
      <aside className={`fixed inset-y-0 left-0 z-40 flex w-[252px] flex-col bg-[#123b32] text-white transition-transform lg:translate-x-0 ${menuOpen ? "translate-x-0" : "-translate-x-full"}`}>
        <div className="flex h-[76px] items-center gap-3 px-6">
          <div className="grid size-9 place-items-center rounded-xl bg-[#d4f3dd] text-[#123b32]"><Command size={20} strokeWidth={2.5} /></div>
          <div><div className="text-[15px] font-bold tracking-tight">Referral CRM</div><div className="text-[10px] font-medium uppercase tracking-[0.17em] text-emerald-100/60">Job search workspace</div></div>
          <button onClick={() => setMenuOpen(false)} className="ml-auto rounded-md p-1 text-emerald-100/70 lg:hidden" aria-label="Close menu"><X size={18} /></button>
        </div>
        <div className="px-4 pt-5">
          <p className="px-3 pb-2 text-[10px] font-bold uppercase tracking-[0.16em] text-emerald-100/45">Workspace</p>
          <nav className="space-y-1" aria-label="Main navigation">
            {navigation.map(({ href, label, icon: Icon }) => {
              const active = pathname === href || (href !== "/dashboard" && pathname.startsWith(`${href}/`));
              return <Link key={href} href={href} aria-current={active ? "page" : undefined} className={`group flex h-10 items-center gap-3 rounded-xl px-3 text-[13px] font-medium transition ${active ? "bg-white/12 text-white shadow-sm" : "text-emerald-50/70 hover:bg-white/7 hover:text-white"}`}><Icon size={17} strokeWidth={1.8} /><span>{label}</span>{label === "Contacts" && <span className="ml-auto text-[11px] text-emerald-100/40">CRM</span>}</Link>;
            })}
          </nav>
          <div className="mt-7 rounded-2xl border border-white/10 bg-white/5 p-4">
            <div className="flex items-center gap-2 text-emerald-100"><span className="size-2 rounded-full bg-[#95d7ad]" /><span className="text-[11px] font-semibold">Keep outreach human</span></div>
            <p className="mt-2 text-xs leading-5 text-emerald-50/60">Prepare thoughtful notes here, then send them yourself.</p>
            <div className="mt-3 flex items-center gap-1 text-[11px] font-medium text-[#bfe9cb]">Your pace, your voice <ArrowUpRight size={13} /></div>
          </div>
        </div>
        <div className="mt-auto border-t border-white/10 p-4">
          <div className="flex w-full items-center gap-3 rounded-xl p-2 text-left">
            <div className="grid size-9 shrink-0 place-items-center rounded-full bg-[#cbe9d5] text-xs font-bold text-[#153b32]">{initials(user.fullName || user.email)}</div>
            <span className="min-w-0 flex-1"><span className="block truncate text-xs font-semibold">{user.fullName || user.email}</span><span className="block truncate text-[10px] text-emerald-100/55">{user.email}</span></span>
            <button onClick={() => void logout()} aria-label="Sign out" title="Sign out" className="grid size-8 shrink-0 place-items-center rounded-lg text-emerald-100/60 hover:bg-white/10 hover:text-white"><LogOut size={15} /></button>
          </div>
        </div>
      </aside>
      <div className="min-h-screen lg:pl-[252px]">
        <header className="sticky top-0 z-20 flex h-[68px] items-center justify-between border-b border-slate-200/80 bg-white/90 px-4 backdrop-blur-md dark:border-slate-800 dark:bg-slate-950/85 sm:px-7 lg:px-9">
          <div className="flex min-w-0 items-center gap-3">
            <Button variant="ghost" size="icon" aria-label="Open navigation" onClick={() => setMenuOpen(true)} className="lg:hidden"><Menu size={19} /></Button>
            <div className="hidden items-center gap-2 text-xs text-slate-400 sm:flex"><span>Workspace</span><span>/</span><span className="font-medium text-slate-600 dark:text-slate-300">{navigation.find((n) => n.href === pathname || pathname.startsWith(`${n.href}/`))?.label ?? "Referral CRM"}</span></div>
            <div className="relative w-[min(44vw,300px)] sm:hidden"><Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={15} /><input aria-label="Search contacts and companies" placeholder="Search" value={globalSearch} onChange={(e) => setGlobalSearch(e.target.value)} onKeyDown={(e) => { if (e.key === "Enter" && globalSearch.trim()) router.push(`/search?q=${encodeURIComponent(globalSearch.trim())}`); }} className="h-9 w-full rounded-xl border border-slate-200 bg-slate-50 pl-9 pr-3 text-xs outline-none focus:border-emerald-700 dark:border-slate-800 dark:bg-slate-900" /></div>
          </div>
          <div className="flex items-center gap-2">
            <div className="relative hidden w-[250px] sm:block"><Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" /><input aria-label="Search contacts and companies" placeholder="Search contacts & companies" value={globalSearch} onChange={(e) => setGlobalSearch(e.target.value)} onKeyDown={(e) => { if (e.key === "Enter" && globalSearch.trim()) router.push(`/search?q=${encodeURIComponent(globalSearch.trim())}`); }} className="h-9 w-full rounded-xl border border-slate-200 bg-slate-50 pl-9 pr-3 text-xs outline-none placeholder:text-slate-400 focus:border-emerald-700 dark:border-slate-800 dark:bg-slate-900" /></div>
            <Button variant="ghost" size="icon" aria-label={dark ? "Switch to light theme" : "Switch to dark theme"} onClick={toggleTheme}>{dark ? <Sun size={17} /> : <Moon size={17} />}</Button>
            <div ref={profileRef} className="relative hidden sm:block">
              <button type="button" aria-label="Open profile menu" aria-haspopup="menu" aria-expanded={profileOpen} onClick={() => setProfileOpen((open) => !open)} className="flex items-center gap-1.5 rounded-lg px-2.5 py-2 text-xs font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"><span className="grid size-6 place-items-center rounded-full bg-emerald-100 text-[10px] font-bold text-emerald-900 dark:bg-emerald-900 dark:text-emerald-100">{initials(user.fullName || user.email)}</span><span className="max-w-[100px] truncate">{user.fullName.split(" ")[0] || user.email}</span><ChevronDown size={13} className={`transition-transform ${profileOpen ? "rotate-180" : ""}`} /></button>
              {profileOpen && <div role="menu" aria-label="Profile menu" className="absolute right-0 top-full z-30 mt-2 w-64 rounded-xl border border-slate-200 bg-white p-2 shadow-xl dark:border-slate-700 dark:bg-slate-900"><div className="border-b border-slate-100 px-3 py-2.5 dark:border-slate-800"><p className="truncate text-sm font-semibold text-slate-900 dark:text-slate-100">{user.fullName || "Your account"}</p><p className="mt-0.5 truncate text-xs text-slate-500">{user.email}</p></div><button type="button" role="menuitem" onClick={() => void logout()} className="mt-1 flex w-full items-center gap-2 rounded-lg px-3 py-2.5 text-left text-sm text-slate-600 hover:bg-slate-50 hover:text-rose-700 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-rose-300"><LogOut size={15} /> Sign out</button></div>}
            </div>
          </div>
        </header>
        <main className="mx-auto w-full max-w-[1440px] px-4 py-7 sm:px-7 lg:px-9 lg:py-9">{children}</main>
      </div>
    </div>
  );
}
