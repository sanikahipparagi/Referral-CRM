import { cva, type VariantProps } from "class-variance-authority";
import type { ButtonHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

const buttonStyles = cva("inline-flex items-center justify-center gap-2 rounded-xl text-sm font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 disabled:pointer-events-none disabled:opacity-50", {
  variants: {
    variant: {
      primary: "bg-emerald-800 text-white shadow-sm hover:bg-emerald-900 dark:bg-emerald-500 dark:text-emerald-950 dark:hover:bg-emerald-400",
      secondary: "border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800",
      ghost: "text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white",
      danger: "bg-rose-600 text-white hover:bg-rose-700",
    },
    size: { sm: "h-9 px-3", md: "h-10 px-4", lg: "h-11 px-5", icon: "size-10" },
  },
  defaultVariants: { variant: "primary", size: "md" },
});

type Props = ButtonHTMLAttributes<HTMLButtonElement> & VariantProps<typeof buttonStyles>;
export function Button({ className, variant, size, ...props }: Props) {
  return <button className={cn(buttonStyles({ variant, size }), className)} {...props} />;
}
