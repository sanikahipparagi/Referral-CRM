import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

const tones: Record<string, string> = {
  NOT_CONTACTED: "bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300",
  MESSAGE_READY: "bg-indigo-50 text-indigo-700 dark:bg-indigo-950 dark:text-indigo-300",
  CONTACTED: "bg-sky-50 text-sky-700 dark:bg-sky-950 dark:text-sky-300",
  REPLIED: "bg-violet-50 text-violet-700 dark:bg-violet-950 dark:text-violet-300",
  REFERRED: "bg-emerald-50 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300",
  INTERVIEW: "bg-amber-50 text-amber-800 dark:bg-amber-950 dark:text-amber-300",
  REJECTED: "bg-rose-50 text-rose-700 dark:bg-rose-950 dark:text-rose-300",
  NO_RESPONSE: "bg-orange-50 text-orange-800 dark:bg-orange-950 dark:text-orange-300",
};

export function Badge({ className, tone = "default", ...props }: HTMLAttributes<HTMLSpanElement> & { tone?: string }) {
  const style = tones[tone] ?? "bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300";
  return <span className={cn("inline-flex items-center rounded-full px-2.5 py-1 text-[11px] font-semibold leading-none", style, className)} {...props} />;
}
