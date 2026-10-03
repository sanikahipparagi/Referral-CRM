"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { ArrowRight, Check, Command, Eye, EyeOff, Handshake, ShieldCheck } from "lucide-react";
import { Suspense, useEffect, useState, type FormEvent } from "react";
import { useAuth } from "@/components/auth-provider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

function LoginContent() {
  const router = useRouter();
  const params = useSearchParams();
  const { signIn, signUp, user, loading } = useAuth();
  const [register, setRegister] = useState(params.get("mode") === "signup");
  const [showPassword, setShowPassword] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => { if (!loading && user) router.replace("/dashboard"); }, [loading, user, router]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setBusy(true);
    const data = new FormData(event.currentTarget);
    try {
      if (register) await signUp({ fullName: String(data.get("fullName")), email: String(data.get("email")), password: String(data.get("password")) });
      else await signIn({ email: String(data.get("email")), password: String(data.get("password")) });
      router.replace("/dashboard");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "We couldn’t sign you in. Try again.");
    } finally { setBusy(false); }
  }

  return (
    <main className="grid min-h-screen bg-white dark:bg-[#091210] lg:grid-cols-[1.03fr_0.97fr]">
      <section className="relative hidden overflow-hidden bg-[#123b32] px-12 py-10 text-white lg:flex lg:flex-col xl:px-[8vw]">
        <div className="pointer-events-none absolute -right-40 top-32 size-[480px] rounded-full border border-white/5" /><div className="pointer-events-none absolute -right-16 top-56 size-[330px] rounded-full border border-white/7" />
        <Link href="/" className="relative flex w-fit items-center gap-3"><span className="grid size-10 place-items-center rounded-xl bg-[#d4f3dd] text-[#123b32]"><Command size={20} strokeWidth={2.5} /></span><span className="text-[15px] font-bold tracking-tight">Referral CRM</span></Link>
        <div className="relative my-auto max-w-xl pb-8">
          <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-emerald-50/75"><span className="size-1.5 rounded-full bg-[#9addae]" />A more thoughtful job search</div>
          <h1 className="text-[clamp(38px,4.2vw,58px)] font-semibold leading-[1.08] tracking-[-0.045em]">Good opportunities<br />start with <span className="text-[#a8ddb9]">good people.</span></h1>
          <p className="mt-6 max-w-md text-[15px] leading-7 text-emerald-50/65">Keep every conversation, follow-up, and next step in one quiet place. You stay in control of every message you send.</p>
          <div className="mt-10 space-y-4 text-sm text-emerald-50/80">
            {[
              { Icon: Handshake, text: "Build genuine professional relationships" },
              { Icon: Check, text: "Keep follow-ups from slipping through" },
              { Icon: ShieldCheck, text: "Your outreach is always sent by you" },
            ].map(({ Icon, text }) => <div key={text} className="flex items-center gap-3"><span className="grid size-7 place-items-center rounded-full bg-white/8 text-[#bde7c8]"><Icon size={15} /></span>{text}</div>)}
          </div>
        </div>
        <p className="relative text-xs text-emerald-50/40">A personal workspace for your next chapter.</p>
      </section>
      <section className="flex min-h-screen items-center justify-center px-5 py-12 sm:px-10">
        <div className="w-full max-w-[410px]">
          <Link href="/" className="mb-12 flex items-center gap-2.5 lg:hidden"><span className="grid size-9 place-items-center rounded-xl bg-[#123b32] text-white"><Command size={18} /></span><span className="font-bold text-slate-900 dark:text-white">Referral CRM</span></Link>
          <div className="mb-8">
            <p className="mb-3 text-xs font-bold uppercase tracking-[0.15em] text-emerald-800 dark:text-emerald-400">Welcome {register ? "aboard" : "back"}</p>
            <h2 className="text-[32px] font-semibold tracking-[-0.04em] text-slate-900 dark:text-white">{register ? "Create your workspace" : "Sign in to your workspace"}</h2>
            <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">{register ? "Start organizing your referral outreach." : "Pick up right where you left off."}</p>
          </div>
          <form onSubmit={submit} className="space-y-4">
            {register && <label className="block space-y-1.5 text-sm font-medium text-slate-700 dark:text-slate-200">Full name<Input name="fullName" autoComplete="name" required maxLength={160} placeholder="Your name" /></label>}
            <label className="block space-y-1.5 text-sm font-medium text-slate-700 dark:text-slate-200">Email address<Input type="email" name="email" autoComplete="email" required maxLength={320} placeholder="you@example.com" /></label>
            <label className="block space-y-1.5 text-sm font-medium text-slate-700 dark:text-slate-200">Password<span className="relative block"><Input type={showPassword ? "text" : "password"} name="password" autoComplete={register ? "new-password" : "current-password"} minLength={register ? 12 : 1} maxLength={72} required placeholder={register ? "At least 12 characters" : "Enter your password"} className="pr-11" /><button type="button" className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400" onClick={() => setShowPassword(!showPassword)} aria-label={showPassword ? "Hide password" : "Show password"}>{showPassword ? <EyeOff size={17} /> : <Eye size={17} />}</button></span></label>
            {error && <p role="alert" className="rounded-xl bg-rose-50 px-3.5 py-3 text-sm text-rose-700 dark:bg-rose-950/60 dark:text-rose-300">{error}</p>}
            <Button type="submit" size="lg" className="mt-2 w-full" disabled={busy}>{busy ? "Please wait…" : register ? "Create account" : "Sign in"}<ArrowRight size={16} /></Button>
          </form>
          <p className="mt-7 text-center text-sm text-slate-500 dark:text-slate-400">{register ? "Already have an account?" : "New to Referral CRM?"} <button onClick={() => { setError(""); setRegister(!register); }} className="font-semibold text-emerald-800 hover:underline dark:text-emerald-400">{register ? "Sign in" : "Create an account"}</button></p>
          <p className="mt-12 text-center text-[11px] leading-5 text-slate-400">By continuing, you agree to use this workspace for your own job search. Your LinkedIn messages are never sent automatically.</p>
        </div>
      </section>
    </main>
  );
}

export default function LoginPage() { return <Suspense fallback={<div className="min-h-screen bg-white dark:bg-slate-950" />}><LoginContent /></Suspense>; }
