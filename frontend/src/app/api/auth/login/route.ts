import { NextRequest, NextResponse } from "next/server";

const baseUrl = () => (process.env.CRM_API_URL ?? "http://localhost:8080/api/v1").replace(/\/$/, "");
const cookieName = "referral_session";

export async function POST(request: NextRequest) {
  const body = await request.json().catch(() => null);
  if (!body?.email || !body?.password) return NextResponse.json({ message: "Email and password are required." }, { status: 400 });
  try {
    const upstream = await fetch(`${baseUrl()}/auth/login`, {
      method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body), cache: "no-store",
    });
    const payload = await upstream.json().catch(() => ({}));
    if (!upstream.ok) return NextResponse.json(payload, { status: upstream.status });
    const response = NextResponse.json({ user: payload.user });
    response.cookies.set(cookieName, payload.accessToken, {
      httpOnly: true, secure: process.env.NODE_ENV === "production", sameSite: "lax", path: "/",
      maxAge: Number(payload.expiresInSeconds ?? 43200),
    });
    return response;
  } catch {
    return NextResponse.json({ message: "Can’t reach the CRM API. Make sure the backend is running." }, { status: 503 });
  }
}
