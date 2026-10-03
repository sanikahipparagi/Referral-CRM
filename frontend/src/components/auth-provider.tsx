"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import type { User } from "@/lib/types";

type Credentials = { email: string; password: string };
type RegisterFields = Credentials & { fullName: string };
type AuthContextValue = { user: User | null; loading: boolean; signIn: (input: Credentials) => Promise<void>; signUp: (input: RegisterFields) => Promise<void>; signOut: () => Promise<void> };
const AuthContext = createContext<AuthContextValue | null>(null);

async function authRequest(path: string, body?: object) {
  const response = await fetch(`/api/auth/${path}`, {
    method: body ? "POST" : "GET", headers: body ? { "Content-Type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined, credentials: "same-origin", cache: "no-store",
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(payload.message ?? "Unable to complete sign-in.");
  return payload;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    authRequest("session").then((value) => setUser(value.user)).catch(() => setUser(null)).finally(() => setLoading(false));
  }, []);
  const signIn = useCallback(async (input: Credentials) => { const result = await authRequest("login", input); setUser(result.user); }, []);
  const signUp = useCallback(async (input: RegisterFields) => { const result = await authRequest("register", input); setUser(result.user); }, []);
  const signOut = useCallback(async () => { await fetch("/api/auth/logout", { method: "POST", credentials: "same-origin" }); setUser(null); }, []);
  const value = useMemo(() => ({ user, loading, signIn, signUp, signOut }), [user, loading, signIn, signUp, signOut]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuth must be used inside AuthProvider");
  return value;
}
