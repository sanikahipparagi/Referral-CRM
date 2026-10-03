import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) { return <div className={cn("rounded-2xl border border-slate-200/80 bg-white shadow-[0_2px_10px_rgba(20,40,34,0.035)] dark:border-slate-800 dark:bg-slate-900", className)} {...props} />; }
export function CardHeader({ className, ...props }: HTMLAttributes<HTMLDivElement>) { return <div className={cn("flex flex-col gap-1.5 p-5", className)} {...props} />; }
export function CardTitle({ className, ...props }: HTMLAttributes<HTMLHeadingElement>) { return <h2 className={cn("font-semibold tracking-tight text-slate-900 dark:text-white", className)} {...props} />; }
export function CardDescription({ className, ...props }: HTMLAttributes<HTMLParagraphElement>) { return <p className={cn("text-sm text-slate-500 dark:text-slate-400", className)} {...props} />; }
export function CardContent({ className, ...props }: HTMLAttributes<HTMLDivElement>) { return <div className={cn("px-5 pb-5", className)} {...props} />; }
